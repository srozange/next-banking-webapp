package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Virements programmés et recherche de compte par numéro sur une vraie base. */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-004-03")
@Tag("FR-004-07")
@Tag("FR-004-13")
@Tag("FR-021-02")
@Tag("FR-021-07")
class VirementsProgrammesJdbcIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private VirementsProgrammes virements;

    @Autowired
    private Comptes comptes;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource source;

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    private VirementProgramme enregistrer(long client, String motif, String date, String prochaine) {
        return virements.enregistrer(client, DemandeVirementProgramme.valider(1L, "FR76-0001-0002", "Epargne", "35",
                "TRIMESTRIEL", motif, date), Instant.parse(prochaine), Instant.parse("2026-11-17T09:00:00Z"));
    }

    @Test
    void l_enregistrement_est_actif_a_zero_execution_et_relu_a_l_identique() {
        VirementProgramme cree = enregistrer(1, "Motif", "2027-01-01", "2026-12-31T23:00:00Z");

        assertThat(cree.actif()).isTrue();
        assertThat(cree.nbExecutions()).isZero();
        assertThat(virements.listerDuClient(1)).containsExactly(cree);
        assertThat(virements.listerDuClient(1).getFirst().montant().texte()).isEqualTo("35.00");
    }

    @Test
    void le_motif_absent_reste_nul() {
        enregistrer(1, null, "2027-01-01", "2026-12-31T23:00:00Z");

        assertThat(virements.listerDuClient(1).getFirst().motif()).isNull();
    }

    @Test
    void la_liste_est_filtree_par_client_et_triee_par_prochaine_execution_puis_identifiant() {
        VirementProgramme tard = enregistrer(1, null, "2027-03-01", "2027-02-28T23:00:00Z");
        VirementProgramme tot = enregistrer(1, null, "2027-01-01", "2026-12-31T23:00:00Z");
        VirementProgramme egal = enregistrer(1, null, "2027-01-01", "2026-12-31T23:00:00Z");
        enregistrer(8, null, "2026-01-01", "2025-12-31T23:00:00Z");

        assertThat(virements.listerDuClient(1)).extracting(VirementProgramme::id)
                .containsExactly(tot.id(), egal.id(), tard.id());
        assertThat(virements.listerDuClient(99)).isEmpty();
    }

    @Test
    @Tag("BUG-015")
    void un_compte_se_retrouve_par_son_numero() {
        assertThat(comptes.parNumero("FR76-0001-0002")).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(2L));
        assertThat(comptes.parNumero("FR76-9999-9999")).isEmpty();
    }

    @Test
    @Tag("FR-004-09")
    void le_changement_d_etat_est_conditionnel_et_ne_touche_que_l_etat() {
        VirementProgramme cree = enregistrer(1, "Motif", "2027-01-01", "2026-12-31T23:00:00Z");

        assertThat(virements.changerEtat(cree.id(), 8, true)).isFalse();
        assertThat(virements.changerEtat(cree.id(), 1, false)).isFalse();
        assertThat(virements.changerEtat(cree.id(), 1, true)).isTrue();
        assertThat(virements.changerEtat(cree.id(), 1, true)).isFalse();
        assertThat(virements.listerDuClient(1)).containsExactly(
                new VirementProgramme(cree.id(), cree.clientId(), cree.compteSourceId(), cree.numeroBeneficiaire(),
                        cree.libelleBeneficiaire(), cree.montant(), cree.frequence(), cree.motif(),
                        cree.prochaineExecution(), false, cree.nbExecutions(), cree.creeLe()));
        assertThat(virements.changerEtat(cree.id(), 1, false)).isTrue();
        assertThat(virements.listerDuClient(1)).containsExactly(cree);
    }

    @Test
    @Tag("FR-004-10")
    @Tag("BUG-013")
    void la_suppression_est_limitee_au_client_et_independante_de_l_etat() {
        VirementProgramme actif = enregistrer(1, null, "2027-01-01", "2026-12-31T23:00:00Z");
        VirementProgramme inactif = enregistrer(1, null, "2027-02-01", "2027-01-31T23:00:00Z");
        virements.changerEtat(inactif.id(), 1, true);

        assertThat(virements.supprimer(actif.id(), 8)).isFalse();
        assertThat(virements.supprimer(actif.id(), 1)).isTrue();
        assertThat(virements.supprimer(actif.id(), 1)).isFalse();
        assertThat(virements.supprimer(inactif.id(), 1)).isTrue();
        assertThat(virements.listerDuClient(1)).isEmpty();
    }

    @Test
    void les_echeances_dues_sont_les_actives_atteintes_par_echeance_puis_identifiant() {
        VirementProgramme tard = enregistrer(1, null, "2027-01-01", "2026-11-17T08:55:00Z");
        VirementProgramme tot = enregistrer(8, null, "2027-01-01", "2026-11-17T07:00:00Z");
        VirementProgramme inactif = enregistrer(1, null, "2027-01-01", "2026-11-17T06:00:00Z");
        enregistrer(1, null, "2027-01-01", "2026-11-17T09:00:01Z");
        virements.changerEtat(inactif.id(), 1, true);

        assertThat(virements.dues(Instant.parse("2026-11-17T09:00:00Z"))).extracting(VirementProgramme::id)
                .containsExactly(tot.id(), tard.id());
        assertThat(virements.dues(Instant.parse("2026-11-17T08:55:00Z"))).extracting(VirementProgramme::id)
                .containsExactly(tot.id(), tard.id());
        assertThat(virements.dues(Instant.parse("2026-11-17T08:54:59Z"))).extracting(VirementProgramme::id)
                .containsExactly(tot.id());
    }

    @Test
    void la_prise_d_une_echeance_est_conditionnelle_et_avance_la_date_compte_et_date_la_derniere_execution() {
        VirementProgramme cree = enregistrer(1, null, "2027-01-01", "2026-11-17T07:00:00Z");
        Instant prevue = cree.prochaineExecution();
        Instant prochaine = Instant.parse("2026-12-17T07:00:00Z");
        Instant executeLe = Instant.parse("2026-11-17T07:04:00Z");

        assertThat(virements.prendreEcheance(cree.id(), prevue.plusSeconds(1), prochaine, executeLe)).isFalse();
        assertThat(virements.prendreEcheance(cree.id(), prevue, prochaine, executeLe)).isTrue();
        assertThat(virements.prendreEcheance(cree.id(), prevue, prochaine, executeLe)).isFalse();

        VirementProgramme relu = virements.listerDuClient(1).getFirst();
        assertThat(relu.prochaineExecution()).isEqualTo(prochaine);
        assertThat(relu.nbExecutions()).isEqualTo(1);
        assertThat(jdbc.sql("select derniere_execution from virements_programmes where id = :id")
                .param("id", cree.id()).query(java.sql.Timestamp.class).single().toInstant()).isEqualTo(executeLe);
    }

    @Test
    void une_echeance_inactive_ne_peut_pas_etre_prise() {
        VirementProgramme cree = enregistrer(1, null, "2027-01-01", "2026-11-17T07:00:00Z");
        virements.changerEtat(cree.id(), 1, true);

        assertThat(virements.prendreEcheance(cree.id(), cree.prochaineExecution(),
                Instant.parse("2026-12-17T07:00:00Z"), Instant.parse("2026-11-17T07:04:00Z"))).isFalse();
        assertThat(virements.listerDuClient(1).getFirst().nbExecutions()).isZero();
    }

    @Test
    @Tag("FR-004-12")
    @Tag("FR-021-04")
    void la_desactivation_est_conditionnelle_a_l_echeance_prevue_et_a_l_etat_actif() {
        VirementProgramme cree = enregistrer(1, null, "2027-01-01", "2026-11-17T07:00:00Z");
        Instant prevue = cree.prochaineExecution();

        assertThat(virements.desactiver(cree.id(), prevue.plusSeconds(1))).isFalse();
        assertThat(virements.listerDuClient(1).getFirst().actif()).isTrue();
        assertThat(virements.desactiver(cree.id(), prevue)).isTrue();
        assertThat(virements.desactiver(cree.id(), prevue)).isFalse();

        VirementProgramme relu = virements.listerDuClient(1).getFirst();
        assertThat(relu.actif()).isFalse();
        assertThat(relu.prochaineExecution()).isEqualTo(prevue);
        assertThat(relu.nbExecutions()).isZero();
    }

    @Test
    @Tag("FR-004-12")
    @Tag("FR-021-04")
    void la_marque_de_notification_n_est_posee_qu_une_fois_par_virement_et_par_echeance() {
        Instant echeance = Instant.parse("2026-11-17T07:00:00Z");
        Instant le = Instant.parse("2026-11-17T09:00:00Z");

        assertThat(virements.marquerNotifiee(5, echeance, le)).isTrue();
        assertThat(virements.marquerNotifiee(5, echeance, le.plusSeconds(300))).isFalse();
        assertThat(virements.marquerNotifiee(5, echeance.plusSeconds(3600), le)).isTrue();
        assertThat(virements.marquerNotifiee(6, echeance, le)).isTrue();
        assertThat(jdbc.sql("select count(*) from echeances_notifiees").query(Long.class).single()).isEqualTo(3L);
        assertThat(jdbc.sql("select notifiee_le from echeances_notifiees where virement_programme_id = 5"
                + " and echeance = :echeance").param("echeance", java.sql.Timestamp.from(echeance))
                .query(java.sql.Timestamp.class).single().toInstant()).isEqualTo(le);
    }
}
