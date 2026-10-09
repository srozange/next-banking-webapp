package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-030-07")
@Tag("FR-015-01")
class DroitsRolesTest {

    @Test
    void seul_l_admin_passe_l_exigence_admin() {
        assertThatCode(() -> DroitsRoles.exigerAdmin(Set.of(Role.ADMIN))).doesNotThrowAnyException();
        assertThatThrownBy(() -> DroitsRoles.exigerAdmin(Set.of(Role.CONSEILLER, Role.CLIENT)))
                .isInstanceOf(AccesRefuse.class);
        assertThatThrownBy(() -> DroitsRoles.exigerAdmin(Set.of())).isInstanceOf(AccesRefuse.class);
        assertThatThrownBy(() -> DroitsRoles.exigerAdmin(null)).isInstanceOf(AccesRefuse.class);
    }

    @Test
    void le_conseiller_et_l_admin_passent_l_exigence_conseiller_ou_admin() {
        assertThatCode(() -> DroitsRoles.exigerConseillerOuAdmin(Set.of(Role.CONSEILLER)))
                .doesNotThrowAnyException();
        assertThatCode(() -> DroitsRoles.exigerConseillerOuAdmin(Set.of(Role.ADMIN))).doesNotThrowAnyException();
        assertThatThrownBy(() -> DroitsRoles.exigerConseillerOuAdmin(Set.of(Role.CLIENT)))
                .isInstanceOf(AccesRefuse.class);
        assertThatThrownBy(() -> DroitsRoles.exigerConseillerOuAdmin(Set.of())).isInstanceOf(AccesRefuse.class);
        assertThatThrownBy(() -> DroitsRoles.exigerConseillerOuAdmin(null)).isInstanceOf(AccesRefuse.class);
    }
}
