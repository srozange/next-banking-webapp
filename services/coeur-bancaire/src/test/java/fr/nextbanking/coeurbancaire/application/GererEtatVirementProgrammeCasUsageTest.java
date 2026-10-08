package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Suspension, réactivation et suppression d'un virement programmé : refus unique, propriété (BUG-013), journal. */
@Tag("FR-004-09")
@Tag("FR-004-10")
class GererEtatVirementProgrammeCasUsageTest {

    private static final String NUMERO = "FR76-0001-0002";

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final GererEtatVirementProgrammeCasUsage cas = new GererEtatVirementProgrammeCasUsage(memoire);
    private final Logger logger = Logger.getLogger(GererEtatVirementProgrammeCasUsage.class.getName());
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
        logger.addHandler(capteur);
        memoire.enregistrer(7, DemandeVirementProgramme.valider(1L, NUMERO, "Loyer", "50", "MENSUEL", null,
                "2027-01-01"), Instant.parse("2026-12-31T23:00:00Z"), Instant.parse("2026-11-17T09:00:00Z"));
    }

    @AfterEach
    void nettoyer() {
        logger.removeHandler(capteur);
    }

    private VirementProgramme virement() {
        return memoire.virementsProgrammes.getFirst();
    }

    private static void introuvable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ErreurVirementProgramme.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));
    }

    @Test
    void suspendre_puis_reactiver_ne_change_que_l_etat() {
        VirementProgramme avant = virement();

        cas.suspendre(7, 1);
        assertThat(virement().actif()).isFalse();
        assertThat(virement().prochaineExecution()).isEqualTo(avant.prochaineExecution());
        assertThat(virement().nbExecutions()).isEqualTo(avant.nbExecutions());

        cas.reactiver(7, 1);
        assertThat(virement()).isEqualTo(avant);
    }

    @Test
    void supprimer_retire_le_virement_actif_ou_inactif() {
        cas.supprimer(7, 1);
        assertThat(memoire.virementsProgrammes).isEmpty();

        memoire.enregistrer(7, DemandeVirementProgramme.valider(1L, NUMERO, "Loyer", "50", "MENSUEL", null,
                "2027-01-01"), Instant.parse("2026-12-31T23:00:00Z"), Instant.parse("2026-11-17T09:00:00Z"));
        cas.suspendre(7, 1);
        cas.supprimer(7, 1);
        assertThat(memoire.virementsProgrammes).isEmpty();
    }

    @Test
    @Tag("BUG-013")
    void le_virement_d_autrui_ou_inexistant_est_refuse_et_reste_intact() {
        VirementProgramme avant = virement();

        introuvable(() -> cas.suspendre(8, 1));
        introuvable(() -> cas.reactiver(8, 1));
        introuvable(() -> cas.supprimer(8, 1));
        introuvable(() -> cas.suspendre(7, 99));
        introuvable(() -> cas.reactiver(7, 99));
        introuvable(() -> cas.supprimer(7, 99));

        assertThat(memoire.virementsProgrammes).containsExactly(avant);
    }

    @Test
    void une_transition_deja_atteinte_est_refusee() {
        introuvable(() -> cas.reactiver(7, 1));
        cas.suspendre(7, 1);
        introuvable(() -> cas.suspendre(7, 1));
        cas.supprimer(7, 1);
        introuvable(() -> cas.supprimer(7, 1));
    }

    @Test
    void le_journal_ne_porte_que_des_identifiants_numeriques() {
        cas.suspendre(7, 1);
        introuvable(() -> cas.suspendre(7, 1));

        assertThat(lignes).hasSize(2);
        StringBuilder texte = new StringBuilder();
        lignes.forEach(l -> texte.append(l.getMessage()).append(l.getParameters() == null ? "" : "?"));
        assertThat(lignes.get(0).getLevel()).isEqualTo(java.util.logging.Level.INFO);
        assertThat(lignes.get(1).getLevel()).isEqualTo(java.util.logging.Level.WARNING);
        assertThat(texte.toString()).doesNotContain(NUMERO).doesNotContain("Loyer");
    }
}
