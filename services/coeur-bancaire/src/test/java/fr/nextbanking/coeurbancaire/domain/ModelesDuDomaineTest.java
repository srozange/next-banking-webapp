package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Invariants des objets-valeurs du virement : comptes, opérations, seuils, entrées d'audit et remise au traitement.
 */
@Tag("FR-003-09")
@Tag("FR-003-14")
class ModelesDuDomaineTest {

    private static final Montant UN = Montant.lire("1.00");
    private static final Instant T = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void un_compte_exige_numero_type_et_solde() {
        assertThatThrownBy(() -> new Compte(1, 1, null, TypeCompte.COURANT, UN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, " ", TypeCompte.COURANT, UN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, "FR76", null, UN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Compte(1, 1, "FR76", TypeCompte.COURANT, null))
                .isInstanceOf(IllegalArgumentException.class);
        Compte compte = new Compte(1, 7, "FR76", TypeCompte.EPARGNE, UN);
        assertThat(compte.appartientA(7)).isTrue();
        assertThat(compte.appartientA(8)).isFalse();
    }

    @Test
    void une_operation_exige_tous_ses_champs() {
        assertThatThrownBy(() -> new Operation(1, null, UN, UN, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, null, UN, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, UN, null, "x", T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, UN, UN, null, T, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Operation(1, TypeOperation.DEBIT, UN, UN, "x", null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Operation(1, TypeOperation.DEBIT, UN, UN, "x", T, 1).description())
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
        assertThatThrownBy(() -> new VirementATraiter(null, 1, UN, 1, new CompteId(2), Motif.AUCUN, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementATraiter(id, 1, null, 1, new CompteId(2), Motif.AUCUN, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementATraiter(id, 1, UN, 1, new CompteId(2), Motif.AUCUN, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new VirementATraiter(id, 1, UN, 1, new CompteId(2), Motif.lire("loyer"), T).motif().texte())
                .isEqualTo("loyer");
    }

    @Test
    @Tag("FR-003-14")
    void une_notification_et_une_decision_exigent_leurs_champs() {
        NotificationVirement notification = new NotificationVirement(1, TypeNotification.CONFIRMATION, null, "x");
        assertThatThrownBy(() -> new NotificationVirement(1, null, null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationVirement(1, TypeNotification.ALERTE, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationVirement(1, TypeNotification.ALERTE, null, " "))
                .isInstanceOf(IllegalArgumentException.class);
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new VirementDecide(null, 1, 1, EtatVirement.EXECUTE, notification, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementDecide(id, 1, 1, null, notification, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementDecide(id, 1, 1, EtatVirement.EXECUTE, null, T))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VirementDecide(id, 1, 1, EtatVirement.EXECUTE, notification, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new VirementDecide(id, 1, 1, EtatVirement.EXECUTE, notification, T).notification())
                .isEqualTo(notification);
    }

    @Test
    @Tag("FR-003-14")
    void les_messages_de_notification_et_les_descriptions_d_alerte_sont_ceux_du_legacy() {
        assertThat(TextesVirement.confirmation("100.00", "FR76-0001-0002", null))
                .isEqualTo("Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme");
        assertThat(TextesVirement.confirmation("100.00", "FR76-0001-0002", " "))
                .isEqualTo("Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme");
        assertThat(TextesVirement.confirmation("100.00", "FR76-0001-0002", "loyer"))
                .isEqualTo("Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme (motif : loyer)");
        assertThat(TextesVirement.transactionImportante("5000.00")).startsWith("Virement de 5000.00 EUR en attente");
        assertThat(TextesVirement.descriptionMontantTresEleve("20000.00", "15000"))
                .isEqualTo("Transaction amount 20000.00 exceeds the very-large threshold of 15000");
        assertThat(TextesVirement.descriptionHeureInhabituelle(3)).isEqualTo(
                "Transaction initiated at hour 3 which is outside normal operating hours (06:00-23:00)");
        assertThat(TextesVirement.descriptionIbanEtranger("DE89")).isEqualTo(
                "Target IBAN DE89 does not start with FR (possible foreign transfer)");
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
