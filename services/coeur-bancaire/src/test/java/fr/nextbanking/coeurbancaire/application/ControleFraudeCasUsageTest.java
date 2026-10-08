package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ControleEnErreur;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contrôle de fraude de l'application : lecture de l'historique par le seul port, verrou du client, journalisation
 * avec l'IBAN masqué (BUG-086). Un test échoue dès que l'IBAN en clair figure dans une ligne du journal.
 */
class ControleFraudeCasUsageTest {

    private static final String IBAN = "FR7630006000011234567890189";

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final Logger logger = Logger.getLogger(ControleFraudeCasUsage.class.getName());
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
    private ControleFraudeCasUsage controle;

    @BeforeEach
    void preparer() {
        logger.addHandler(capteur);
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2130.00")));
        controle = new ControleFraudeCasUsage(memoire, memoire, memoire, memoire, ControleFraudeTestes.PARAMETRES);
    }

    @AfterEach
    void nettoyer() {
        logger.removeHandler(capteur);
    }

    private String journal() {
        StringBuilder texte = new StringBuilder();
        for (LogRecord ligne : lignes) {
            texte.append(ligne.getMessage());
            if (ligne.getThrown() != null) {
                texte.append(ligne.getThrown());
            }
        }
        return texte.toString();
    }

    @Test
    @Tag("FR-027-15")
    @Tag("BUG-086")
    void aucune_ligne_du_journal_ne_contient_l_iban_en_clair_seule_la_forme_masquee_apparait() {
        Evaluation evaluation = controle.evaluer(7, new BigDecimal("100.00"), IBAN, MemoireCoeurBancaire.MAINTENANT);

        assertThat(lignes).isNotEmpty();
        assertThat(journal()).contains("FR76****0189").doesNotContain(IBAN).doesNotContain("1234567890");
        // La description stockée dans l'alerte garde le numéro (comme le legacy) ; seul le journal masque.
        assertThat(evaluation.signaux().getFirst().description()).contains(IBAN);
    }

    @Test
    @Tag("FR-027-15")
    @Tag("BUG-086")
    void une_erreur_du_controle_est_journalisee_sans_l_iban_en_clair_puis_relancee() {
        memoire.historiqueEnErreur = new IllegalStateException("historique indisponible");

        assertThatThrownBy(() -> controle.evaluer(7, new BigDecimal("100.00"), IBAN,
                MemoireCoeurBancaire.MAINTENANT)).isInstanceOf(ControleEnErreur.class)
                .hasRootCauseMessage("historique indisponible");

        assertThat(lignes).extracting(LogRecord::getLevel).containsExactly(Level.SEVERE);
        assertThat(lignes.getFirst().getThrown()).hasMessage("historique indisponible");
        assertThat(journal()).contains("FR76****0189").doesNotContain(IBAN);
    }

    @Test
    @Tag("FR-003-15")
    @Tag("FR-027-01")
    void le_verrou_du_client_est_pris_avant_la_lecture_de_l_historique() {
        controle.evaluer(7, new BigDecimal("100.00"), "FR76-0001-0002", MemoireCoeurBancaire.MAINTENANT);

        assertThat(memoire.journal).containsExactly("verrou-client", "historique");
    }

    @Test
    @Tag("FR-003-25")
    @Tag("FR-027-03")
    void sous_le_seuil_aucune_alerte_est_ecrite_et_au_dessus_chaque_signal_est_ecrit() {
        Evaluation faible = controle.evaluer(7, new BigDecimal("100.00"), "FR76-0001-0002",
                MemoireCoeurBancaire.MAINTENANT);
        controle.enregistrerAlertes(faible, 7, 12, MemoireCoeurBancaire.MAINTENANT);
        assertThat(memoire.alertes).isEmpty();

        Evaluation forte = controle.evaluer(7, new BigDecimal("5000.00"), "DE89370400440532013000",
                MemoireCoeurBancaire.MAINTENANT);
        controle.enregistrerAlertes(forte, 7, 12, MemoireCoeurBancaire.MAINTENANT);

        assertThat(memoire.alertes).hasSize(forte.signaux().size()).hasSizeGreaterThan(1);
    }

    @Test
    @Tag("FR-027-06")
    @Tag("BUG-010")
    void le_debut_du_jour_est_celui_du_fuseau_de_l_horloge_et_la_fenetre_de_velocite_remonte_de_60_minutes() {
        List<Instant> debuts = new ArrayList<>();
        List<Instant> fenetres = new ArrayList<>();
        MemoireCoeurBancaire espion = new MemoireCoeurBancaire() {
            @Override
            public long operationsDepuis(long clientId, Instant debut) {
                debuts.add(debut);
                return 0;
            }

            @Override
            public Optional<Instant> dernierVirement(long clientId) {
                return Optional.empty();
            }

            @Override
            public long virementsExecutesAuDessus(long clientId, BigDecimal montantMin, Instant depuis) {
                assertThat(montantMin).isEqualByComparingTo("500");
                fenetres.add(depuis);
                return 0;
            }
        };
        new ControleFraudeCasUsage(espion, espion, espion, espion, ControleFraudeTestes.PARAMETRES)
                .evaluer(7, new BigDecimal("100.00"), "FR76-0001-0002", MemoireCoeurBancaire.MAINTENANT);

        // Le 17/11/2026 à 00:00 Europe/Paris = 16/11 à 23:00 UTC.
        assertThat(debuts).containsExactly(Instant.parse("2026-11-16T23:00:00Z"));
        assertThat(fenetres).containsExactly(Instant.parse("2026-11-17T08:00:00Z"));
    }
}
