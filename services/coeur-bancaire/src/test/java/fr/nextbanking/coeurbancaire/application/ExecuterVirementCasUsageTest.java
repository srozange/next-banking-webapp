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
class ExecuterVirementCasUsageTest {

    private static final String IP = "203.0.113.7";

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private ExecuterVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("2130.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, new BigDecimal("15200.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, new BigDecimal("500.00")));
        cas = new ExecuterVirementCasUsage(memoire, memoire, memoire, memoire, memoire, memoire, memoire,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000")));
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
    }

    @Test
    @Tag("FR-003-09")
    @Tag("FR-003-08")
    void un_virement_valide_debite_credite_et_rend_l_ecrit_des_champs_saisis() {
        Resultat resultat = cas.executer(commande(1L, 2L, "100", "loyer", null));

        assertThat(resultat).isEqualTo(new Resultat(12, EtatVirement.EXECUTE, StatutAffichage.SUCCESS, 1, 2,
                new BigDecimal("100.00"), "loyer"));
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
        assertThat(memoire.comptes.get(2L).solde()).isEqualByComparingTo("15300.00");
        assertThat(memoire.operations).extracting(o -> o.type(), o -> o.description(), o -> o.soldeApres())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.DEBIT,
                                "Virement vers FR76-0001-0002 - loyer", new BigDecimal("2030.00")),
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.CREDIT,
                                "Virement depuis FR76-0001-0001 - loyer", new BigDecimal("15300.00")));
        assertThat(memoire.journal).containsExactly("verrouiller", "debiter", "crediter");
    }

    @Test
    @Tag("FR-003-10")
    void a_5000_euros_le_virement_est_pending_et_a_4999_99_il_est_success() {
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
                MemoireCoeurBancaire.MAINTENANT));
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
        ExecuterVirementCasUsage concurrent = new ExecuterVirementCasUsage(course, course, course, course, course,
                course, course, new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000")));

        Resultat resultat = concurrent.executer(commande(1L, 2L, "100", "loyer", "cle-2"));

        assertThat(resultat.virementId()).isEqualTo(5);
        assertThat(course.comptes.get(1L).solde()).isEqualByComparingTo("2130.00");
        assertThat(course.outboxAudit).isEmpty();
    }
}
