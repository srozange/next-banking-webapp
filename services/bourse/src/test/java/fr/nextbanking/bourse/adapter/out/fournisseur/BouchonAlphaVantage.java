package fr.nextbanking.bourse.adapter.out.fournisseur;

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
 * Bouchon local du fournisseur AlphaVantage (aucun réseau, aucune clé réelle) : réponse réglable, délai de réponse,
 * compteur et journal des requêtes reçues.
 */
public final class BouchonAlphaVantage implements AutoCloseable {

    /** Série journalière : deux points dans le dernier mois au 2026-11-17, un plus ancien. */
    public static final String SERIE_JOURNALIERE = """
            {"Meta Data":{"2. Symbol":"AI.PA"},"Time Series (Daily)":{
            "2026-11-17":{"1. open":"175.2000","2. high":"176.0500","3. low":"174.1000","4. close":"175.9000","5. volume":"1234567"},
            "2026-09-01":{"1. open":"160.0000","2. high":"161.0000","3. low":"159.0000","4. close":"160.5000","5. volume":"1000"},
            "2026-10-19":{"1. open":"170.1000","2. high":"171.0000","3. low":"169.9000","4. close":"170.7700","5. volume":"987654"}}}
            """;
    public static final String SERIE_HEBDOMADAIRE = """
            {"Meta Data":{},"Weekly Time Series":{
            "2026-11-13":{"1. open":"170.0000","2. high":"177.0000","3. low":"168.0000","4. close":"175.0000","5. volume":"5000000"},
            "2025-11-14":{"1. open":"150.0000","2. high":"151.0000","3. low":"149.0000","4. close":"150.5000","5. volume":"4000000"}}}
            """;

    private final HttpServer serveur;
    private final ExecutorService executeur = Executors.newCachedThreadPool();
    private final List<String> requetes = new CopyOnWriteArrayList<>();
    private volatile int statut = 200;
    private volatile String corps = SERIE_JOURNALIERE;
    private volatile Duration attente = Duration.ZERO;

    public BouchonAlphaVantage() {
        try {
            serveur = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        serveur.setExecutor(executeur);
        serveur.createContext("/", echange -> {
            requetes.add(echange.getRequestURI().getRawQuery());
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

    /** Adresse à donner dans {@code ALPHA_VANTAGE_BASE_URL}. */
    public String adresse() {
        return "http://localhost:" + serveur.getAddress().getPort() + "/query";
    }

    public BouchonAlphaVantage repondre(int statut, String corps) {
        this.statut = statut;
        this.corps = corps;
        this.attente = Duration.ZERO;
        return this;
    }

    public BouchonAlphaVantage repondreApres(Duration attente, String corps) {
        repondre(200, corps);
        this.attente = attente;
        return this;
    }

    public int nombreDeRequetes() {
        return requetes.size();
    }

    /** Chaînes de requête reçues, dans l'ordre. */
    public List<String> requetes() {
        return List.copyOf(requetes);
    }

    @Override
    public void close() {
        serveur.stop(0);
        executeur.shutdownNow();
    }
}
