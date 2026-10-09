package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.Set;

/**
 * Point unique du contrôle de rôle (seule classe qui lit {@code Role.ADMIN}) : décisions de l'administrateur (rôle
 * ADMIN d'abord, puis motif obligatoire, D6) et portefeuille du conseiller (CONSEILLER ou ADMIN).
 */
final class DecisionAdministrateur {

    private DecisionAdministrateur() {
    }

    /** Refuse tout rôle autre qu'ADMIN. */
    static void exigerAdmin(Set<Role> roles) {
        if (!roles.contains(Role.ADMIN)) {
            throw new ErreurVirement(Cause.ROLE_INSUFFISANT);
        }
    }

    /** Refuse tout rôle autre que CONSEILLER ou ADMIN (portefeuille du conseiller). */
    static void exigerConseillerOuAdmin(Set<Role> roles) {
        if (!roles.contains(Role.CONSEILLER) && !roles.contains(Role.ADMIN)) {
            throw new ErreurVirement(Cause.ROLE_INSUFFISANT);
        }
    }

    /** Vérifie le rôle puis rend le motif rogné ; un motif absent ou fait d'espaces est refusé. */
    static String motifRetenu(CommandeDecision commande) {
        exigerAdmin(commande.roles());
        if (commande.motif() == null || commande.motif().isBlank()) {
            throw new ErreurVirement(Cause.MOTIF_DECISION_OBLIGATOIRE);
        }
        return commande.motif().strip();
    }
}
