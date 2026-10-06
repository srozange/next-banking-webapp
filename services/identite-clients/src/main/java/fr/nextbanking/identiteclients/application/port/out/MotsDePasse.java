package fr.nextbanking.identiteclients.application.port.out;

import java.util.Optional;

/**
 * Port sortant : comparaison d'un mot de passe saisi à une empreinte enregistrée.
 */
public interface MotsDePasse {

    /**
     * Compare le mot de passe à l'empreinte. Sans empreinte (client inconnu), la comparaison a quand même lieu,
     * sur une empreinte factice, pour que la durée ne trahisse pas l'existence du compte ; elle rend alors faux.
     */
    boolean correspond(String motDePasse, Optional<String> empreinte);
}
