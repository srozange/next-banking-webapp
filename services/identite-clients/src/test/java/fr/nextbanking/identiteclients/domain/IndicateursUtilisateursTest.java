package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-016-02")
class IndicateursUtilisateursTest {

    private static FicheUtilisateur fiche(long id, Role role, boolean actif) {
        return new FicheUtilisateur(id, "Nom " + id, "login" + id, id + "@example.com", null, role, actif, null,
                LocalDate.of(2024, 2, 1));
    }

    @Test
    void compte_les_clients_les_clients_actifs_les_conseillers_et_tous_les_utilisateurs() {
        List<FicheUtilisateur> utilisateurs = List.of(fiche(10, Role.ADMIN, true), fiche(11, Role.CONSEILLER, true),
                fiche(12, Role.CONSEILLER, false), fiche(1, Role.CLIENT, true), fiche(2, Role.CLIENT, true),
                fiche(3, Role.CLIENT, false), fiche(4, Role.CLIENT, true));

        assertThat(IndicateursUtilisateurs.de(utilisateurs)).isEqualTo(new IndicateursUtilisateurs(4, 3, 2, 7));
    }

    @Test
    void sans_utilisateur_tous_les_indicateurs_sont_nuls() {
        assertThat(IndicateursUtilisateurs.de(List.of())).isEqualTo(new IndicateursUtilisateurs(0, 0, 0, 0));
    }

    @Test
    void un_admin_ou_un_conseiller_actif_n_est_pas_un_client_actif() {
        assertThat(IndicateursUtilisateurs.de(List.of(fiche(10, Role.ADMIN, true), fiche(11, Role.CONSEILLER, true))))
                .isEqualTo(new IndicateursUtilisateurs(0, 0, 1, 2));
    }
}
