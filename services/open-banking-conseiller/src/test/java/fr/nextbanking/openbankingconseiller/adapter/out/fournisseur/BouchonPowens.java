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

/**
 * Bouchon local de Powens (aucun réseau, aucun secret réel) : réponse réglable, délai de réponse, journal des requêtes
 * reçues (méthode, chemin avec requête, en-tête Authorization).
 */
public final class BouchonPowens implements AutoCloseable {

    /** Requête reçue par le bouchon. */
    public record Requete(String methode, String chemin, String autorisation, String corps) {
    }

    private final HttpServer serveur;
    private final ExecutorService executeur = Executors.newCachedThreadPool();
    private final List<Requete> requetes = new CopyOnWriteArrayList<>();
    private volatile int statut = 200;
    private volatile String corps = "{}";
    private volatile Duration attente = Duration.ZERO;

    public BouchonPowens() {
        try {
            serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        serveur.setExecutor(executeur);
        serveur.createContext("/", echange -> {
            String requete = echange.getRequestURI().getRawQuery();
            requetes.add(new Requete(echange.getRequestMethod(),
                    echange.getRequestURI().getRawPath() + (requete == null ? "" : "?" + requete),
                    echange.getRequestHeaders().getFirst("Authorization"),
                    new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
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

    /** Base de l'API Powens du bouchon. */
    public String base() {
        return "http://localhost:" + serveur.getAddress().getPort() + "/2.0";
    }

    public BouchonPowens repondre(int statut, String corps) {
        this.statut = statut;
        this.corps = corps;
        this.attente = Duration.ZERO;
        return this;
    }

    public BouchonPowens repondreApres(Duration attente, String corps) {
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
