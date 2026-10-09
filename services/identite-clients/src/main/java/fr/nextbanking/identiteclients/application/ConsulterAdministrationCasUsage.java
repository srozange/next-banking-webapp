package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.ConsulterAdministration;
import fr.nextbanking.identiteclients.application.port.out.AnnuaireUtilisateurs;
import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.IndicateursUtilisateurs;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Cas d'usage : exige ADMIN par {@link DroitsRoles}, puis lit l'annuaire pour les indicateurs, la liste des clients
 * (rôle CLIENT seulement) et la liste de tous les utilisateurs, triées par nom complet puis identifiant.
 */
public class ConsulterAdministrationCasUsage implements ConsulterAdministration {

    private static final Comparator<FicheUtilisateur> ORDRE = Comparator
            .comparing(FicheUtilisateur::nomComplet)
            .thenComparingLong(FicheUtilisateur::id);

    private final AnnuaireUtilisateurs annuaire;

    public ConsulterAdministrationCasUsage(AnnuaireUtilisateurs annuaire) {
        this.annuaire = annuaire;
    }

    @Override
    public IndicateursUtilisateurs indicateurs(Set<Role> rolesActeur) {
        DroitsRoles.exigerAdmin(rolesActeur);
        return IndicateursUtilisateurs.de(annuaire.tous());
    }

    @Override
    public List<FicheUtilisateur> clients(Set<Role> rolesActeur) {
        DroitsRoles.exigerAdmin(rolesActeur);
        return annuaire.tous().stream().filter(FicheUtilisateur::estClient).sorted(ORDRE).toList();
    }

    @Override
    public List<FicheUtilisateur> utilisateurs(Set<Role> rolesActeur) {
        DroitsRoles.exigerAdmin(rolesActeur);
        return annuaire.tous().stream().sorted(ORDRE).toList();
    }
}
