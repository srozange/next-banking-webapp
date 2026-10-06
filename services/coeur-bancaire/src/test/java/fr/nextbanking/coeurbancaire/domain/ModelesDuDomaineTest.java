package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Invariants des objets-valeurs du virement : comptes, opérations, seuils, entrées d'audit et remise au traitement.
 */
@Tag("FR-003-09")
class ModelesDuDomaineTest {

    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void un_compte_exige_numero_type_et_solde() {
        assertThatThrownBy(() -> new Compte(1, 1, null, TypeCompte.COURANT, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, " ", TypeCompte.COURANT, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, "FR76", null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, "FR76", TypeCompte.COURANT, null))
                .isInstanceOf(IllegalArgumentException.class);
        Compte compte = new Compte(1, 7, "FR76", TypeCompte.EPARGNE, BigDecimal.ONE);
        assertThat(compte.appartientA(7)).isTrue();
        assertThat(compte.appartientA(8)).isFalse();
    }

    @Test
    void une_operation_exige_tous_ses_champs() {
        assertThatThrownBy(() -> new Operation(1, null, BigDecimal.ONE, BigDecimal.ONE, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, null, BigDecimal.ONE, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, BigDecimal.ONE, null, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, BigDecimal.ONE, BigDecimal.ONE, null, T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, BigDecimal.ONE, BigDecimal.ONE, "x", null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Operation(1, TypeOperation.DEBIT, BigDecimal.ONE, BigDecimal.ONE, "x", T, 1).description())
                .isEqualTo("x");
    }

    @Test
    void une_entree_d_audit_exige_ses_champs() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new EntreeAudit(null, "1", ActionAudit.VIREMENT, null, T, "d", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(id, null, ActionAudit.VIREMENT, null, T, "d", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(id, " ", ActionAudit.VIREMENT, null, T, "d", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(id, "1", null, null, T, "d", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(id, "1", ActionAudit.VIREMENT, null, null, "d", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(id, "1", ActionAudit.VIREMENT, null, T, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new EntreeAudit(id, "1", ActionAudit.VIREMENT, null, T, "d", 1).virementId()).isEqualTo(1);
    }

    @Test
    @Tag("FR-023-01")
    void une_remise_au_traitement_exige_identifiant_montant_et_horodatage() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new VirementATraiter(null, 1, BigDecimal.ONE, 1, 2, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementATraiter(id, 1, null, 1, 2, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementATraiter(id, 1, BigDecimal.ONE, 1, 2, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new VirementATraiter(id, 1, BigDecimal.ONE, 1, 2, "loyer", T).motif()).isEqualTo("loyer");
    }

    @Test
    void les_libelles_d_operation_ignorent_un_motif_blanc_et_prefixent_un_virement_pending() {
        assertThat(TextesVirement.libelleDebit("FR76-0001-0002", " ", StatutAffichage.SUCCESS))
                .isEqualTo("Virement vers FR76-0001-0002");
        assertThat(TextesVirement.libelleCredit("FR76-0001-0001", null, StatutAffichage.PENDING))
                .isEqualTo("[EN ATTENTE] Virement depuis FR76-0001-0001");
        assertThat(String.format(TextesVirement.TRANSACTION_IMPORTANTE, "5000.00"))
                .isEqualTo("Virement de 5000.00 EUR en attente de validation - transaction importante, "
                        + "verification en cours.");
    }

    @Test
    @Tag("FR-026-03")
    void les_details_d_audit_suivent_le_legacy() {
        assertThat(TextesVirement.detailVirement("100.00", "loyer", "EXECUTE"))
                .isEqualTo("Montant: 100.00 - Motif: loyer - Issue: EXECUTE");
        assertThat(TextesVirement.detailExecution("100.00", "loyer")).isEqualTo("Montant: 100.00 - Motif: loyer");
        assertThat(TextesVirement.detailExecution("100.00", null)).isEqualTo("Montant: 100.00 - Motif: ");
    }
}
