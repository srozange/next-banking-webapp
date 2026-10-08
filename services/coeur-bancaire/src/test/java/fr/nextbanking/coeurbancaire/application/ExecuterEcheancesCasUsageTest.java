package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.TypeNotification;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Exécution des échéances dues (FEAT-021) sur un cœur bancaire en mémoire : sélection, une échéance par passage,
 * date suivante sans dérive, fraude, isolement des échecs, concurrence, audit, journal d'exécution.
 */
@Tag("FR-021-01")
@Tag("FR-021-02")
@Tag("FR-021-03")
@Tag("FR-021-05")
@Tag("FR-021-06")
@Tag("FR-021-07")
@Tag("FR-021-08")
@Tag("FR-021-10")
@Tag("FR-004-07")
@Tag("FR-004-12")
@Tag("FR-004-13")
@Tag("FR-004-15")
@Tag("FR-021-04")
@Tag("FR-021-09")
@Tag("BUG-014")
@Tag("BUG-015")
@Tag("BUG-068")
@Tag("BUG-070")
@Tag("FR-004-11")
@Tag("FR-004-14")
@Tag("BUG-069")
class ExecuterEcheancesCasUsageTest {

    private static final Instant DIX_HEURES = Instant.parse("2026-11-17T09:00:00Z");
    private static final Instant NEUF_55 = Instant.parse("2026-11-17T08:55:00Z");
    private static final Instant HUIT_HEURES = Instant.parse("2026-11-17T07:00:00Z");
    private static final Instant SEPT_HEURES = Instant.parse("2026-11-17T06:00:00Z");

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final Collecteur collecteur = new Collecteur();
    private ExecuterEcheancesCasUsage cas;

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2130.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("15200.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("500.00")));
        memoire.maintenant = DIX_HEURES;
        memoire.beneficiairesActifs.put(7L, java.util.Set.of("FR76-0001-0002"));
        ControleFraudeCasUsage controle = new ControleFraudeCasUsage(memoire, memoire, memoire, memoire,
                ControleFraudeTestes.PARAMETRES);
        cas = new ExecuterEcheancesCasUsage(memoire, memoire, memoire, memoire, memoire, memoire, memoire, memoire, memoire,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000")), controle);
        Logger.getLogger(ExecuterEcheancesCasUsage.class.getName()).addHandler(collecteur);
    }

    @AfterEach
    void detacher() {
        Logger.getLogger(ExecuterEcheancesCasUsage.class.getName()).removeHandler(collecteur);
    }

    private VirementProgramme programme(long id, long client, long source, String numero, String montant,
            FrequenceVirement frequence, String motif, Instant prochaine, boolean actif) {
        VirementProgramme v = new VirementProgramme(id, client, source, numero, "Epargne", Montant.lire(montant),
                frequence, motif, prochaine, actif, 0, Instant.parse("2026-01-01T00:00:00Z"));
        memoire.virementsProgrammes.add(v);
        return v;
    }

    private VirementProgramme mensuel(long id, Instant prochaine) {
        return programme(id, 7, 1, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, "Epargne mensuelle",
                prochaine, true);
    }

    private VirementProgramme lire(long id) {
        return memoire.virementsProgrammes.stream().filter(v -> v.id() == id).findFirst().orElseThrow();
    }

    private void rien() {
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.virements).isEmpty();
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("2130.00");
        assertThat(memoire.comptes.get(2L).solde().texte()).isEqualTo("15200.00");
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void seuls_les_actifs_dus_sont_executes_dans_l_ordre_des_echeances() {
        mensuel(1, NEUF_55);
        mensuel(2, HUIT_HEURES);
        programme(3, 7, 1, "FR76-0001-0002", "999.00", FrequenceVirement.MENSUEL, null, SEPT_HEURES, false);
        mensuel(4, DIX_HEURES.plusSeconds(1));

        assertThat(cas.executerLesDues()).isEqualTo(2);

        assertThat(memoire.virements.keySet()).containsExactly(12L, 13L);
        assertThat(memoire.operations).extracting(o -> o.compteId()).containsExactly(1L, 2L, 1L, 2L);
        assertThat(lire(3).nbExecutions()).isZero();
        assertThat(lire(3).prochaineExecution()).isEqualTo(SEPT_HEURES);
        assertThat(lire(4).nbExecutions()).isZero();
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("1930.00");
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|2|SUCCESS|Verification virements programmes dus");
        assertThat(collecteur.messages).contains("Verification virements programmes dus");
    }

    @Test
    void une_echeance_due_debite_credite_ecrit_deux_operations_et_avance_sans_notification() {
        Instant prevue = Instant.parse("2026-11-17T07:00:00Z");
        mensuel(1, prevue);
        memoire.maintenant = Instant.parse("2026-11-17T07:04:00Z");

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("2030.00");
        assertThat(memoire.comptes.get(2L).solde().texte()).isEqualTo("15300.00");
        assertThat(memoire.operations).extracting(o -> o.type(), o -> o.description(), o -> o.date())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.DEBIT,
                                "Virement programme vers Epargne - Epargne mensuelle", memoire.maintenant),
                        org.assertj.core.groups.Tuple.tuple(TypeOperation.CREDIT,
                                "Virement programme de FR76-0001-0001 - Epargne mensuelle", memoire.maintenant));
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(memoire.cles).isEmpty();
        assertThat(lire(1).nbExecutions()).isEqualTo(1);
        assertThat(lire(1).prochaineExecution()).isEqualTo(Instant.parse("2026-12-17T07:00:00Z"));
        assertThat(memoire.derniereExecution.get(1L)).isEqualTo(memoire.maintenant);
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.outboxAudit).extracting(e -> e.action(), e -> e.utilisateur(), e -> e.adresseIp(),
                e -> e.detail()).containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ActionAudit.VIREMENT, "7", null,
                                "Montant: 100.00 - Motif: Epargne mensuelle - Issue: EXECUTE"),
                        org.assertj.core.groups.Tuple.tuple(ActionAudit.TRANSFER_EXECUTED, "7", null,
                                "Montant: 100.00 - Motif: Epargne mensuelle"));
        assertThat(collecteur.messages).contains("Virement programme execute id=1 montant=100.00");
    }

    @Test
    void l_echeance_suivante_part_de_l_echeance_prevue_pas_de_l_heure_reelle() {
        mensuel(1, Instant.parse("2026-01-15T07:00:00Z"));
        memoire.maintenant = Instant.parse("2026-01-15T07:04:00Z");

        cas.executerLesDues();

        assertThat(lire(1).prochaineExecution()).isEqualTo(Instant.parse("2026-02-15T07:00:00Z"));
    }

    @Test
    void trois_periodes_de_retard_ne_donnent_qu_une_execution_par_passage() {
        mensuel(1, Instant.parse("2026-08-17T07:00:00Z"));

        assertThat(cas.executerLesDues()).isEqualTo(1);
        assertThat(lire(1).prochaineExecution()).isEqualTo(Instant.parse("2026-09-17T07:00:00Z"));
        assertThat(cas.executerLesDues()).isEqualTo(1);
        assertThat(lire(1).prochaineExecution()).isEqualTo(Instant.parse("2026-10-17T07:00:00Z"));
        assertThat(lire(1).nbExecutions()).isEqualTo(2);
    }

    @Test
    void sans_motif_les_libelles_n_ont_pas_de_suffixe() {
        programme(1, 7, 1, "FR76-0001-0002", "100.00", FrequenceVirement.HEBDOMADAIRE, null, HUIT_HEURES, true);

        cas.executerLesDues();

        assertThat(memoire.operations).extracting(o -> o.description()).containsExactly(
                "Virement programme vers Epargne", "Virement programme de FR76-0001-0001");
        assertThat(memoire.outboxAudit.getFirst().detail()).isEqualTo("Montant: 100.00 - Motif:  - Issue: EXECUTE");
    }

    @Test
    void un_montant_en_attente_ne_porte_jamais_la_mention_en_attente() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("9000.00")));
        programme(1, 7, 1, "FR76-0001-0002", "5000.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.operations).extracting(o -> o.description()).noneMatch(d -> d.contains("[EN ATTENTE]"));
    }

    @Test
    void un_solde_insuffisant_ne_change_rien_reste_du_et_notifie_une_seule_fois() {
        programme(1, 7, 1, "FR76-0001-0002", "500.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("499.99")));

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.virements).isEmpty();
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("499.99");
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(lire(1).nbExecutions()).isZero();
        assertThat(lire(1).actif()).isTrue();
        assertThat(memoire.derniereExecution).isEmpty();
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.journal).doesNotContain("historique", "debiter");
        assertThat(collecteur.messages).contains("Solde insuffisant pour virement programme id=1");
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|SUCCESS|Verification virements programmes dus");
        assertThat(memoire.echeancesNonExecutees).hasSize(1);
        var evenement = memoire.echeancesNonExecutees.getFirst();
        assertThat(evenement.virementProgrammeId()).isEqualTo(1L);
        assertThat(evenement.horodatage()).isEqualTo(DIX_HEURES);
        assertThat(evenement.notification().clientId()).isEqualTo(7L);
        assertThat(evenement.notification().type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(evenement.notification().titre()).isEqualTo("Virement programmé non exécuté");
        assertThat(evenement.notification().message()).isEqualTo(
                "Virement programmé vers Epargne non exécuté : solde insuffisant, rien n'a été débité");
        assertThat(memoire.echeancesPubliees).containsExactly(401L);

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.echeancesNonExecutees).hasSize(1);
        assertThat(memoire.echeancesPubliees).hasSize(1);
        assertThat(collecteur.messages).filteredOn("Solde insuffisant pour virement programme id=1"::equals)
                .hasSize(2);
    }

    @Test
    void quand_le_solde_est_retabli_l_echeance_est_executee_sans_notification() {
        programme(1, 7, 1, "FR76-0001-0002", "500.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("499.99")));
        cas.executerLesDues();

        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("600.00")));
        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.echeancesNonExecutees).hasSize(1);
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("100.00");
        assertThat(lire(1).nbExecutions()).isEqualTo(1);
    }

    @Test
    void une_ecriture_de_notification_en_erreur_est_comptee_en_echec_et_l_echeance_reste_due() {
        programme(1, 7, 1, "FR76-0001-0002", "5000.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        memoire.marqueEnErreur = true;

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.echeancesNonExecutees).isEmpty();
        assertThat(memoire.echeancesNotifiees).isEmpty();
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|FAILED|Echec execution virement programme id=1");

        memoire.marqueEnErreur = false;
        cas.executerLesDues();
        assertThat(memoire.echeancesNonExecutees).hasSize(1);
    }

    @Test
    void un_debit_refuse_a_l_ecriture_annule_toute_l_echeance() {
        mensuel(1, HUIT_HEURES);
        memoire.debitRefuse = true;

        assertThat(cas.executerLesDues()).isZero();

        rien();
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(lire(1).nbExecutions()).isZero();
        assertThat(memoire.alertes).isEmpty();
    }

    @Test
    void un_destinataire_introuvable_est_refuse_avant_le_solde_desactive_et_notifie() {
        programme(1, 7, 1, "FR76-9999-9999", "5000.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.virements).isEmpty();
        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("10.00");
        assertThat(lire(1).actif()).isFalse();
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(lire(1).nbExecutions()).isZero();
        assertThat(memoire.journal).doesNotContain("verrouiller", "prendre", "debiter");
        assertThat(collecteur.messages).contains("Echec execution virement programme id=1");
        assertThat(collecteur.niveaux).contains(Level.WARNING);
        assertThat(memoire.echeancesNonExecutees).hasSize(1);
        var notification = memoire.echeancesNonExecutees.getFirst().notification();
        assertThat(notification.type()).isEqualTo(TypeNotification.ALERTE);
        assertThat(notification.titre()).isEqualTo("Virement programmé refusé");
        assertThat(notification.message()).isEqualTo(
                "Virement programmé vers Epargne refusé : compte destinataire introuvable, rien n'a été débité");
        assertThat(memoire.echeancesPubliees).hasSize(1);
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|SUCCESS|Verification virements programmes dus");

        assertThat(cas.executerLesDues()).isZero();
        assertThat(memoire.echeancesNonExecutees).hasSize(1);
    }

    @Test
    void un_refus_dont_la_desactivation_est_perdue_n_a_aucun_effet() {
        programme(1, 7, 1, "FR76-9999-9999", "100.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        memoire.desactivationsEcheancePerdues.add(1L);

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.echeancesNonExecutees).isEmpty();
        assertThat(memoire.echeancesNotifiees).isEmpty();
        assertThat(memoire.echeancesPubliees).isEmpty();
    }

    @Test
    void une_echeance_suspendue_par_la_fraude_est_annulee_et_reste_due() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("50000.00")));
        programme(1, 7, 1, "FR76-0001-0002", "12000.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.comptes.get(1L).solde().texte()).isEqualTo("50000.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.virements).isEmpty();
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(lire(1).nbExecutions()).isZero();
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|SUCCESS|Verification virements programmes dus");
    }

    @Test
    void un_controle_en_erreur_annule_l_echeance_la_compte_en_echec_et_la_retente() {
        mensuel(1, HUIT_HEURES);
        memoire.historiqueEnErreur = new IllegalStateException("historique indisponible");

        assertThat(cas.executerLesDues()).isZero();

        rien();
        assertThat(lire(1).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|FAILED|Echec execution virement programme id=1");

        memoire.historiqueEnErreur = null;
        assertThat(cas.executerLesDues()).isEqualTo(1);
    }

    @Test
    void un_echec_isole_laisse_les_autres_executees_et_la_fautive_intacte() {
        mensuel(1, SEPT_HEURES);
        programme(2, 7, 3, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);
        mensuel(3, NEUF_55);

        assertThat(cas.executerLesDues()).isEqualTo(2);

        assertThat(lire(1).nbExecutions()).isEqualTo(1);
        assertThat(lire(2).nbExecutions()).isZero();
        assertThat(lire(2).prochaineExecution()).isEqualTo(HUIT_HEURES);
        assertThat(lire(3).nbExecutions()).isEqualTo(1);
        assertThat(memoire.comptes.get(3L).solde().texte()).isEqualTo("500.00");
        assertThat(collecteur.messages).contains("Echec execution virement programme id=2");
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|2|PARTIAL|Echec execution virement programme id=2");
    }

    @Test
    void plusieurs_echecs_sont_listes_separes_par_un_point_virgule() {
        programme(1, 7, 3, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, null, SEPT_HEURES, true);
        programme(2, 7, 3, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, null, HUIT_HEURES, true);

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.lignesBatch).containsExactly("RECURRING_TRANSFERS|0|FAILED|"
                + "Echec execution virement programme id=1; Echec execution virement programme id=2");
    }

    @Test
    void un_compte_source_disparu_ou_un_motif_trop_long_est_un_echec_isole() {
        programme(1, 7, 99, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, null, SEPT_HEURES, true);
        programme(2, 7, 1, "FR76-0001-0002", "100.00", FrequenceVirement.MENSUEL, "x".repeat(101), HUIT_HEURES, true);
        mensuel(3, NEUF_55);

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(lire(1).nbExecutions()).isZero();
        assertThat(lire(2).nbExecutions()).isZero();
        assertThat(lire(3).nbExecutions()).isEqualTo(1);
        assertThat(memoire.lignesBatch.getFirst()).startsWith("RECURRING_TRANSFERS|1|PARTIAL|");
    }

    @Test
    void une_prise_perdue_est_sans_effet() {
        mensuel(1, HUIT_HEURES);
        memoire.prisesPerdues.add(1L);

        assertThat(cas.executerLesDues()).isZero();

        rien();
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|SUCCESS|Verification virements programmes dus");
    }

    @Test
    void une_lecture_en_erreur_ecrit_une_ligne_failed() {
        memoire.lectureDuesEnErreur = true;

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.lignesBatch).containsExactly(
                "RECURRING_TRANSFERS|0|FAILED|Erreur: lecture des échéances en erreur");
    }

    @Test
    void une_ecriture_de_journal_en_erreur_n_est_jamais_relancee() {
        mensuel(1, HUIT_HEURES);
        memoire.batchRefuse = true;

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.lignesBatch).isEmpty();
    }

    @Test
    void un_audit_en_erreur_ne_defait_pas_l_echeance() {
        mensuel(1, HUIT_HEURES);
        memoire.auditRefuse = true;

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.operations).hasSize(2);
        assertThat(List.copyOf(memoire.outboxAudit)).isEmpty();
    }

    /** Collecte les messages et niveaux du journal du cas d'usage. */
    private static final class Collecteur extends Handler {
        final List<String> messages = new java.util.ArrayList<>();
        final List<Level> niveaux = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord enregistrement) {
            messages.add(enregistrement.getMessage());
            niveaux.add(enregistrement.getLevel());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
