package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.IndicateursUtilisateurs;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-016-01")
@Tag("FR-030-07")
class ConsulterAdministrationCasUsageTest {

    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);
    private static final LocalDate CREATION = LocalDate.of(2024, 2, 1);

    private static final FicheUtilisateur ALICE = new FicheUtilisateur(10L, "Alice Martin", "admin",
            "alice.martin@demobank.fr", "+33612345670", Role.ADMIN, true, null, CREATION);
    private static final FicheUtilisateur SOPHIE = new FicheUtilisateur(11L, "Sophie Durand", "sophie",
            "sophie.durand@demobank.fr", null, Role.CONSEILLER, true, null, CREATION);
    private static final FicheUtilisateur MARIE = new FicheUtilisateur(2L, "Marie Lebrun", "user2",
            "marie.lebrun@example.com", null, Role.CLIENT, false, "Sophie Durand", CREATION);
    private static final FicheUtilisateur JEAN = new FicheUtilisateur(1L, "Jean Dupont", "user1",
            "jean.dupont@example.com", "+33601020304", Role.CLIENT, true, "Sophie Durand", CREATION);
    private static final FicheUtilisateur JEAN_BIS = new FicheUtilisateur(5L, "Jean Dupont", "user5",
            "jean.bis@example.com", null, Role.CLIENT, true, null, CREATION);

    private final List<FicheUtilisateur> annuaire = new ArrayList<>(List.of(SOPHIE, JEAN_BIS, MARIE, ALICE, JEAN));
    private int lectures;
    private final ConsulterAdministrationCasUsage casUsage = new ConsulterAdministrationCasUsage(() -> {
        lectures++;
        return List.copyOf(annuaire);
    });

    @Test
    @Tag("FR-016-02")
    void les_indicateurs_comptent_clients_clients_actifs_conseillers_et_utilisateurs() {
        assertThat(casUsage.indicateurs(ADMIN)).isEqualTo(new IndicateursUtilisateurs(3, 2, 1, 5));
        assertThat(lectures).isEqualTo(1);
    }

    @Test
    @Tag("FR-016-08")
    void la_liste_des_clients_ne_garde_que_le_role_client_triee_par_nom_puis_identifiant() {
        assertThat(casUsage.clients(ADMIN)).containsExactly(JEAN, JEAN_BIS, MARIE);
    }

    @Test
    @Tag("FR-016-13")
    void la_liste_des_utilisateurs_garde_tous_les_roles_triee_par_nom_puis_identifiant() {
        assertThat(casUsage.utilisateurs(ADMIN)).containsExactly(ALICE, JEAN, JEAN_BIS, MARIE, SOPHIE);
    }

    @Test
    @Tag("FR-016-02")
    @Tag("FR-016-08")
    @Tag("FR-016-13")
    void un_annuaire_vide_donne_des_listes_vides_et_des_indicateurs_nuls() {
        annuaire.clear();

        assertThat(casUsage.indicateurs(ADMIN)).isEqualTo(new IndicateursUtilisateurs(0, 0, 0, 0));
        assertThat(casUsage.clients(ADMIN)).isEmpty();
        assertThat(casUsage.utilisateurs(ADMIN)).isEmpty();
    }

    @Test
    void un_client_ou_un_conseiller_est_refuse_sans_lecture() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER), Set.<Role>of())) {
            assertThatThrownBy(() -> casUsage.indicateurs(roles)).isInstanceOf(AccesRefuse.class);
            assertThatThrownBy(() -> casUsage.clients(roles)).isInstanceOf(AccesRefuse.class);
            assertThatThrownBy(() -> casUsage.utilisateurs(roles)).isInstanceOf(AccesRefuse.class);
        }
        assertThat(lectures).isZero();
    }
}
