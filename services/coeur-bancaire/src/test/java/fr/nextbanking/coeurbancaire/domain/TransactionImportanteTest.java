package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Virement important exécuté (5 000 à 9 999,99 €, score ≤ 75) : déjà débité, il n'est jamais annoncé « en attente »
 * ni « verification » ; notification ALERTE et opérations portent les textes de FR-003-29 (BUG-009 corrigé).
 */
@Tag("FR-003-29")
@Tag("BUG-009")
class TransactionImportanteTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");
    private static final SeuilsFraude SEUILS =
            new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000"));
    private static final Compte SOURCE =
            new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00"));
    private static final Compte DESTINATAIRE =
            new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("15200.00"));

    private static Virement.Execution executer(String montant, String motif) {
        Soumission soumission = new Soumission(12, 7,
                DemandeVirement.lire(SOURCE.id(), DESTINATAIRE.id(), montant, motif), MAINTENANT);
        return Virement.executer(soumission, SOURCE, DESTINATAIRE, SEUILS);
    }

    @Test
    void aucun_texte_d_un_virement_important_execute_ne_dit_en_attente_ni_verification() {
        Virement.Execution execution = executer("5000.00", "loyer");
        NotificationVirement notification =
                NotificationVirement.pourExecution(execution.virement(), DESTINATAIRE.numero(), SEUILS);

        assertThat(execution.virement().statut()).isEqualTo(StatutAffichage.PENDING);
        assertThat(notification.type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(notification.message())
                .isEqualTo("Virement de 5000.00 EUR vers le compte FR76-0001-0002 execute - transaction importante.");
        assertThat(execution.debit().description())
                .isEqualTo("[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer");
        assertThat(execution.credit().description())
                .isEqualTo("[TRANSACTION IMPORTANTE] Virement depuis FR76-0001-0001 - loyer");
        assertThat(List.of(notification.message(), execution.debit().description(), execution.credit().description()))
                .extracting(texte -> texte.toLowerCase(Locale.ROOT))
                .allSatisfy(texte -> assertThat(texte).doesNotContain("en attente").doesNotContain("verification"));
    }

    @Test
    void sans_motif_la_description_n_a_que_la_mention() {
        Virement.Execution execution = executer("5000.00", "");

        assertThat(execution.debit().description()).isEqualTo("[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002");
    }

    @Test
    void sous_le_seuil_le_virement_est_confirme_sans_mention() {
        Virement.Execution execution = executer("4999.99", "loyer");
        NotificationVirement notification =
                NotificationVirement.pourExecution(execution.virement(), DESTINATAIRE.numero(), SEUILS);

        assertThat(execution.virement().statut()).isEqualTo(StatutAffichage.SUCCESS);
        assertThat(notification.type()).isEqualTo(TypeNotification.CONFIRMATION);
        assertThat(execution.debit().description()).isEqualTo("Virement vers FR76-0001-0002 - loyer");
    }
}
