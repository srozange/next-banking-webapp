package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.AlerteFraude;

/** Port sortant : alertes de fraude. */
public interface AlertesFraude {

    /** Enregistre l'alerte, au statut donné, dans la transaction courante. */
    void enregistrer(AlerteFraude alerte);
}
