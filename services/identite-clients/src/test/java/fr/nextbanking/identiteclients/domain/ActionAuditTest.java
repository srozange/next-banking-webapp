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
                ActionAudit.LOGOUT);
        assertThat(ActionAudit.LOGIN.detail()).isEqualTo("Connexion réussie");
        assertThat(ActionAudit.LOGIN_FAILED.detail()).isEqualTo("Tentative échouée");
        assertThat(ActionAudit.LOGOUT.detail()).isEqualTo("Déconnexion");
    }
}
