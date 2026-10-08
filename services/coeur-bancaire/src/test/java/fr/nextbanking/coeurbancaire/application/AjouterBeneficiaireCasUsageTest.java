package fr.nextbanking.coeurbancaire.application;

import static fr.nextbanking.coeurbancaire.application.BeneficiairesTestes.IBAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire;
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

/** Ajout d'un bénéficiaire : validation avant écriture, normalisation, date de l'horloge, journal sans IBAN en clair. */
class AjouterBeneficiaireCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final AjouterBeneficiaireCasUsage cas = new AjouterBeneficiaireCasUsage(memoire, memoire);
    private final Logger logger = Logger.getLogger(AjouterBeneficiaireCasUsage.class.getName());
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
    }

    @AfterEach
    void nettoyer() {
        logger.removeHandler(capteur);
    }

    @Test
    @Tag("FR-013-07")
    @Tag("FR-013-08")
    void l_ajout_nominal_enregistre_la_saisie_normalisee_actif_avec_la_date_de_l_horloge() {
        memoire.maintenant = Instant.parse("2026-11-17T23:30:00Z");

        BeneficiaireExterne cree = cas.ajouter(7, "[retro] test beneficiaire", "Retro Test SA",
                " fr35 3000 4000 0001 2345 6789 010 ", " hsbcfrppxxx ", "   ");

        assertThat(cree.iban().enClair()).isEqualTo(IBAN);
        assertThat(cree.bic()).isEqualTo("HSBCFRPPXXX");
        assertThat(cree.banque()).isNull();
        assertThat(cree.clientId()).isEqualTo(7);
        assertThat(memoire.beneficiairesExternes).containsValue(cree);
        assertThat(memoire.ajouts).hasSize(1);
        assertThat(memoire.ajouts.getFirst().zoneSepa()).isTrue();
        // 18/11/2026 00:00 Europe/Paris : le jour de l'horloge, pas l'instant.
        assertThat(memoire.ajouts.getFirst().creeLe()).isEqualTo(Instant.parse("2026-11-17T23:00:00Z"));
    }

    @Test
    @Tag("FR-013-05")
    @Tag("FR-013-06")
    void une_saisie_refusee_n_ecrit_rien() {
        assertThatThrownBy(() -> cas.ajouter(7, "", "t", IBAN, null, null))
                .isInstanceOf(ErreurSaisieBeneficiaire.class);
        assertThatThrownBy(() -> cas.ajouter(7, "a", "t", "FR3530004000000123456789011", null, null))
                .isInstanceOf(ErreurSaisieBeneficiaire.class);
        assertThatThrownBy(() -> cas.ajouter(7, "x".repeat(101), "t", IBAN, null, null))
                .isInstanceOf(ErreurSaisieBeneficiaire.class);

        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.beneficiairesExternes).isEmpty();
        assertThat(lignes).isEmpty();
    }

    @Test
    @Tag("FR-013-08")
    void une_panne_d_enregistrement_remonte_sans_rien_enregistrer() {
        memoire.ajoutEnErreur = true;

        assertThatThrownBy(() -> cas.ajouter(7, "a", "t", IBAN, null, null))
                .isInstanceOf(IllegalStateException.class);

        assertThat(memoire.beneficiairesExternes).isEmpty();
        assertThat(lignes).isEmpty();
    }

    @Test
    @Tag("FR-013-07")
    @Tag("BUG-086")
    void le_journal_ne_contient_jamais_l_iban_en_clair() {
        cas.ajouter(7, "a", "t", IBAN, null, null);

        assertThat(lignes).hasSize(1);
        String journal = lignes.getFirst().getMessage();
        assertThat(journal).contains("FR35****9010").doesNotContain(IBAN).doesNotContain("0123456789");
    }
}
