package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Point unique du contrôle de rôle : ADMIN seul, ou CONSEILLER ou ADMIN pour le portefeuille. */
@Tag("FR-030-07")
@Tag("FR-015-01")
class DecisionAdministrateurTest {

    @Test
    void le_conseiller_et_l_admin_passent_l_exigence_conseiller_ou_admin() {
        assertThatCode(() -> DecisionAdministrateur.exigerConseillerOuAdmin(Set.of(Role.CONSEILLER)))
                .doesNotThrowAnyException();
        assertThatCode(() -> DecisionAdministrateur.exigerConseillerOuAdmin(Set.of(Role.ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    void un_client_ou_aucun_role_est_refuse() {
        for (Set<Role> roles : java.util.List.of(Set.of(Role.CLIENT), Set.<Role>of())) {
            assertThatThrownBy(() -> DecisionAdministrateur.exigerConseillerOuAdmin(roles))
                    .isInstanceOfSatisfying(ErreurVirement.class,
                            refus -> org.assertj.core.api.Assertions.assertThat(refus.motif())
                                    .isEqualTo(Cause.ROLE_INSUFFISANT));
        }
    }

    @Test
    void le_conseiller_ne_passe_pas_l_exigence_admin() {
        assertThatThrownBy(() -> DecisionAdministrateur.exigerAdmin(Set.of(Role.CONSEILLER)))
                .isInstanceOf(ErreurVirement.class);
    }
}
