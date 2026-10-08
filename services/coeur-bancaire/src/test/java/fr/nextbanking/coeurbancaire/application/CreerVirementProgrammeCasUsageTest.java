package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Création d'un virement programmé : ordre des contrôles, destinataire par numéro, horloge, journal. */
class CreerVirementProgrammeCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final CreerVirementProgrammeCasUsage cas = new CreerVirementProgrammeCasUsage(memoire, memoire, memoire);
    private final Logger logger = Logger.getLogger(CreerVirementProgrammeCasUsage.class.getName());
    private final List<LogRecord> lignes = new ArrayList<>();
    private final Handler capteur = new Handler() {
        @Override
        public void publish(LogRecord ligne) {
            lignes.add(ligne);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("100.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("10.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        logger.addHandler(capteur);
    }

    @AfterEach
    void nettoyer() {
        logger.removeHandler(capteur);
    }

    private LigneVirementProgramme creerNominal() {
        return cas.creer(7, 1L, "FR76-0001-0002", "[retro] test virprog", "50", "MENSUEL",
                "[retro] virement programm gm", "2027-01-01");
    }

    private void refus(Cause cause, Long source, String numero, String montant, String date) {
        assertThatThrownBy(() -> cas.creer(7, source, numero, "libelle", montant, "MENSUEL", null, date))
                .isInstanceOfSatisfying(ErreurVirementProgramme.class, e -> assertThat(e.motif()).isEqualTo(cause));
        assertThat(memoire.virementsProgrammes).isEmpty();
    }

    @Test
    @Tag("FR-004-07")
    void la_creation_nominale_enregistre_un_virement_actif_a_zero_execution() {
        LigneVirementProgramme ligne = creerNominal();

        assertThat(memoire.virementsProgrammes).hasSize(1);
        VirementProgramme enregistre = memoire.virementsProgrammes.getFirst();
        assertThat(enregistre.clientId()).isEqualTo(7);
        assertThat(enregistre.compteSourceId()).isEqualTo(1);
        assertThat(enregistre.actif()).isTrue();
        assertThat(enregistre.nbExecutions()).isZero();
        assertThat(enregistre.montant().texte()).isEqualTo("50.00");
        assertThat(enregistre.frequence()).isEqualTo(FrequenceVirement.MENSUEL);
        assertThat(enregistre.creeLe()).isEqualTo(MemoireCoeurBancaire.MAINTENANT);
        // 01/01/2027 à 00:00 Europe/Paris (UTC+1).
        assertThat(enregistre.prochaineExecution()).isEqualTo(Instant.parse("2026-12-31T23:00:00Z"));
        assertThat(ligne.numeroCompteSource()).isEqualTo("FR76-0001-0001");
        assertThat(ligne.prochaineExecution()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(ligne.motif()).isEqualTo("[retro] virement programm gm");
    }

    @Test
    @Tag("FR-004-07")
    void une_date_passee_est_acceptee_et_devient_la_prochaine_execution() {
        LigneVirementProgramme ligne = cas.creer(7, 1L, "FR76-0001-0002", "l", "5", "ANNUEL", null, "2020-01-01");

        assertThat(ligne.prochaineExecution()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(memoire.virementsProgrammes.getFirst().prochaineExecution())
                .isEqualTo(Instant.parse("2019-12-31T23:00:00Z"));
    }

    @Test
    @Tag("FR-004-04")
    @Tag("FR-004-05")
    void une_saisie_invalide_n_ecrit_rien_et_n_interroge_pas_les_comptes() {
        refus(Cause.COMPTE_SOURCE_OBLIGATOIRE, null, "FR76-0001-0002", "5", "2027-01-01");
        refus(Cause.NUMERO_OBLIGATOIRE, 1L, " ", "5", "2027-01-01");
        refus(Cause.MONTANT_INVALIDE, 1L, "FR76-0001-0002", "-5", "2027-01-01");
        refus(Cause.DATE_OBLIGATOIRE, 1L, "FR76-0001-0002", "5", "");
        refus(Cause.DATE_INVALIDE, 1L, "FR76-0001-0002", "5", "01/01/2027");
    }

    @Test
    @Tag("FR-004-06")
    void le_compte_source_inexistant_ou_d_autrui_est_refuse() {
        refus(Cause.COMPTE_SOURCE_INTROUVABLE, 99L, "FR76-0001-0002", "5", "2027-01-01");
        refus(Cause.COMPTE_SOURCE_ETRANGER, 3L, "FR76-0001-0002", "5", "2027-01-01");
    }

    @Test
    @Tag("FR-004-13")
    @Tag("BUG-015")
    void un_destinataire_inconnu_est_refuse_apres_les_controles_du_compte_source() {
        refus(Cause.DESTINATAIRE_INTROUVABLE, 1L, "FR76-9999-9999", "5", "2027-01-01");
        // Le compte source est contrôlé avant le destinataire.
        refus(Cause.COMPTE_SOURCE_ETRANGER, 3L, "FR76-9999-9999", "5", "2027-01-01");
    }

    @Test
    @Tag("FR-004-07")
    void une_panne_d_enregistrement_remonte_sans_rien_enregistrer() {
        memoire.enregistrementProgrammeEnErreur = true;

        assertThatThrownBy(this::creerNominal).isInstanceOf(IllegalStateException.class);

        assertThat(memoire.virementsProgrammes).isEmpty();
        assertThat(lignes).isEmpty();
    }

    @Test
    @Tag("FR-004-07")
    void le_journal_ne_contient_ni_numero_ni_libelle_du_beneficiaire() {
        creerNominal();

        assertThat(lignes).hasSize(1);
        assertThat(lignes.getFirst().getMessage()).doesNotContain("FR76-0001-0002").doesNotContain("retro");
    }
}
