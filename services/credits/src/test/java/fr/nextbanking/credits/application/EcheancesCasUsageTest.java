package fr.nextbanking.credits.application;

import static fr.nextbanking.credits.application.DepotEcheancesEnMemoire.MAINTENANT;
import static fr.nextbanking.credits.application.DepotEcheancesEnMemoire.echeance;
import static fr.nextbanking.credits.application.DepotEcheancesEnMemoire.pret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.StatutPret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class EcheancesCasUsageTest {

    private final DepotEcheancesEnMemoire depot = new DepotEcheancesEnMemoire();
    private final ConsulterEcheancesCasUsage consulter = new ConsulterEcheancesCasUsage(depot);
    private final PayerEcheanceCasUsage payer = new PayerEcheanceCasUsage(depot, depot.transaction, depot.horloge);
    private final SupprimerPretCasUsage supprimer = new SupprimerPretCasUsage(depot);

    @BeforeEach
    void donnees() {
        depot.prets.add(pret(7L, 1L, StatutPret.EN_COURS));
        depot.prets.add(pret(8L, 2L, StatutPret.EN_COURS));
        depot.prets.add(pret(9L, 1L, StatutPret.EN_ATTENTE));
        depot.prets.add(pret(10L, 1L, StatutPret.REFUSE));
        depot.prets.add(pret(11L, 1L, StatutPret.REMBOURSE));
        depot.echeances.add(echeance(70L, 7L, 1, true));
        depot.echeances.add(echeance(71L, 7L, 2, false));
        depot.echeances.add(echeance(72L, 7L, 3, false));
        depot.echeances.add(echeance(80L, 8L, 1, false));
        depot.echeances.add(echeance(110L, 11L, 1, true));
    }

    @Test
    @Tag("FR-007-11")
    void les_echeances_d_un_pret_en_cours_ou_rembourse_sont_rendues_pour_son_proprietaire() {
        assertThat(consulter.consulter(1L, 7L)).extracting(Echeance::id).containsExactly(70L, 71L, 72L);
        assertThat(consulter.consulter(1L, 11L)).extracting(Echeance::id).containsExactly(110L);
    }

    @Test
    @Tag("FR-007-11")
    void un_pret_en_attente_ou_refuse_a_un_echeancier_vide_sans_lecture() {
        assertThat(consulter.consulter(1L, 9L)).isEmpty();
        assertThat(consulter.consulter(1L, 10L)).isEmpty();
        assertThat(depot.journal).noneMatch(appel -> appel.startsWith("echeances"));
    }

    @Test
    @Tag("FR-007-11")
    void un_pret_inconnu_ou_d_un_autre_client_est_refuse() {
        assertThatThrownBy(() -> consulter.consulter(1L, 99L)).isInstanceOf(PretIntrouvable.class);
        assertThatThrownBy(() -> consulter.consulter(1L, 8L)).isInstanceOf(AccesPretRefuse.class);
    }

    @Test
    @Tag("FR-007-12")
    void le_paiement_marque_l_echeance_a_l_instant_de_l_horloge_sans_passer_le_pret_rembourse_s_il_en_reste() {
        Echeance payee = payer.payer(1L, 71L);

        assertThat(payee.paye()).isTrue();
        assertThat(payee.datePaiement()).isEqualTo(MAINTENANT);
        assertThat(depot.journal).containsExactly("debut", "echeanceParId 71", "parId 7", "verrouiller 7",
                "payer 71 " + MAINTENANT, "nonPayees 7", "echeanceParId 71", "validation");
    }

    @Test
    @Tag("FR-007-12")
    void une_echeance_deja_payee_garde_sa_date() {
        Echeance payee = payer.payer(1L, 70L);

        assertThat(payee.datePaiement()).isEqualTo(java.time.Instant.parse("2026-09-01T08:00:00Z"));
    }

    @Test
    @Tag("FR-007-12")
    @Tag("FR-007-13")
    void la_derniere_echeance_payee_fait_passer_le_pret_rembourse_par_transition_conditionnelle() {
        payer.payer(1L, 71L);
        depot.journal.clear();

        payer.payer(1L, 72L);

        assertThat(depot.journal).contains("transitionner EN_COURS->REMBOURSE");
    }

    @Test
    @Tag("FR-007-12")
    void le_paiement_d_une_echeance_d_autrui_ou_inconnue_est_refuse_sans_rien_modifier() {
        assertThatThrownBy(() -> payer.payer(1L, 80L)).isInstanceOf(PaiementRefuse.class);
        assertThatThrownBy(() -> payer.payer(1L, 999L)).isInstanceOf(EcheanceIntrouvable.class);

        assertThat(depot.journal).noneMatch(appel -> appel.startsWith("payer") || appel.startsWith("verrouiller"));
    }

    @Test
    @Tag("FR-007-12")
    void une_echeance_dont_le_pret_a_disparu_est_refusee() {
        depot.echeances.add(echeance(900L, 404L, 1, false));

        assertThatThrownBy(() -> payer.payer(1L, 900L)).isInstanceOf(PaiementRefuse.class);
    }

    @Test
    @Tag("FR-007-12")
    void une_echeance_supprimee_pendant_le_paiement_est_introuvable() {
        PayerEcheanceCasUsage concurrent = new PayerEcheanceCasUsage(new DepotEcheancesEnMemoire() {
            private int lectures;

            @Override
            public java.util.Optional<Echeance> echeanceParId(long echeanceId) {
                return lectures++ == 0 ? java.util.Optional.of(echeance(1L, 7L, 1, false)) : java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<fr.nextbanking.credits.domain.Pret> parId(long pretId) {
                return java.util.Optional.of(pret(7L, 1L, StatutPret.EN_COURS));
            }
        }, depot.transaction, depot.horloge);

        assertThatThrownBy(() -> concurrent.payer(1L, 1L)).isInstanceOf(EcheanceIntrouvable.class);
    }

    @Test
    @Tag("FR-007-12")
    void une_panne_inattendue_du_paiement_devient_un_echec_sans_detail() {
        depot.panne = new IllegalStateException("base coupée");

        assertThatThrownBy(() -> payer.payer(1L, 71L)).isInstanceOf(PaiementEnEchec.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @Tag("FR-007-13")
    void la_suppression_d_un_pret_du_proprietaire_le_retire_quel_que_soit_son_statut() {
        supprimer.supprimer(1L, 9L);
        supprimer.supprimer(1L, 7L);

        assertThat(depot.prets).extracting(p -> p.id()).containsExactly(8L, 10L, 11L);
    }

    @Test
    @Tag("FR-007-13")
    void la_suppression_d_un_pret_d_autrui_ou_inconnu_est_refusee_sans_rien_supprimer() {
        assertThatThrownBy(() -> supprimer.supprimer(1L, 8L)).isInstanceOf(SuppressionRefusee.class);
        assertThatThrownBy(() -> supprimer.supprimer(1L, 99L)).isInstanceOf(PretIntrouvable.class);

        assertThat(depot.journal).noneMatch(appel -> appel.startsWith("supprimer"));
    }

    @Test
    @Tag("FR-007-13")
    void une_panne_inattendue_de_la_suppression_devient_un_echec_sans_detail() {
        depot.panne = new IllegalStateException("base coupée");

        assertThatThrownBy(() -> supprimer.supprimer(1L, 7L)).isInstanceOf(SuppressionEnEchec.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
