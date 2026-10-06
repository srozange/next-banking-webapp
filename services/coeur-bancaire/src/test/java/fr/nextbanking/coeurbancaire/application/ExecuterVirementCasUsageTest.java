package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Resultat;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.TypeNotification;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cas d'usage du virement immédiat sur un cœur bancaire en mémoire : contrôles dans l'ordre, atomicité, remise au
 * traitement asynchrone, audit indépendant, idempotence.
 */
@Tag("FR-003-14")
class ExecuterVirementCasUsageTest {

    private static final String IP = "203.0.113.7";

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private ExecuterVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("2130.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, new BigDecimal("15200.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, new BigDecimal("500.00")));
        cas = assembler(memoire);
    }

    static ExecuterVirementCasUsage assembler(MemoireCoeurBancaire m) {
        ControleFraudeCasUsage controle = new ControleFraudeCasUsage(m, m, m, m, ControleFraudeTestes.PARAMETRES);
        return new ExecuterVirementCasUsage(m, m, m, m, m, m, m,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000")), controle);
    }

    private static Commande commande(Long source, Long destinataire, String montant, String motif, String cle) {
        return new Commande(7, source, destinataire, montant, motif, cle, IP);
    }

    private ErreurVirement refus(Commande commande) {
        return catchThrowableOfType(ErreurVirement.class, () -> cas.executer(commande));
    }

    private void aucuneEcriture() {
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2130.00");
        assertThat(memoire.comptes.get(2L).solde()).isEqualByComparingTo("15200.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.virements).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
    }

    @Test
    @Tag("FR-003-09")
    @Tag("FR-003-08")
    void un_virement_valide_debite_credite_et_rend_l_ecrit_des_champs_saisis() {
        Resultat resultat = cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(resultat).isEqualTo(new Resultat(12, EtatVirement.EXECUTE, null, StatutAffichage.SUCCESS, 1, 2,
                new BigDecimal("100.00"), "loyer"));
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
        assertThat(memoire.comptes.get(2L).solde()).isEqualByComparingTo("15300.00");
        assertThat(memoire.operations).extracting(o -> o.type(), o -> o.description(), o -> o.soldeApres())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.DEBIT,
                                "Virement vers FR76-0001-0002 - loyer", new BigDecimal("2030.00")),
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.CREDIT,
                                "Virement depuis FR76-0001-0001 - loyer", new BigDecimal("15300.00")));
        assertThat(memoire.journal).containsExactly("verrouiller", "verrou-client", "historique",
                "debiter", "crediter");
    }

    @Test
    @Tag("FR-003-10")
    void a_5000_euros_le_virement_est_pending_et_a_4999_99_il_est_success() {
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));

        Resultat pending = cas.executer(commande(1L, 2L, "5000.00", "loyer", null));
        Resultat succes = cas.executer(commande(1L, 2L, "4999.99", "loyer", null));

        assertThat(pending.statut()).isEqualTo(StatutAffichage.PENDING);
        assertThat(succes.statut()).isEqualTo(StatutAffichage.SUCCESS);
        assertThat(memoire.operations.getFirst().description()).startsWith("[EN ATTENTE] ");
        assertThat(memoire.operations.get(2).description()).doesNotContain("EN ATTENTE");
    }

    @Test
    @Tag("FR-023-01")
    @Tag("FR-003-13")
    void la_remise_au_traitement_est_ecrite_dans_la_transaction_puis_publiee_apres_validation() {
        cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(memoire.outbox).singleElement().satisfies(remise -> {
            assertThat(remise.virementId()).isEqualTo(12);
            assertThat(remise.montant()).isEqualByComparingTo("100.00");
            assertThat(remise.emetteurId()).isEqualTo(7);
            assertThat(remise.compteDestinataireId()).isEqualTo(2);
            assertThat(remise.motif()).isEqualTo("loyer");
            assertThat(remise.horodatage()).isEqualTo(MemoireCoeurBancaire.MAINTENANT);
        });
        assertThat(memoire.publies).containsExactly(101L);
    }

    @Test
    @Tag("FR-003-14")
    @Tag("FR-002-10")
    void une_confirmation_sans_motif_ou_avec_motif_blanc_n_a_pas_de_mention_de_motif() {
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        cas.executer(commande(1L, 2L, "100", null, null));
        cas.executer(commande(1L, 2L, "100", "   ", null));

        assertThat(memoire.decisions).extracting(d -> d.notification().type(), d -> d.notification().message())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeNotification.CONFIRMATION,
                                "Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme"),
                        org.assertj.core.groups.Tuple.tuple(TypeNotification.CONFIRMATION,
                                "Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme"));
    }

    @Test
    @Tag("FR-003-14")
    @Tag("FR-002-10")
    void une_confirmation_avec_motif_le_reprend() {
        cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(memoire.decisions).singleElement().satisfies(decision -> {
            assertThat(decision.notification().message())
                    .isEqualTo("Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme (motif : loyer)");
            assertThat(decision.notification().titre()).isNull();
            assertThat(decision.notification().clientId()).isEqualTo(7);
            assertThat(decision.virementId()).isEqualTo(12);
            assertThat(decision.clientId()).isEqualTo(7);
            assertThat(decision.etat()).isEqualTo(EtatVirement.EXECUTE);
            assertThat(decision.horodatage()).isEqualTo(MemoireCoeurBancaire.MAINTENANT);
        });
    }

    @Test
    @Tag("FR-003-26")
    @Tag("FR-025-01")
    @Tag("FR-025-02")
    void a_5000_euros_la_notification_est_une_alerte_et_a_4999_99_une_confirmation() {
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));

        cas.executer(commande(1L, 2L, "5000.00", "loyer", null));
        cas.executer(commande(1L, 2L, "4999.99", "loyer", null));

        assertThat(memoire.decisions).extracting(d -> d.notification().type(), d -> d.notification().message())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeNotification.ALERTE,
                                "Virement de 5000.00 EUR en attente de validation - transaction importante, "
                                        + "verification en cours."),
                        org.assertj.core.groups.Tuple.tuple(TypeNotification.CONFIRMATION,
                                "Virement de 4999.99 EUR vers le compte FR76-0001-0002 confirme (motif : loyer)"));
    }

    @Test
    @Tag("FR-003-14")
    void une_seule_decision_par_virement_ecrite_dans_la_transaction_et_publiee_avant_la_remise() {
        cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(memoire.decisions).hasSize(1);
        assertThat(memoire.outbox).hasSize(1);
        assertThat(memoire.decisionsPubliees).containsExactly(301L);
        assertThat(memoire.publies).containsExactly(101L);
    }

    @Test
    @Tag("FR-003-02")
    @Tag("FR-003-07")
    void une_saisie_refusee_n_ecrit_rien_et_n_est_pas_auditee() {
        assertThat(refus(commande(null, 2L, "100", null, null)).motif()).isEqualTo(Cause.COMPTES_OBLIGATOIRES);
        assertThat(refus(commande(1L, 1L, "100", null, null)).motif()).isEqualTo(Cause.COMPTES_IDENTIQUES);
        assertThat(refus(commande(1L, 2L, "abc", null, null)).motif()).isEqualTo(Cause.MONTANT_INVALIDE);
        assertThat(refus(commande(1L, 2L, "100", "x".repeat(101), null)).motif()).isEqualTo(Cause.MOTIF_TROP_LONG);

        aucuneEcriture();
        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    @Tag("FR-003-05")
    void un_compte_inexistant_donne_compte_introuvable_sans_ecriture() {
        assertThat(refus(commande(999L, 2L, "100", null, null)).motif()).isEqualTo(Cause.COMPTE_INTROUVABLE);
        assertThat(refus(commande(1L, 999L, "100", null, null)).motif()).isEqualTo(Cause.COMPTE_INTROUVABLE);

        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-05")
    void un_compte_d_un_autre_client_est_refuse_sans_ecriture() {
        assertThat(refus(commande(3L, 2L, "100", null, null)).motif()).isEqualTo(Cause.COMPTE_ETRANGER);
        assertThat(refus(commande(1L, 3L, "100", null, null)).motif()).isEqualTo(Cause.COMPTE_ETRANGER);

        aucuneEcriture();
    }

    @Test
    @Tag("FR-003-06")
    @Tag("BUG-008")
    @Tag("BUG-011")
    void un_solde_insuffisant_refuse_le_virement_sans_ecriture() {
        assertThat(refus(commande(1L, 2L, "9999", null, null)))
                .hasMessage("Solde insuffisant sur le compte source")
                .extracting(ErreurVirement::motif).isEqualTo(Cause.SOLDE_INSUFFISANT);

        aucuneEcriture();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    @Tag("FR-003-06")
    void un_debit_conditionnel_refuse_annule_toute_la_transaction() {
        memoire.debitRefuse = true;

        assertThat(refus(commande(1L, 2L, "100", "loyer", "cle-1")).motif()).isEqualTo(Cause.SOLDE_INSUFFISANT);

        aucuneEcriture();
        assertThat(memoire.cles).isEmpty();
    }

    @Test
    @Tag("FR-023-02")
    @Tag("BUG-073")
    void une_remise_impossible_annule_tout_et_laisse_une_entree_virement_dans_l_audit() {
        memoire.outboxRefusee = true;

        ErreurVirement erreur = refus(commande(1L, 2L, "100", "loyer", null));

        assertThat(erreur.motif()).isEqualTo(Cause.REMISE_IMPOSSIBLE);
        assertThat(erreur).hasMessage("Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard");
        assertThat(erreur.getCause()).hasMessage("outbox refusée");
        aucuneEcriture();
        assertThat(memoire.publies).isEmpty();
        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.VIREMENT);
            assertThat(entree.detail()).isEqualTo("Montant: 100.00 - Motif: loyer - Issue: ECHEC_REMISE");
            assertThat(entree.virementId()).isEqualTo(12);
            assertThat(entree.utilisateur()).isEqualTo("7");
            assertThat(entree.adresseIp()).isEqualTo(IP);
        });
        assertThat(memoire.auditPublie).containsExactly(201L);
    }

    @Test
    @Tag("FR-026-03")
    @Tag("FR-003-11")
    void un_virement_execute_ecrit_virement_puis_transfer_executed_dans_l_audit_independant() {
        cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(memoire.outboxAudit).extracting(EntreeAudit::action, EntreeAudit::detail,
                EntreeAudit::utilisateur, EntreeAudit::adresseIp, EntreeAudit::virementId,
                EntreeAudit::horodatage)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ActionAudit.VIREMENT,
                                "Montant: 100.00 - Motif: loyer - Issue: EXECUTE", "7", IP, 12L,
                                MemoireCoeurBancaire.MAINTENANT),
                        org.assertj.core.groups.Tuple.tuple(ActionAudit.TRANSFER_EXECUTED,
                                "Montant: 100.00 - Motif: loyer", "7", IP, 12L, MemoireCoeurBancaire.MAINTENANT));
        assertThat(memoire.auditPublie).containsExactly(201L, 202L);
    }

    @Test
    @Tag("FR-026-03")
    void un_echec_d_ecriture_de_l_audit_n_annule_pas_le_virement_deja_valide() {
        memoire.auditRefuse = true;

        Resultat resultat = cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(resultat.etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
        assertThat(memoire.auditPublie).isEmpty();
    }

    @Test
    @Tag("FR-023-02")
    void un_echec_d_ecriture_de_l_audit_n_empeche_pas_de_signaler_la_remise_impossible() {
        memoire.outboxRefusee = true;
        memoire.auditRefuse = true;

        assertThat(refus(commande(1L, 2L, "100", null, null)).motif()).isEqualTo(Cause.REMISE_IMPOSSIBLE);
    }

    @Test
    @Tag("FR-003-11")
    void sans_motif_l_audit_porte_un_motif_vide() {
        cas.executer(commande(1L, 2L, "100", null, null));

        assertThat(memoire.outboxAudit.getFirst().detail()).isEqualTo("Montant: 100.00 - Motif:  - Issue: EXECUTE");
    }

    @Test
    @Tag("FR-003-08")
    void la_meme_cle_d_idempotence_rend_le_premier_resultat_sans_deuxieme_debit() {
        Resultat premier = cas.executer(commande(1L, 2L, "100", "loyer", "cle-1"));
        Resultat second = cas.executer(commande(1L, 2L, "100", "loyer", "cle-1"));

        assertThat(second).isEqualTo(premier);
        assertThat(memoire.virements).hasSize(1);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
        assertThat(memoire.outboxAudit).hasSize(2);
    }

    @Test
    @Tag("FR-003-08")
    void une_cle_d_idempotence_qui_gagne_la_course_rend_le_virement_du_gagnant() {
        // Le gagnant enregistre sa clé entre la première lecture et l'écriture de ce virement.
        memoire.cles.put("7/cle-2", 5L);
        memoire.virements.put(5L, new fr.nextbanking.coeurbancaire.domain.Virement(5, 7, 1, 2,
                new BigDecimal("100.00"), "loyer", EtatVirement.EXECUTE, StatutAffichage.SUCCESS,
                MemoireCoeurBancaire.MAINTENANT, null, null));
        MemoireCoeurBancaire course = new MemoireCoeurBancaire() {
            private int lectures;

            @Override
            public java.util.Optional<fr.nextbanking.coeurbancaire.domain.Virement> parCle(long clientId, String cle) {
                return lectures++ == 0 ? java.util.Optional.empty() : super.parCle(clientId, cle);
            }
        };
        course.comptes.putAll(memoire.comptes);
        course.cles.putAll(memoire.cles);
        course.virements.putAll(memoire.virements);
        ExecuterVirementCasUsage concurrent = assembler(course);

        Resultat resultat = concurrent.executer(commande(1L, 2L, "100", "loyer", "cle-2"));

        assertThat(resultat.virementId()).isEqualTo(5);
        assertThat(course.comptes.get(1L).solde()).isEqualByComparingTo("2130.00");
        assertThat(course.outboxAudit).isEmpty();
    }

    @Test
    @Tag("FR-003-15")
    @Tag("FR-027-01")
    void le_controle_de_fraude_a_lieu_apres_les_controles_de_proprietes_et_de_solde_et_avant_tout_debit() {
        assertThat(refus(commande(3L, 2L, "100", null, null)).motif()).isEqualTo(Cause.COMPTE_ETRANGER);
        assertThat(refus(commande(1L, 2L, "9999", null, null)).motif()).isEqualTo(Cause.SOLDE_INSUFFISANT);
        assertThat(memoire.journal).doesNotContain("verrou-client", "historique", "alerte");

        cas.executer(commande(1L, 2L, "100", null, null));

        assertThat(memoire.journal.indexOf("verrou-client")).isGreaterThan(memoire.journal.lastIndexOf("verrouiller") - 1);
        assertThat(memoire.journal.indexOf("historique")).isLessThan(memoire.journal.indexOf("debiter"));
    }

    @Test
    @Tag("FR-003-07")
    @Tag("FR-027-01")
    void un_virement_rejete_n_entre_pas_dans_la_succession_rapide_du_suivant() {
        assertThat(refus(commande(1L, 1L, "100", null, null)).motif()).isEqualTo(Cause.COMPTES_IDENTIQUES);
        assertThat(refus(commande(1L, 2L, "-5", null, null)).motif()).isEqualTo(Cause.MONTANT_INVALIDE);
        assertThat(refus(commande(1L, 2L, "9999", null, null)).motif()).isEqualTo(Cause.SOLDE_INSUFFISANT);

        cas.executer(commande(1L, 2L, "100", null, null));

        // Seul NEW_BENEFICIARY (35) : ni RAPID_SUCCESSION, donc aucune alerte.
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.dernierVirement(7)).isPresent();
    }

    @Test
    @Tag("FR-003-15")
    @Tag("FR-003-25")
    @Tag("FR-027-03")
    void le_parcours_nominal_ne_leve_que_new_beneficiary_sans_alerte_et_le_virement_est_execute() {
        Resultat resultat = cas.executer(commande(1L, 2L, "100", null, null));

        assertThat(resultat.etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
    }

    @Test
    @Tag("FR-003-25")
    @Tag("FR-027-03")
    void a_75_aucune_alerte_et_a_80_ou_plus_une_alerte_open_par_signal_dans_la_transaction_du_virement() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));

        // 5000.50 : LARGE_AMOUNT 40 + NEW_BENEFICIARY 35 = 75.
        Resultat a75 = cas.executer(commande(1L, 2L, "5000.50", null, null));
        assertThat(memoire.alertes).isEmpty();
        assertThat(a75.etat()).isEqualTo(EtatVirement.EXECUTE);

        // Immédiatement après : RAPID_SUCCESSION 55 en plus, 130 au total : suspendu, sans nouveau débit.
        int debitsAvant = java.util.Collections.frequency(memoire.journal, "debiter");
        Resultat a130 = cas.executer(commande(1L, 2L, "5000.50", null, null));

        assertThat(a130.etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(java.util.Collections.frequency(memoire.journal, "debiter")).isEqualTo(debitsAvant);
        assertThat(memoire.alertes).extracting(a -> a.regle().name(), a -> a.score(), a -> a.statut().name(),
                a -> a.virementId()).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("LARGE_AMOUNT", 40, "OPEN", a130.virementId()),
                        org.assertj.core.groups.Tuple.tuple("NEW_BENEFICIARY", 35, "OPEN", a130.virementId()),
                        org.assertj.core.groups.Tuple.tuple("RAPID_SUCCESSION", 55, "OPEN", a130.virementId()));
        assertThat(memoire.alertes.getFirst().description())
                .isEqualTo("Transaction amount 5000.50 exceeds the large threshold of 5000");
    }

    @Test
    @Tag("FR-003-25")
    void les_alertes_sont_annulees_avec_le_virement_quand_la_remise_est_impossible() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));
        memoire.outboxRefusee = true;

        assertThat(refus(commande(1L, 2L, "5000.00", null, null)).motif()).isEqualTo(Cause.REMISE_IMPOSSIBLE);

        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.virements).isEmpty();
    }

    @Test
    @Tag("FR-003-15")
    @Tag("FR-027-02")
    void une_erreur_du_controle_annule_la_transaction_sans_debit_et_refuse_le_virement_pour_erreur_de_controle() {
        memoire.historiqueEnErreur = new IllegalStateException("historique indisponible");

        ErreurVirement erreur = refus(commande(1L, 2L, "100", "loyer", null));

        assertThat(erreur.motif()).isEqualTo(Cause.CONTROLE_EN_ERREUR);
        assertThat(erreur.getCause()).hasCauseInstanceOf(IllegalStateException.class);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2130.00");
        assertThat(memoire.journal).doesNotContain("debiter", "crediter");
    }

    @Test
    @Tag("FR-027-06")
    @Tag("BUG-085")
    void les_operations_du_jour_comptent_debits_et_credits_et_excluent_les_jours_precedents() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        for (int i = 0; i < 100; i++) {
            memoire.operations.add(new fr.nextbanking.coeurbancaire.domain.Operation(1, TypeOperation.DEBIT,
                    BigDecimal.ONE, BigDecimal.ONE, "ancienne", MemoireCoeurBancaire.MAINTENANT.minusSeconds(86400L * 2),
                    1));
        }
        // 3 virements = 6 opérations du jour (débits et crédits), plus 2 : 8 au plus, jamais de signal.
        for (int i = 0; i < 4; i++) {
            memoire.operations.add(new fr.nextbanking.coeurbancaire.domain.Operation(1, TypeOperation.DEBIT,
                    BigDecimal.ONE, BigDecimal.ONE, "du jour", MemoireCoeurBancaire.MAINTENANT, 1));
            memoire.operations.add(new fr.nextbanking.coeurbancaire.domain.Operation(2, TypeOperation.CREDIT,
                    BigDecimal.ONE, BigDecimal.ONE, "du jour", MemoireCoeurBancaire.MAINTENANT, 1));
        }
        memoire.dernierVirement = java.util.Optional.empty();

        cas.executer(commande(1L, 2L, "100", null, null));
        assertThat(memoire.alertes).isEmpty();
        // 8 + 2 opérations du virement précédent = 10 > 8 : HIGH_FREQUENCY 50 ; avec 5000 € : 40 + 20 + 50 = 110.
        cas.executer(commande(1L, 2L, "5000.00", null, null));

        assertThat(memoire.alertes).extracting(a -> a.regle().name()).containsExactly("LARGE_AMOUNT",
                "HIGH_FREQUENCY", "ROUND_AMOUNT");
    }

    @Test
    @Tag("FR-027-10")
    void la_velocite_compte_les_virements_executes_de_plus_de_500_euros_de_la_derniere_heure() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("20000.00")));
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        memoire.dernierVirement = java.util.Optional.empty();
        memoire.virementsImportants = 5L;

        cas.executer(commande(1L, 2L, "5000.00", null, null));

        // 40 + 20 + 65 = 125.
        assertThat(memoire.alertes).extracting(a -> a.regle().name()).containsExactly("LARGE_AMOUNT",
                "ROUND_AMOUNT", "VELOCITY_CHECK");
    }
}
