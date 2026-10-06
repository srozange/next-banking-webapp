package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bouchon local d'Azure OpenAI (aucun réseau, aucune clé réelle) : réponse réglable, requêtes reçues conservées. */
public final class BouchonAzureOpenAi implements AutoCloseable {

    public static final String REPONSE = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Bonjour !\"}}]}";

    /** Requête reçue. */
    public record Requete(String chemin, String cle, String corps) {
    }

    private final HttpServer serveur;
    private final ExecutorService executeur = Executors.newCachedThreadPool();
    private final List<Requete> requetes = new CopyOnWriteArrayList<>();
    private volatile int statut = 200;
    private volatile String corps = REPONSE;
    private volatile Duration attente = Duration.ZERO;

    public BouchonAzureOpenAi() {
        try {
            serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        serveur.setExecutor(executeur);
        serveur.createContext("/", echange -> {
            String recu = new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requetes.add(new Requete(echange.getRequestURI().getRawPath() + "?" + echange.getRequestURI().getRawQuery(),
                    echange.getRequestHeaders().getFirst("api-key"), recu));
            try {
                Thread.sleep(attente);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().add("Content-Type", "application/json");
            echange.sendResponseHeaders(statut, octets.length);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(octets);
            } catch (IOException e) {
                // Client parti après son délai d'attente.
            }
        });
        serveur.start();
    }

    public String adresse() {
        return "http://localhost:" + serveur.getAddress().getPort();
    }

    public BouchonAzureOpenAi repondre(int statut, String corps) {
        this.statut = statut;
        this.corps = corps;
        this.attente = Duration.ZERO;
        return this;
    }

    public BouchonAzureOpenAi repondreApres(Duration attente, String corps) {
        repondre(200, corps);
        this.attente = attente;
        return this;
    }

    public List<Requete> requetes() {
        return List.copyOf(requetes);
    }

    @Override
    public void close() {
        serveur.stop(0);
        executeur.shutdownNow();
    }
}
