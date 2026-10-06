package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import fr.nextbanking.openbankingconseiller.domain.RoleEchange;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptateur réel d'Azure OpenAI (mode {@code reel}) : Chat Completions, {@code max_tokens} 500, température 0,7
 * (RG-010-018), derrière un coupe-circuit Resilience4j ; les délais (15 s / 30 s) sont ceux du client HTTP. Pas de
 * retry. Sans clé, aucune requête n'est émise. Ni la clé ni le corps du fournisseur ne sont journalisés ni exposés.
 *
 * <p>Sont des échecs comptés par le coupe-circuit : refus 401/403, statut non 2xx, délai, erreur d'appel, corps
 * illisible.
 */
public class AzureOpenAiReel implements ConversationIa {

    private static final Logger JOURNAL = LoggerFactory.getLogger(AzureOpenAiReel.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.7;

    private final RestClient client;
    private final ParametresDeploiementAzure parametres;
    private final CircuitBreaker coupeCircuit;

    public AzureOpenAiReel(RestClient client, ParametresDeploiementAzure parametres, ProprietesAzureOpenAi proprietes) {
        this.client = client;
        this.parametres = parametres;
        this.coupeCircuit = CircuitBreaker.of("azure-openai", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(proprietes.seuilEchecs())
                .minimumNumberOfCalls(proprietes.seuilEchecs())
                .failureRateThreshold(100)
                .waitDurationInOpenState(proprietes.dureeOuverture())
                .build());
    }

    @Override
    public String repondre(String consigne, List<Echange> historique, String message) {
        Optional<String> cle = parametres.cle();
        if (cle.isEmpty()) {
            JOURNAL.warn("Azure OpenAI indisponible : clé absente, aucune requête émise");
            throw ServiceIaIndisponible.indisponible("clé absente");
        }
        String corps = corps(consigne, historique, message);
        try {
            return coupeCircuit.executeCallable(() -> appeler(cle.get(), corps));
        } catch (CallNotPermittedException ouvert) {
            throw ServiceIaIndisponible.indisponible("coupe-circuit ouvert");
        } catch (ServiceIaIndisponible echec) {
            throw echec;
        } catch (Exception erreur) {
            // Le message d'une erreur d'appel peut porter l'URL : seule sa nature est conservée.
            throw ServiceIaIndisponible.indisponible(motif(erreur));
        }
    }

    private String appeler(String cle, String corps) {
        URI uri = UriComponentsBuilder.fromUriString(parametres.endpoint())
                .path("/openai/deployments/{deploiement}/chat/completions")
                .queryParam("api-version", "{version}").encode()
                .buildAndExpand(Map.of("deploiement", parametres.deploiement(), "version", parametres.version()))
                .toUri();
        Reponse reponse = client.post().uri(uri).header("api-key", cle).contentType(MediaType.APPLICATION_JSON)
                .body(corps).exchange((envoi, retour) -> new Reponse(retour.getStatusCode().value(),
                        new String(retour.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        if (reponse.statut() == 401 || reponse.statut() == 403) {
            throw ServiceIaIndisponible.authentification(reponse.statut());
        }
        if (reponse.statut() < 200 || reponse.statut() > 299) {
            throw ServiceIaIndisponible.statut(reponse.statut());
        }
        return contenu(reponse.corps());
    }

    private static String contenu(String corps) {
        try {
            JsonNode contenu = JSON.readTree(corps).path("choices").path(0).path("message").path("content");
            if (!contenu.isString()) {
                throw ServiceIaIndisponible.indisponible("corps illisible");
            }
            return contenu.asString();
        } catch (JacksonException e) {
            throw ServiceIaIndisponible.indisponible("corps illisible");
        }
    }

    private static String corps(String consigne, List<Echange> historique, String message) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", consigne));
        for (Echange echange : historique) {
            messages.add(Map.of("role", echange.role() == RoleEchange.ASSISTANT ? "assistant" : "user",
                    "content", echange.contenu()));
        }
        messages.add(Map.of("role", "user", "content", message));
        Map<String, Object> requete = new LinkedHashMap<>();
        requete.put("messages", messages);
        requete.put("max_tokens", MAX_TOKENS);
        requete.put("temperature", TEMPERATURE);
        return JSON.writeValueAsString(requete);
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
}
