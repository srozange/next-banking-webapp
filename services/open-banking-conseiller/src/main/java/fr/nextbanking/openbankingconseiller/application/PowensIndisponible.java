package fr.nextbanking.openbankingconseiller.application;

/**
 * Powens ne répond pas ou répond en erreur (5xx, 404 aux transactions, délai, coupe-circuit ouvert) : jamais présenté
 * comme une liste vide.
 */
public class PowensIndisponible extends RuntimeException {

    public PowensIndisponible(String motif) {
        super(motif, null, false, false);
    }
}
