package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-02")
class EntreeAuditTest {

    private static final UUID ID = UUID.fromString("9a2c6e1d-4b7f-4a3e-8c5d-2e1f0a9b8c7d");
    private static final Instant INSTANT = Instant.parse("2026-10-09T10:15:00Z");

    @Test
    void l_approbation_porte_le_code_et_le_detail_du_catalogue() {
        EntreeAudit entree = EntreeAudit.decision(ID, "10", true, "203.0.113.7", INSTANT, 42L);

        assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_LOAN_APPROVE);
        assertThat(entree.detail()).isEqualTo("Approbation prêt id=42");
        assertThat(entree.utilisateur()).isEqualTo("10");
        assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
        assertThat(entree.pretId()).isEqualTo(42L);
    }

    @Test
    void le_refus_porte_le_code_et_le_detail_du_catalogue() {
        EntreeAudit entree = EntreeAudit.decision(ID, "10", false, null, INSTANT, 42L);

        assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_LOAN_REFUSE);
        assertThat(entree.detail()).isEqualTo("Refus prêt id=42");
    }

    @Test
    void refuse_les_champs_obligatoires_absents() {
        assertThatThrownBy(() -> new EntreeAudit(null, "10", ActionAudit.ADMIN_LOAN_APPROVE, null, INSTANT, "d", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, null, ActionAudit.ADMIN_LOAN_APPROVE, null, INSTANT, "d", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, " ", ActionAudit.ADMIN_LOAN_APPROVE, null, INSTANT, "d", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, "10", null, null, INSTANT, "d", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, "10", ActionAudit.ADMIN_LOAN_APPROVE, null, null, "d", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, "10", ActionAudit.ADMIN_LOAN_APPROVE, null, INSTANT, null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
