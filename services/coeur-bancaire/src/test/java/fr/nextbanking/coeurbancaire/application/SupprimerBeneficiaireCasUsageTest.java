package fr.nextbanking.coeurbancaire.application;

import static fr.nextbanking.coeurbancaire.application.BeneficiairesTestes.IBAN;
import static fr.nextbanking.coeurbancaire.application.BeneficiairesTestes.beneficiaire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire.Cause;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Suppression d'un bénéficiaire : propriété décidée par le cas d'usage (BUG-047), transition unique, journal sans
 * IBAN en clair.
 */
class SupprimerBeneficiaireCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final SupprimerBeneficiaireCasUsage cas = new SupprimerBeneficiaireCasUsage(memoire);
    private final Logger logger = Logger.getLogger(SupprimerBeneficiaireCasUsage.class.getName());
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
        memoire.beneficiairesExternes.put(1L, beneficiaire(1, 7, "Loyer"));
    }

    @AfterEach
    void nettoyer() {
        logger.removeHandler(capteur);
    }

    private String journal() {
        StringBuilder texte = new StringBuilder();
        lignes.forEach(ligne -> texte.append(ligne.getMessage()));
        return texte.toString();
    }

    @Test
    @Tag("FR-013-09")
    void le_proprietaire_desactive_son_beneficiaire() {
        cas.supprimer(7, 1);

        assertThat(memoire.beneficiairesExternes).isEmpty();
        assertThat(memoire.journal).containsExactly("desactiver 1");
    }

    @Test
    @Tag("FR-013-10")
    @Tag("BUG-047")
    void le_beneficiaire_d_un_autre_client_est_refuse_et_reste_actif() {
        assertThatThrownBy(() -> cas.supprimer(8, 1)).isInstanceOfSatisfying(ErreurBeneficiaire.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.ETRANGER));

        assertThat(memoire.beneficiairesExternes).containsKey(1L);
        assertThat(memoire.journal).isEmpty();
    }

    @Test
    @Tag("FR-013-10")
    void un_beneficiaire_inexistant_ou_deja_desactive_est_introuvable() {
        assertThatThrownBy(() -> cas.supprimer(7, 99)).isInstanceOfSatisfying(ErreurBeneficiaire.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));

        cas.supprimer(7, 1);
        assertThatThrownBy(() -> cas.supprimer(7, 1)).isInstanceOfSatisfying(ErreurBeneficiaire.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));
    }

    @Test
    @Tag("FR-013-09")
    void la_perdante_d_une_course_est_introuvable() {
        memoire.desactivationsPerdues.add(1L);

        assertThatThrownBy(() -> cas.supprimer(7, 1)).isInstanceOfSatisfying(ErreurBeneficiaire.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));
    }

    @Test
    @Tag("FR-013-09")
    @Tag("BUG-086")
    void le_journal_ne_contient_jamais_l_iban_en_clair() {
        cas.supprimer(7, 1);
        assertThatThrownBy(() -> {
            memoire.beneficiairesExternes.put(2L, beneficiaire(2, 7, "Autre"));
            cas.supprimer(8, 2);
        }).isInstanceOf(ErreurBeneficiaire.class);

        assertThat(lignes).hasSize(2);
        assertThat(journal()).contains("FR35****9010").doesNotContain(IBAN).doesNotContain("0123456789");
    }
}
