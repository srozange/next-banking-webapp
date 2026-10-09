package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.credits.domain.Role;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Point unique du contrôle de rôle : ADMIN seul, ou CONSEILLER ou ADMIN pour le portefeuille. */
@Tag("FR-030-07")
@Tag("FR-015-01")
class DroitsAdministrateurTest {

    @Test
    void le_conseiller_et_l_admin_passent_l_exigence_conseiller_ou_admin() {
        assertThatCode(() -> DroitsAdministrateur.exigerConseillerOuAdmin(Set.of(Role.CONSEILLER)))
                .doesNotThrowAnyException();
        assertThatCode(() -> DroitsAdministrateur.exigerConseillerOuAdmin(Set.of(Role.ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    void un_client_ou_aucun_role_est_refuse() {
        assertThatThrownBy(() -> DroitsAdministrateur.exigerConseillerOuAdmin(Set.of(Role.CLIENT)))
                .isInstanceOf(AccesAdministrationRefuse.class);
        assertThatThrownBy(() -> DroitsAdministrateur.exigerConseillerOuAdmin(Set.of()))
                .isInstanceOf(AccesAdministrationRefuse.class);
    }

    @Test
    void le_conseiller_ne_passe_pas_l_exigence_admin() {
        assertThatThrownBy(() -> DroitsAdministrateur.exigerAdmin(Set.of(Role.CONSEILLER)))
                .isInstanceOf(AccesAdministrationRefuse.class);
    }
}
