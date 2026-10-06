package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PretDecideTest {

    private static final UUID ID = UUID.fromString("7a1d2c3b-4e5f-4a6b-8c7d-9e0f1a2b3c4d");
    private static final Instant MAINTENANT = Instant.parse("2026-10-06T08:30:01Z");

    @Test
    @Tag("FR-024-06")
    void l_approbation_donne_en_cours_et_une_confirmation_aux_textes_du_legacy() {
        PretDecide decide = PretDecide.de(ID, ScoringTest.pret(TypePret.CONSOMMATION, "10000.00", 36),
                DecisionScoring.APPROUVE, MAINTENANT);

        assertThat(decide.statut()).isEqualTo(StatutPret.EN_COURS);
        assertThat(decide.pretId()).isEqualTo(42L);
        assertThat(decide.clientId()).isEqualTo(5L);
        assertThat(decide.notification()).isEqualTo(new Notification(5L, TypeNotification.CONFIRMATION,
                "Demande de prêt approuvée",
                "Votre demande de pret Consommation de 10000.00 EUR a ete approuvee par le service credit."));
    }

    @Test
    @Tag("FR-024-07")
    void le_refus_donne_refuse_et_une_alerte_aux_textes_du_legacy() {
        PretDecide decide = PretDecide.de(ID, ScoringTest.pret(TypePret.AUTO, "40000.01", 36),
                DecisionScoring.REFUSE, MAINTENANT);

        assertThat(decide.statut()).isEqualTo(StatutPret.REFUSE);
        assertThat(decide.notification()).isEqualTo(new Notification(5L, TypeNotification.ALERTE,
                "Demande de prêt refusée",
                "Votre demande de pret Automobile de 40000.01 EUR a ete refusee (plafond depasse ou duree excessive)."));
    }

    @Test
    @Tag("FR-024-05")
    @Tag("BUG-078")
    void le_pret_sans_type_a_le_libelle_type_non_reconnu() {
        Notification notification = Notification.de(ScoringTest.pret(null, "10000.00", 60), DecisionScoring.REFUSE);

        assertThat(notification.message()).isEqualTo(
                "Votre demande de pret (type non reconnu) de 10000.00 EUR a ete refusee (plafond depasse ou duree excessive).");
    }

    @Test
    @Tag("FR-024-07")
    void les_libelles_des_types() {
        assertThat(List.of(TypePret.values()).stream().map(TypePret::libelle).toList())
                .containsExactly("Immobilier", "Automobile", "Consommation", "Travaux", "Etudiant");
    }

    @Test
    void les_champs_obligatoires_et_le_statut_sont_controles() {
        Notification notification = Notification.de(ScoringTest.pret(TypePret.AUTO, "1.00", 6),
                DecisionScoring.APPROUVE);

        assertThatThrownBy(() -> new PretDecide(null, 1L, 1L, StatutPret.EN_COURS, notification, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PretDecide(ID, 1L, 1L, StatutPret.EN_ATTENTE, notification, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PretDecide(ID, 1L, 1L, StatutPret.EN_COURS, notification, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new PretDecide(ID, 1L, 1L, StatutPret.REFUSE, notification, MAINTENANT).statut())
                .isEqualTo(StatutPret.REFUSE);
    }

    @Test
    @Tag("FR-024-06")
    void l_echeancier_a_une_echeance_par_mois_non_payee_datee_du_debut_plus_le_numero() {
        Pret pret = ScoringTest.pret(TypePret.CONSOMMATION, "10000.00", 36);

        List<EcheancePret> echeancier = EcheancePret.echeancier(pret);

        assertThat(echeancier).hasSize(36);
        assertThat(echeancier.get(0).numero()).isEqualTo(1);
        assertThat(echeancier.get(0).dateEcheance()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(echeancier.get(35).numero()).isEqualTo(36);
        assertThat(echeancier.get(35).dateEcheance()).isEqualTo(LocalDate.of(2029, 10, 1));
        assertThat(echeancier.get(0).total()).isEqualByComparingTo("293.02");
        assertThat(echeancier.get(35).capitalRestant()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
