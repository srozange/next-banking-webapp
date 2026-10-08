package fr.nextbanking.credits.domain;

/**
 * Prêt présenté à l'administrateur avec le nom complet de son client (repli : l'identifiant du client en texte quand
 * la projection ne le connaît pas).
 */
public record PretAvecClient(Pret pret, String nomClient) {

    /** Prêt avec le nom du client, ou son identifiant en texte si le nom est absent. */
    public static PretAvecClient de(Pret pret, String nomClient) {
        return new PretAvecClient(pret, nomClient == null ? Long.toString(pret.clientId()) : nomClient);
    }
}
