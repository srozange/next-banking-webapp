package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-01")
@Tag("FR-001-12")
class ActionAuditTest {

    @Test
    void les_codes_de_connexion_portent_le_detail_du_legacy() {
        assertThat(ActionAudit.values()).containsExactly(ActionAudit.LOGIN, ActionAudit.LOGIN_FAILED,
                ActionAudit.LOGOUT, ActionAudit.ADMIN_DEACTIVATE, ActionAudit.ADMIN_ACTIVATE);
        assertThat(ActionAudit.LOGIN.detail()).isEqualTo("Connexion réussie");
        assertThat(ActionAudit.LOGIN_FAILED.detail()).isEqualTo("Tentative échouée");
        assertThat(ActionAudit.LOGOUT.detail()).isEqualTo("Déconnexion");
    }

    @Test
    @Tag("FR-026-02")
    void les_codes_d_administration_portent_l_identifiant_du_client_dans_le_detail() {
        assertThat(ActionAudit.ADMIN_DEACTIVATE.detail(7L)).isEqualTo("Désactivation client id=7");
        assertThat(ActionAudit.ADMIN_ACTIVATE.detail(7L)).isEqualTo("Activation client id=7");
    }
}
