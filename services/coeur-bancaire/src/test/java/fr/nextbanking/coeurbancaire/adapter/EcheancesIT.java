package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterEcheances;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exécution des échéances dues sur une vraie base (FEAT-021) : débit, crédit, opérations, avance de la date, audit sans
 * adresse IP, aucun événement de décision ni de remise, ligne du journal d'exécution.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Import(EcheancesIT.ConfigurationHorloge.class)
@Tag("FR-021-01")
@Tag("FR-021-03")
@Tag("FR-021-06")
@Tag("FR-021-08")
@Tag("FR-021-10")
@Tag("FR-004-07")
@Tag("FR-004-12")
@Tag("FR-004-13")
@Tag("FR-021-04")
@Tag("BUG-014")
@Tag("BUG-068")
class EcheancesIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @Autowired
    private HorlogeFixe horloge;
    @Autowired
    private ExecuterEcheances echeances;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
        horloge.regler(Instant.parse("2026-01-15T07:04:00Z"));
        jdbc.sql("""
                insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire,
                    libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le)
                values (1, 1, 1, 'FR76-0001-0002', 'Livret', 100.00, 'MENSUEL', 'Epargne mensuelle',
                    '2026-01-15T07:00:00Z', true, 0, now())""").update();
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private List<String> audit() {
        return jdbc.sql("select charge from outbox_audit order by id").query(String.class).list();
    }

    @Test
    void une_echeance_due_est_executee_de_bout_en_bout() {
        assertThat(echeances.executerLesDues()).isEqualTo(1);

        assertThat(jdbc.sql("select solde from comptes where id = 1").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("2350.00");
        assertThat(jdbc.sql("select solde from comptes where id = 2").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("15100.00");
        assertThat(jdbc.sql("select description from operations order by id").query(String.class).list())
                .containsExactly("Virement programme vers Livret - Epargne mensuelle",
                        "Virement programme de FR76-0001-0001 - Epargne mensuelle");
        assertThat(jdbc.sql("select etat from virements").query(String.class).single()).isEqualTo("EXECUTE");
        assertThat(jdbc.sql("select nb_executions from virements_programmes").query(Integer.class).single())
                .isEqualTo(1);
        assertThat(jdbc.sql("select prochaine_execution from virements_programmes").query(java.sql.Timestamp.class)
                .single().toInstant()).isEqualTo(Instant.parse("2026-02-15T07:00:00Z"));
        assertThat(jdbc.sql("select derniere_execution from virements_programmes").query(java.sql.Timestamp.class)
                .single().toInstant()).isEqualTo(Instant.parse("2026-01-15T07:04:00Z"));
        // Aucune notification : seuls les événements compte-modifie (source puis destinataire) et operation-enregistree
        // (débit puis crédit) sont consignés.
        assertThat(jdbc.sql("select topic || ':' || cle from outbox order by id").query(String.class).list())
                .containsExactly("coeur-bancaire.compte-modifie.v1:1", "coeur-bancaire.compte-modifie.v1:2",
                        "coeur-bancaire.operation-enregistree.v1:1", "coeur-bancaire.operation-enregistree.v1:2");
        assertThat(jdbc.sql("select count(*) from alertes_fraude").query(Long.class).single()).isZero();
    }

    @Test
    void l_audit_est_ecrit_au_nom_du_titulaire_sans_adresse_ip() {
        echeances.executerLesDues();

        List<String> charges = audit();
        assertThat(charges).hasSize(2);
        assertThat(json.readTree(charges.get(0)).get("donnees").get("action").asString()).isEqualTo("VIREMENT");
        assertThat(charges.get(0)).contains("Montant: 100.00 - Motif: Epargne mensuelle - Issue: EXECUTE");
        assertThat(json.readTree(charges.get(1)).get("donnees").get("action").asString())
                .isEqualTo("TRANSFER_EXECUTED");
        for (String charge : charges) {
            var donnees = json.readTree(charge).get("donnees");
            assertThat(donnees.get("utilisateur").asString()).isEqualTo("1");
            assertThat(donnees.get("adresseIp") == null || donnees.get("adresseIp").isNull()).isTrue();
        }
    }

    @Test
    void chaque_passage_ecrit_une_ligne_du_journal_et_la_date_suivante_ne_derive_pas() {
        echeances.executerLesDues();
        horloge.regler(Instant.parse("2026-01-15T07:09:00Z"));
        assertThat(echeances.executerLesDues()).isZero();

        assertThat(jdbc.sql("select job || '|' || enregistrements || '|' || statut || '|' || details"
                + " from batch_job_logs order by id").query(String.class).list()).containsExactly(
                        "RECURRING_TRANSFERS|1|SUCCESS|Verification virements programmes dus",
                        "RECURRING_TRANSFERS|0|SUCCESS|Verification virements programmes dus");
        assertThat(jdbc.sql("select prochaine_execution from virements_programmes").query(java.sql.Timestamp.class)
                .single().toInstant()).isEqualTo(Instant.parse("2026-02-15T07:00:00Z"));
    }

    private List<String> evenements() {
        return jdbc.sql("select charge from outbox where topic = 'coeur-bancaire.echeance-non-executee.v1'"
                + " order by id").query(String.class).list();
    }

    @Test
    void un_solde_insuffisant_laisse_l_echeance_due_sans_ecriture_et_notifie_une_seule_fois() {
        jdbc.sql("update comptes set solde = 99.99 where id = 1").update();

        assertThat(echeances.executerLesDues()).isZero();

        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from virements").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("99.99");
        assertThat(jdbc.sql("select nb_executions from virements_programmes").query(Integer.class).single())
                .isZero();
        assertThat(jdbc.sql("select count(*) from virements_programmes where derniere_execution is null"
                + " and actif and prochaine_execution = '2026-01-15T07:00:00Z'").query(Long.class).single())
                .isEqualTo(1);
        List<String> charges = evenements();
        assertThat(charges).hasSize(1);
        var donnees = json.readTree(charges.getFirst()).get("donnees");
        assertThat(donnees.get("virementProgrammeId").asLong()).isEqualTo(1L);
        assertThat(donnees.get("notification").get("clientId").asLong()).isEqualTo(1L);
        assertThat(donnees.get("notification").get("type").asString()).isEqualTo("ALERTE");
        assertThat(donnees.get("notification").get("titre").asString()).isEqualTo("Virement programmé non exécuté");
        assertThat(donnees.get("notification").get("message").asString()).isEqualTo(
                "Virement programmé vers Livret non exécuté : solde insuffisant, rien n'a été débité");

        horloge.regler(Instant.parse("2026-01-15T07:09:00Z"));
        assertThat(echeances.executerLesDues()).isZero();
        assertThat(evenements()).hasSize(1);

        jdbc.sql("update comptes set solde = 2450.00 where id = 1").update();
        horloge.regler(Instant.parse("2026-01-15T07:14:00Z"));
        assertThat(echeances.executerLesDues()).isEqualTo(1);
        assertThat(evenements()).hasSize(1);
    }

    @Test
    void un_compte_destinataire_introuvable_est_refuse_avant_le_solde_et_desactive_le_virement() {
        jdbc.sql("update comptes set solde = 10.00 where id = 1").update();
        jdbc.sql("update virements_programmes set numero_beneficiaire = 'FR76-9999-9999'").update();

        assertThat(echeances.executerLesDues()).isZero();

        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from virements").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("10.00");
        assertThat(jdbc.sql("select actif from virements_programmes").query(Boolean.class).single()).isFalse();
        List<String> charges = evenements();
        assertThat(charges).hasSize(1);
        assertThat(json.readTree(charges.getFirst()).get("donnees").get("notification").get("titre").asString())
                .isEqualTo("Virement programmé refusé");

        horloge.regler(Instant.parse("2026-01-15T07:09:00Z"));
        assertThat(echeances.executerLesDues()).isZero();
        assertThat(evenements()).hasSize(1);
        assertThat(jdbc.sql("select count(*) from echeances_notifiees").query(Long.class).single()).isEqualTo(1L);
    }
}
