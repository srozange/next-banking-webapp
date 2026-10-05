package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Temoin;

/**
 * Port sortant : conserver les témoins écrits.
 */
public interface DepotTemoins {

    void enregistrer(Temoin temoin);
}
