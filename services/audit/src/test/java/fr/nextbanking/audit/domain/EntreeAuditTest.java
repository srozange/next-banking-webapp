package fr.nextbanking.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-01")
class EntreeAuditTest {

    private static final Instant HORODATAGE = Instant.parse("2026-10-05T08:30:00Z");

    @Test
    void porte_l_entree_recue() {
        EntreeAudit entree = new EntreeAudit(ActionAudit.LOGIN, HORODATAGE, "Connexion réussie", "203.0.113.7", "1");

        assertThat(entree.action()).isEqualTo(ActionAudit.LOGIN);
        assertThat(entree.horodatage()).isEqualTo(HORODATAGE);
        assertThat(entree.detail()).isEqualTo("Connexion réussie");
        assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
        assertThat(entree.utilisateur()).isEqualTo("1");
    }

    @Test
    void le_detail_et_l_adresse_peuvent_etre_absents() {
        EntreeAudit entree = new EntreeAudit(ActionAudit.TRANSFER_EXECUTED, HORODATAGE, null, null, "SYSTEM");

        assertThat(entree.detail()).isNull();
        assertThat(entree.adresseIp()).isNull();
    }

    @Test
    void refuse_une_entree_sans_code_horodatage_ou_utilisateur() {
        assertThatThrownBy(() -> new EntreeAudit(null, HORODATAGE, "d", null, "1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ActionAudit.LOGIN, null, "d", null, "1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ActionAudit.LOGIN, HORODATAGE, "d", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ActionAudit.LOGIN, HORODATAGE, "d", null, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
