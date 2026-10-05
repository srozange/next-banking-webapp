package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Temoin;

/**
 * Port sortant : conserver les témoins écrits.
 */
public interface DepotTemoins {

    /** Enregistre le témoin et rend son identifiant. */
    long enregistrer(Temoin temoin);
}
