package fr.nextbanking.identiteclients.application.port.out;

import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import java.util.List;

/**
 * Port sortant : annuaire de tous les utilisateurs, pour l'administration.
 */
public interface AnnuaireUtilisateurs {

    /** Utilisateurs non supprimés, tous rôles, actifs et inactifs, avec le nom complet de leur conseiller. */
    List<FicheUtilisateur> tous();
}
