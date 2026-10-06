package fr.nextbanking.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-01")
class ActionAuditTest {

    @Test
    void le_catalogue_ferme_compte_les_13_codes() {
        assertThat(ActionAudit.values()).extracting(Enum::name).containsExactly("LOGIN", "LOGIN_FAILED", "LOGOUT",
                "ADMIN_DEACTIVATE", "ADMIN_ACTIVATE", "ADMIN_LOAN_APPROVE", "ADMIN_LOAN_REFUSE",
                "ADMIN_TRANSFER_RELEASE", "ADMIN_TRANSFER_REJECT", "VIREMENT", "TRANSFER_EXECUTED",
                "TRANSFER_AUTO_REJECT", "TRANSFER_REJECT_INSUFFICIENT_FUNDS");
    }

    @Test
    void reconnait_un_code_du_catalogue_casse_comprise() {
        assertThat(ActionAudit.depuisCode("LOGIN")).contains(ActionAudit.LOGIN);
        assertThat(ActionAudit.depuisCode("TRANSFER_REJECT_INSUFFICIENT_FUNDS"))
                .contains(ActionAudit.TRANSFER_REJECT_INSUFFICIENT_FUNDS);
    }

    @Test
    void rejette_un_code_hors_catalogue() {
        assertThat(ActionAudit.depuisCode("CODE_INCONNU")).isEmpty();
        assertThat(ActionAudit.depuisCode("login")).isEmpty();
        assertThat(ActionAudit.depuisCode(null)).isEmpty();
    }
}
