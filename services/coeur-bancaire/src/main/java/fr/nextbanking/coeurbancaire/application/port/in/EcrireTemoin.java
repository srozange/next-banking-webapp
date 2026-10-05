package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Temoin;

/**
 * Port entrant : écrire un témoin.
 */
public interface EcrireTemoin {

    Temoin ecrire(String libelle);
}
