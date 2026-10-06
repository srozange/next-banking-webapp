package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur réel de Powens (mode {@code reel}) : délais de connexion et de lecture portés par le client HTTP,
 * coupe-circuit Resilience4j sur chaque appel. Basic pour {@code /auth/init}, Bearer (jeton permanent) ensuite.
 * Toute panne (statut non 2xx, délai, corps illisible, coupe-circuit ouvert) lève {@link PowensIndisponible}, jamais
 * une liste vide (FR-028-12). Ni le secret, ni les jetons, ni les adresses qui les portent ne sont journalisés.
 */
public class PowensReel implements Powens {

    static final int TRANSACTIONS_MAX = 20;

    private static final Logger JOURNAL = LoggerFactory.getLogger(PowensReel.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient client;
    private final String base;
    private final String domaine;
    private final String clientId;
    private final String secret;
    private final String retour;
    private final String banques;
    private final CircuitBreaker coupeCircuit;

    public PowensReel(RestClient client, String base, String domaine, String clientId, String secret, String retour,
            String banques, ProprietesPowens proprietes) {
        this.client = client;
        this.base = base;
        this.domaine = domaine;
        this.clientId = clientId;
        this.secret = secret;
        this.retour = retour;
        this.banques = banques;
        this.coupeCircuit = CircuitBreaker.of("powens", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(proprietes.seuilEchecs())
                .minimumNumberOfCalls(proprietes.seuilEchecs())
                .failureRateThreshold(100)
                .waitDurationInOpenState(proprietes.dureeOuverture())
                .build());
    }

    @Override
    public UtilisateurPowens creerUtilisateur() {
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        String corps = JSON.writeValueAsString(Map.of("client_id", clientId, "client_secret", secret));
        JsonNode reponse = appeler(() -> lire(client.post().uri(URI.create(base + "/auth/init"))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .contentType(MediaType.APPLICATION_JSON).body(corps)));
        String jeton = texte(reponse, "auth_token");
        return new UtilisateurPowens(reponse.path("id_user").asLong(0), jeton);
    }

    @Override
    public String codeTemporaire(String jeton) {
        return texte(appeler(() -> lire(client.get().uri(URI.create(base + "/auth/token/code"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton))), "code");
    }

    @Override
    public String adresseConnexion(String code, String state) {
        return ConstructeurAdresse.adresse(domaine, clientId, retour, code, banques, state);
    }

    @Override
    public List<CompteExterne> comptes(String jeton) {
        JsonNode reponse = appeler(() -> lire(client.get().uri(URI.create(base + "/users/me/accounts"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton)));
        return convertir(() -> {
            List<CompteExterne> comptes = new ArrayList<>();
            for (JsonNode compte : reponse.path("accounts")) {
                JsonNode solde = compte.get("balance");
                comptes.add(new CompteExterne(compte.get("id").asLong(), compte.path("name").asString(""),
                        compte.path("type").asString(""),
                        solde == null || solde.isNull() ? null
                                : new BigDecimal(solde.asString()).setScale(2, RoundingMode.HALF_UP),
                        compte.path("iban").asString("")));
            }
            return List.copyOf(comptes);
        });
    }

    @Override
    public List<TransactionExterne> transactions(String jeton, long compteId) {
        JsonNode reponse = appeler(() -> lire(client.get()
                .uri(URI.create(base + "/users/me/accounts/" + compteId + "/transactions?limit=" + TRANSACTIONS_MAX))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton)));
        return convertir(() -> {
            List<TransactionExterne> transactions = new ArrayList<>();
            for (JsonNode transaction : reponse.path("transactions")) {
                if (transactions.size() == TRANSACTIONS_MAX) {
                    break;
                }
                String simplifie = transaction.path("simplified_wording").asString("");
                String libelle = simplifie.isBlank() ? transaction.path("original_wording").asString("") : simplifie;
                transactions.add(new TransactionExterne(transaction.get("id").asLong(),
                        LocalDate.parse(transaction.get("date").asString()), libelle,
                        new BigDecimal(transaction.get("value").asString()).setScale(2, RoundingMode.HALF_UP)));
            }
            return List.copyOf(transactions);
        });
    }

    private JsonNode appeler(Callable<JsonNode> appel) {
        try {
            return coupeCircuit.executeCallable(appel);
        } catch (CallNotPermittedException ouvert) {
            JOURNAL.warn("Powens indisponible : coupe-circuit ouvert");
            throw new PowensIndisponible("coupe-circuit ouvert");
        } catch (EchecFournisseur echec) {
            JOURNAL.warn("Powens indisponible : {}", echec.getMessage());
            throw new PowensIndisponible(echec.getMessage());
        } catch (Exception erreur) {
            // Le message d'une erreur d'appel peut porter l'adresse : seule sa nature est journalisée.
            String motif = motif(erreur);
            JOURNAL.error("Powens indisponible : {}", motif);
            throw new PowensIndisponible(motif);
        }
    }

    private static JsonNode lire(RestClient.RequestHeadersSpec<?> requete) {
        Reponse reponse = requete.accept(MediaType.APPLICATION_JSON).exchange((envoi, retour) -> new Reponse(
                retour.getStatusCode().value(), new String(retour.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        if (reponse.statut() < 200 || reponse.statut() > 299) {
            throw new EchecFournisseur("statut " + reponse.statut());
        }
        try {
            JsonNode racine = JSON.readTree(reponse.corps());
            if (racine == null || !racine.isObject()) {
                throw new EchecFournisseur("corps illisible");
            }
            return racine;
        } catch (JacksonException e) {
            throw new EchecFournisseur("corps illisible");
        }
    }

    // Lecture du corps déjà reçu : un corps de forme inattendue est une indisponibilité, pas une liste vide.
    private static <T> T convertir(Callable<T> lecture) {
        try {
            return lecture.call();
        } catch (Exception e) {
            JOURNAL.warn("Powens indisponible : corps illisible");
            throw new PowensIndisponible("corps illisible");
        }
    }

    private static String texte(JsonNode reponse, String champ) {
        JsonNode valeur = reponse.get(champ);
        return valeur == null || valeur.isNull() ? null : valeur.asString();
    }

    private static String motif(Throwable erreur) {
        for (Throwable cause = erreur; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException) {
                return "délai dépassé";
            }
        }
        return "erreur d'appel (" + erreur.getClass().getSimpleName() + ")";
    }

    private record Reponse(int statut, String corps) {
    }

    /** Échec signalé par Powens ou réponse inexploitable ; compté par le coupe-circuit. */
    static final class EchecFournisseur extends RuntimeException {

        EchecFournisseur(String motif) {
            super(motif, null, false, false);
        }
    }
}
