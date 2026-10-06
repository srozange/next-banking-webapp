package fr.nextbanking.bourse.adapter.out.fournisseur;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.domain.PointCotation;
import fr.nextbanking.bourse.domain.TypeSerie;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur réel du fournisseur AlphaVantage (mode {@code reel}) : {@code TIME_SERIES_DAILY} compacte ou
 * {@code TIME_SERIES_WEEKLY}, derrière un délai d'attente, un coupe-circuit (Resilience4j) et un cache (Caffeine).
 * Toute indisponibilité rend une liste vide et est journalisée, jamais propagée (FR-029-03) ; sans clé, aucune requête
 * n'est émise (FR-029-05). Ni la clé ni l'URL qui la porte ne sont journalisées.
 *
 * <p>Le coupe-circuit s'ouvre après {@code seuil-echecs} appels consécutifs en échec (exception, statut non 2xx, corps
 * illisible, délai dépassé, champ {@code Information} ou {@code Note}) ; un champ {@code Error Message} ou une série
 * vide ne sont pas des échecs. Seules les séries non vides sont mises en cache, par symbole et type de série.
 */
public class AlphaVantageCotations implements Cotations {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AlphaVantageCotations.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map<TypeSerie, String> FONCTIONS = Map.of(
            TypeSerie.JOURNALIERE, "TIME_SERIES_DAILY", TypeSerie.HEBDOMADAIRE, "TIME_SERIES_WEEKLY");
    private static final Map<TypeSerie, String> CHAMPS_SERIE = Map.of(
            TypeSerie.JOURNALIERE, "Time Series (Daily)", TypeSerie.HEBDOMADAIRE, "Weekly Time Series");

    private final RestClient client;
    private final String adresse;
    private final Optional<String> cle;
    private final CircuitBreaker coupeCircuit;
    private final Cache<CleSerie, List<PointCotation>> cache;

    public AlphaVantageCotations(RestClient client, String adresse, Optional<String> cle,
            ProprietesAlphaVantage proprietes) {
        this(client, adresse, cle, proprietes, Ticker.systemTicker());
    }

    AlphaVantageCotations(RestClient client, String adresse, Optional<String> cle, ProprietesAlphaVantage proprietes,
            Ticker ticker) {
        this.client = client;
        this.adresse = adresse;
        this.cle = cle;
        this.coupeCircuit = CircuitBreaker.of("alphavantage", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(proprietes.seuilEchecs())
                .minimumNumberOfCalls(proprietes.seuilEchecs())
                .failureRateThreshold(100)
                .waitDurationInOpenState(proprietes.dureeOuverture())
                .build());
        this.cache = Caffeine.newBuilder().expireAfterWrite(proprietes.dureeCache()).ticker(ticker).build();
    }

    @Override
    public List<PointCotation> serie(String symbole, TypeSerie type) {
        String nom = nettoye(symbole);
        if (cle.isEmpty()) {
            JOURNAL.warn("Cotations AlphaVantage indisponibles pour {} ({}) : clé absente, aucune requête émise",
                    nom, type);
            return List.of();
        }
        CleSerie cleSerie = new CleSerie(symbole, type);
        List<PointCotation> enCache = cache.getIfPresent(cleSerie);
        if (enCache != null) {
            return enCache;
        }
        try {
            List<PointCotation> points = coupeCircuit.executeCallable(() -> appeler(symbole, nom, type));
            if (!points.isEmpty()) {
                cache.put(cleSerie, points);
            }
            return points;
        } catch (CallNotPermittedException ouvert) {
            JOURNAL.warn("Cotations AlphaVantage indisponibles pour {} ({}) : coupe-circuit ouvert", nom, type);
        } catch (EchecFournisseur echec) {
            JOURNAL.warn("Cotations AlphaVantage indisponibles pour {} ({}) : {}", nom, type, echec.getMessage());
        } catch (Exception erreur) {
            // Le message d'une erreur d'appel peut porter l'URL, donc la clé : seule sa nature est journalisée.
            JOURNAL.error("Cotations AlphaVantage indisponibles pour {} ({}) : {}", nom, type, motif(erreur));
        }
        return List.of();
    }

    private List<PointCotation> appeler(String symbole, String nom, TypeSerie type) {
        UriComponentsBuilder requete = UriComponentsBuilder.fromUriString(adresse)
                .queryParam("function", "{fonction}")
                .queryParam("symbol", "{symbole}");
        if (type == TypeSerie.JOURNALIERE) {
            requete.queryParam("outputsize", "compact");
        }
        URI uri = requete.queryParam("apikey", "{cle}").encode()
                .buildAndExpand(Map.of("fonction", FONCTIONS.get(type), "symbole", symbole, "cle", cle.orElseThrow()))
                .toUri();
        Reponse reponse = client.get().uri(uri).exchange((envoi, retour) -> new Reponse(
                retour.getStatusCode().value(), new String(retour.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        if (reponse.statut() < 200 || reponse.statut() > 299) {
            throw new EchecFournisseur("statut " + reponse.statut());
        }
        JsonNode racine = lire(reponse.corps());
        if (racine.has("Information")) {
            throw new EchecFournisseur("champ Information (limite ou information du fournisseur)");
        }
        if (racine.has("Note")) {
            throw new EchecFournisseur("champ Note (limite du fournisseur)");
        }
        if (racine.has("Error Message")) {
            JOURNAL.warn("Cotations AlphaVantage indisponibles pour {} ({}) : champ Error Message", nom, type);
            return List.of();
        }
        return points(racine.get(CHAMPS_SERIE.get(type)));
    }

    private static JsonNode lire(String corps) {
        try {
            JsonNode racine = JSON.readTree(corps);
            if (racine == null || !racine.isObject()) {
                throw new EchecFournisseur("corps illisible");
            }
            return racine;
        } catch (JacksonException e) {
            throw new EchecFournisseur("corps illisible");
        }
    }

    private static List<PointCotation> points(JsonNode serie) {
        if (serie == null) {
            return List.of();
        }
        if (!serie.isObject()) {
            throw new EchecFournisseur("corps illisible");
        }
        List<PointCotation> points = new ArrayList<>();
        try {
            for (Map.Entry<String, JsonNode> seance : serie.properties()) {
                JsonNode valeurs = seance.getValue();
                points.add(new PointCotation(LocalDate.parse(seance.getKey()), prix(valeurs, "1. open"),
                        prix(valeurs, "2. high"), prix(valeurs, "3. low"), prix(valeurs, "4. close"),
                        Long.parseLong(texte(valeurs, "5. volume"))));
            }
        } catch (RuntimeException e) {
            throw new EchecFournisseur("corps illisible");
        }
        return List.copyOf(points);
    }

    private static BigDecimal prix(JsonNode valeurs, String champ) {
        return new BigDecimal(texte(valeurs, champ));
    }

    private static String texte(JsonNode valeurs, String champ) {
        return valeurs.get(champ).asString();
    }

    private static String motif(Throwable erreur) {
        for (Throwable cause = erreur; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException) {
                return "délai dépassé";
            }
        }
        return "erreur d'appel (" + erreur.getClass().getSimpleName() + ")";
    }

    // Symbole saisi par l'utilisateur : pas de saut de ligne dans le journal.
    private static String nettoye(String symbole) {
        return symbole.replaceAll("[\\r\\n]", "_");
    }

    /** Clé du cache : 1W et 1M partagent la même série journalière. */
    record CleSerie(String symbole, TypeSerie type) {
    }

    private record Reponse(int statut, String corps) {
    }

    /** Échec signalé par le fournisseur ou réponse inexploitable ; compté par le coupe-circuit. */
    static final class EchecFournisseur extends RuntimeException {

        EchecFournisseur(String motif) {
            super(motif, null, false, false);
        }
    }
}
