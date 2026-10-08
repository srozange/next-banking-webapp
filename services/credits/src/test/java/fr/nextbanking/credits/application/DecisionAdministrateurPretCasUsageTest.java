package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.JournalAudit;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.ActionAudit;
import fr.nextbanking.credits.domain.EntreeAudit;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.Role;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

/**
 * Décision manuelle de l'administrateur : rôle, transition conditionnelle, échéancier, audit indépendant écrit avant la
 * fin de la transaction métier, prêt ignoré, erreur sans détail.
 */
class DecisionAdministrateurPretCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-09T10:15:00Z");
    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);

    private final List<String> journal = new ArrayList<>();
    private final PretRepository prets = mock(PretRepository.class);
    private final JournalAudit audit = mock(JournalAudit.class);
    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            journal.add("debut");
            T resultat = travail.get();
            journal.add("validation");
            return resultat;
        }

        @Override
        public <T> T executerIndependamment(Supplier<T> travail) {
            journal.add("debut independante");
            T resultat = travail.get();
            journal.add("validation independante");
            return resultat;
        }
    };
    private final Horloge horloge = mock(Horloge.class);
    private final DecisionAdministrateurPretCasUsage casUsage =
            new DecisionAdministrateurPretCasUsage(prets, audit, transaction, horloge);

    private static Pret pret(StatutPret statut) {
        return new Pret(42L, 5L, TypePret.CONSOMMATION, new BigDecimal("10000.00"), new BigDecimal("3.5000"), 36,
                new BigDecimal("293.02"), new BigDecimal("10548.72"), new BigDecimal("548.72"), "Voiture", statut,
                LocalDate.of(2026, 10, 1), LocalDate.of(2029, 10, 1));
    }

    private void prete(StatutPret statut) {
        when(prets.parId(42L)).thenReturn(Optional.of(pret(statut)));
        when(prets.transitionner(anyLong(), any(), any())).thenAnswer(appel -> {
            journal.add("transitionner " + appel.getArgument(1) + "->" + appel.getArgument(2));
            return true;
        });
        when(horloge.instant()).thenReturn(MAINTENANT);
        when(audit.consigner(any())).thenAnswer(appel -> {
            journal.add("consigner");
            return 77L;
        });
    }

    @Test
    @Tag("FR-016-04")
    @Tag("FR-016-06")
    @Tag("FR-026-02")
    @Tag("FR-026-05")
    void l_approbation_passe_en_cours_ecrit_l_audit_en_independante_puis_cree_les_36_echeances() {
        prete(StatutPret.EN_ATTENTE);

        casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7");

        assertThat(journal).containsExactly("debut", "transitionner EN_ATTENTE->EN_COURS", "debut independante",
                "consigner", "validation independante", "validation");
        ArgumentCaptor<EntreeAudit> entree = ArgumentCaptor.forClass(EntreeAudit.class);
        verify(audit).consigner(entree.capture());
        assertThat(entree.getValue().utilisateur()).isEqualTo("10");
        assertThat(entree.getValue().action()).isEqualTo(ActionAudit.ADMIN_LOAN_APPROVE);
        assertThat(entree.getValue().detail()).isEqualTo("Approbation prêt id=42");
        assertThat(entree.getValue().adresseIp()).isEqualTo("203.0.113.7");
        assertThat(entree.getValue().horodatage()).isEqualTo(MAINTENANT);
        assertThat(entree.getValue().pretId()).isEqualTo(42L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<fr.nextbanking.credits.domain.EcheancePret>> echeances = ArgumentCaptor.forClass(List.class);
        verify(prets).creerEcheancier(ArgumentMatchers.eq(42L), echeances.capture());
        assertThat(echeances.getValue()).hasSize(36);
        verify(audit).publier(77L);
    }

    @Test
    @Tag("FR-016-05")
    @Tag("FR-016-06")
    @Tag("FR-026-02")
    void le_refus_passe_refuse_sans_echeancier_et_ecrit_l_audit() {
        prete(StatutPret.EN_ATTENTE);

        casUsage.refuser(ADMIN, "10", 42L, "203.0.113.7");

        assertThat(journal).contains("transitionner EN_ATTENTE->REFUSE");
        ArgumentCaptor<EntreeAudit> entree = ArgumentCaptor.forClass(EntreeAudit.class);
        verify(audit).consigner(entree.capture());
        assertThat(entree.getValue().action()).isEqualTo(ActionAudit.ADMIN_LOAN_REFUSE);
        assertThat(entree.getValue().detail()).isEqualTo("Refus prêt id=42");
        verify(prets, never()).creerEcheancier(anyLong(), any());
        verify(audit).publier(77L);
    }

    @Test
    @Tag("FR-016-04")
    @Tag("FR-016-05")
    void un_pret_qui_n_est_plus_en_attente_est_ignore_sans_echeancier_ni_audit() {
        for (StatutPret statut : List.of(StatutPret.EN_COURS, StatutPret.REFUSE, StatutPret.REMBOURSE,
                StatutPret.DEFAUT)) {
            prete(statut);

            casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7");
            casUsage.refuser(ADMIN, "10", 42L, "203.0.113.7");
        }

        verify(prets, never()).transitionner(anyLong(), any(), any());
        verify(prets, never()).creerEcheancier(anyLong(), any());
        verify(audit, never()).consigner(any());
        verify(audit, never()).publier(anyLong());
    }

    @Test
    @Tag("FR-016-04")
    void un_pret_inconnu_est_ignore_sans_erreur() {
        when(prets.parId(42L)).thenReturn(Optional.empty());

        casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7");

        verify(audit, never()).consigner(any());
    }

    @Test
    @Tag("FR-016-04")
    @Tag("FR-016-05")
    void une_transition_perdue_contre_une_decision_concurrente_est_ignoree_sans_audit() {
        prete(StatutPret.EN_ATTENTE);
        when(prets.transitionner(42L, StatutPret.EN_ATTENTE, StatutPret.EN_COURS)).thenReturn(false);

        casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7");

        verify(prets, never()).creerEcheancier(anyLong(), any());
        verify(audit, never()).consigner(any());
    }

    @Test
    @Tag("FR-030-07")
    void un_role_autre_qu_admin_est_refuse_sans_rien_lire_ni_modifier() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER), Set.<Role>of())) {
            assertThatThrownBy(() -> casUsage.approuver(roles, "1", 42L, "203.0.113.7"))
                    .isInstanceOf(AccesAdministrationRefuse.class);
            assertThatThrownBy(() -> casUsage.refuser(roles, "1", 42L, "203.0.113.7"))
                    .isInstanceOf(AccesAdministrationRefuse.class);
        }

        verify(prets, never()).parId(anyLong());
        verify(audit, never()).consigner(any());
    }

    @Test
    @Tag("FR-016-07")
    @Tag("BUG-054")
    @Tag("FR-026-04")
    void une_erreur_pendant_l_echeancier_annule_la_decision_mais_l_audit_deja_ecrit_est_publie() {
        prete(StatutPret.EN_ATTENTE);
        doThrow(new IllegalStateException("détail technique")).when(prets).creerEcheancier(anyLong(), any());

        assertThatThrownBy(() -> casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7"))
                .isInstanceOf(DecisionAdministrateurEnEchec.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        // L'audit a été validé (transaction indépendante) avant l'erreur, la transaction métier n'est pas validée.
        assertThat(journal).contains("validation independante").doesNotContain("validation");
        verify(audit).publier(77L);
    }

    @Test
    @Tag("FR-016-07")
    @Tag("BUG-054")
    void une_erreur_a_la_lecture_du_pret_est_une_decision_en_echec() {
        when(prets.parId(42L)).thenThrow(new IllegalStateException("base indisponible"));

        assertThatThrownBy(() -> casUsage.refuser(ADMIN, "10", 42L, "203.0.113.7"))
                .isInstanceOf(DecisionAdministrateurEnEchec.class);

        verify(audit, never()).consigner(any());
    }

    @Test
    @Tag("FR-016-04")
    void un_echec_de_publication_de_l_audit_ne_defait_rien() {
        prete(StatutPret.EN_ATTENTE);
        doThrow(new IllegalStateException("kafka")).when(audit).publier(77L);

        casUsage.approuver(ADMIN, "10", 42L, "203.0.113.7");

        verify(prets).creerEcheancier(ArgumentMatchers.eq(42L), any());
        assertThat(journal).contains("validation");
    }
}
