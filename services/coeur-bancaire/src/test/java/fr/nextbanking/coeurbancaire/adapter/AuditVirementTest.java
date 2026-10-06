package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.application.port.in.LibererVirement;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterEcheances;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
 * Les six parcours d'un virement et leur journal d'audit (FR-026-02, FR-026-03) : exactement 2, 3, 3, 2, 2 et 1 entrées,
 * dans l'ordre, chacune écrite par {@code outbox_audit} hors de la transaction du virement. Le consommateur Kafka est
 * arrêté : le test rejoue lui-même la remise du virement libéré, pour que le solde reste sous son contrôle. L'audit est
 * lu dans l'outbox d'audit du cœur bancaire (la lecture d'{@code AUDIT_LOGS} appartient au service audit).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class,
        properties = {"spring.kafka.listener.auto-startup=false", "nb.fraude.acces-historique.delai=300ms"})
@DirtiesContext
@Import(AuditVirementTest.ConfigurationHorloge.class)
@Tag("FR-026-02")
@Tag("FR-026-03")
@Tag("FR-027-16")
@Tag("FR-016-19")
class AuditVirementTest {

    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);

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
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;
    @Autowired
    private ExecuterVirement executerVirement;
    @Autowired
    private LibererVirement libererVirement;
    @Autowired
    private RejeterVirement rejeterVirement;
    @Autowired
    private RejeterEcheances rejeterEcheances;
    @Autowired
    private TraiterVirementATraiter traiterVirementATraiter;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void recharger() throws Exception {
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        DonneesDeReference.recharger(source, jdbc);
        jdbc.sql("update comptes set solde = 50000.00 where id = 1").update();
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private long soumettre(String montant) {
        return executerVirement.executer(new Commande(1, 1L, 2L, montant, "loyer", null, "203.0.113.7")).virementId();
    }

    private CommandeDecision decision(long virementId, String motif) {
        return new CommandeDecision(99, ADMIN, virementId, motif, "203.0.113.9");
    }

    private List<String> audit(long virementId) {
        return jdbc.sql("select charge from outbox_audit where cle = :cle order by id")
                .param("cle", Long.toString(virementId)).query(String.class).list().stream()
                .map(charge -> json.readTree(charge).get("donnees").get("action").asString()).toList();
    }

    private String etat(long virementId) {
        return jdbc.sql("select etat from virements where id = :id").param("id", virementId).query(String.class)
                .single();
    }

    @Test
    void execute_deux_entrees() {
        long id = soumettre("100.00");

        assertThat(audit(id)).containsExactly("VIREMENT", "TRANSFER_EXECUTED");
        assertThat(etat(id)).isEqualTo("EXECUTE");
    }

    @Test
    void suspendu_puis_libere_puis_execute_trois_entrees() {
        long id = soumettre("10000.00");
        libererVirement.liberer(decision(id, "vérifié par téléphone"));
        traiterVirementATraiter.traiter(UUID.randomUUID(), id);

        assertThat(audit(id)).containsExactly("VIREMENT", "ADMIN_TRANSFER_RELEASE", "TRANSFER_EXECUTED");
        assertThat(etat(id)).isEqualTo("EXECUTE");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(Long.class).single()).isEqualTo(40000L);
    }

    @Test
    void suspendu_libere_puis_rejete_au_debit_trois_entrees() {
        long id = soumettre("10000.00");
        libererVirement.liberer(decision(id, "vérifié par téléphone"));
        jdbc.sql("update comptes set solde = 9999.99 where id = 1").update();
        traiterVirementATraiter.traiter(UUID.randomUUID(), id);

        assertThat(audit(id)).containsExactly("VIREMENT", "ADMIN_TRANSFER_RELEASE",
                "TRANSFER_REJECT_INSUFFICIENT_FUNDS");
        assertThat(etat(id)).isEqualTo("REJETE");
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("9999.99");
    }

    @Test
    void suspendu_puis_rejete_par_l_administrateur_deux_entrees() {
        long id = soumettre("10000.00");
        rejeterVirement.rejeter(decision(id, "client injoignable"));

        assertThat(audit(id)).containsExactly("VIREMENT", "ADMIN_TRANSFER_REJECT");
        assertThat(etat(id)).isEqualTo("REJETE");
    }

    @Test
    void suspendu_puis_rejete_automatiquement_deux_entrees() {
        long id = soumettre("10000.00");
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(24 * 3600));
        rejeterEcheances.rejeterLesEchus();

        assertThat(audit(id)).containsExactly("VIREMENT", "TRANSFER_AUTO_REJECT");
        assertThat(etat(id)).isEqualTo("REJETE");
        assertThat(jdbc.sql("select count(*) from batch_job_logs where job = 'SUSPENDED_TRANSFERS_EXPIRY' "
                + "and enregistrements = 1 and statut = 'SUCCESS'").query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void refuse_pour_erreur_du_controle_une_entree_conservee_malgre_l_annulation_de_la_transaction() throws Exception {
        try (Connection verrou = source.getConnection()) {
            verrou.setAutoCommit(false);
            verrou.createStatement().execute("select pg_advisory_xact_lock(1)");

            assertThatThrownBy(() -> soumettre("50.00")).isInstanceOf(ErreurVirement.class)
                    .hasMessageContaining("Votre virement n'a pas pu être vérifié");
            verrou.rollback();
        }

        long id = jdbc.sql("select id from virements").query(Long.class).single();
        assertThat(audit(id)).containsExactly("VIREMENT");
        assertThat(jdbc.sql("select charge from outbox_audit").query(String.class).single())
                .contains("Issue: REFUSE_ERREUR_CONTROLE");
        assertThat(etat(id)).isEqualTo("REFUSE_ERREUR_CONTROLE");
        assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select count(*) from outbox").query(Long.class).single()).isZero();
    }
}
