package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.domain.TypeSignal;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Signaux d'historique, bénéficiaires et alertes sur une vraie base : comptages, bornes, verrou consultatif du client.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-027-01")
@Tag("FR-003-15")
class HistoriqueVirementsJdbcIT {

    private static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private HistoriqueVirements historique;

    @Autowired
    private Beneficiaires beneficiaires;

    @Autowired
    private AlertesFraude alertes;

    @Autowired
    private TransactionTemplate transaction;

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

    private void virement(long id, String montant, String etat, Instant soumisLe) {
        jdbc.sql("""
                insert into virements (id, client_id, compte_source_id, compte_destinataire_id, montant, etat, statut,
                                       soumis_le)
                values (:id, 1, 1, 2, :montant, :etat, 'SUCCESS', :soumisLe)""")
                .param("id", id).param("montant", new BigDecimal(montant)).param("etat", etat)
                .param("soumisLe", java.sql.Timestamp.from(soumisLe)).update();
    }

    private void operation(long compteId, String type, Instant date) {
        jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation)
                values (:compte, :type, 1.00, 1.00, 'x', :date)""")
                .param("compte", compteId).param("type", type).param("date", java.sql.Timestamp.from(date)).update();
    }

    @Test
    @Tag("FR-027-06")
    @Tag("BUG-010")
    @Tag("BUG-085")
    void les_operations_comptent_debits_et_credits_des_comptes_du_client_depuis_le_debut_du_jour_paris() {
        Instant debut = Instant.parse("2026-11-16T23:00:00Z");
        operation(1, "DEBIT", debut.minusSeconds(1));
        operation(2, "CREDIT", debut.minusSeconds(86400));
        operation(1, "DEBIT", debut);
        operation(2, "CREDIT", MAINTENANT);
        operation(3, "DEBIT", MAINTENANT);

        assertThat(historique.operationsDepuis(1, debut)).isEqualTo(2);
        assertThat(historique.operationsDepuis(8, debut)).isEqualTo(1);
        assertThat(historique.operationsDepuis(99, debut)).isZero();
    }

    @Test
    @Tag("FR-003-23")
    void le_dernier_virement_est_le_plus_recent_enregistre_du_client() {
        assertThat(historique.dernierVirement(1)).isEmpty();
        virement(1, "10.00", "EXECUTE", MAINTENANT.minusSeconds(100));
        virement(2, "10.00", "EXECUTE", MAINTENANT.minusSeconds(29));

        assertThat(historique.dernierVirement(1)).contains(MAINTENANT.minusSeconds(29));
        assertThat(historique.dernierVirement(8)).isEmpty();
    }

    @Test
    @Tag("FR-003-24")
    @Tag("FR-027-10")
    void la_velocite_compte_les_virements_executes_strictement_au_dessus_du_montant_dans_la_fenetre() {
        Instant depuis = MAINTENANT.minusSeconds(3600);
        virement(1, "500.00", "EXECUTE", MAINTENANT);
        virement(2, "500.01", "EXECUTE", MAINTENANT);
        virement(3, "900.00", "EXECUTE", depuis);
        virement(4, "900.00", "EXECUTE", depuis.minusSeconds(1));
        virement(5, "900.00", "SUSPENDU", MAINTENANT);

        assertThat(historique.virementsExecutesAuDessus(1, new BigDecimal("500"), depuis)).isEqualTo(2);
        assertThat(historique.virementsExecutesAuDessus(8, new BigDecimal("500"), depuis)).isZero();
    }

    @Test
    @Tag("FR-003-19")
    @Tag("FR-027-07")
    void un_beneficiaire_n_est_actif_que_pour_son_client_son_numero_et_son_indicateur() {
        jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, actif) values
                (1, 'A', 'FR76-0001-0002', 'actif', true), (1, 'B', 'FR76-9999', 'inactif', false),
                (1, 'C', 'FR76-0000', 'indéterminé', null)""").update();

        assertThat(beneficiaires.estActif(1, "FR76-0001-0002")).isTrue();
        assertThat(beneficiaires.estActif(1, "FR76-9999")).isFalse();
        assertThat(beneficiaires.estActif(1, "FR76-0000")).isFalse();
        assertThat(beneficiaires.estActif(2, "FR76-0001-0002")).isFalse();
        assertThat(beneficiaires.estActif(1, "inconnu")).isFalse();
    }

    @Test
    @Tag("FR-003-25")
    @Tag("FR-027-03")
    void une_alerte_est_ecrite_au_statut_open_avec_son_score_et_sa_description() {
        virement(1, "10.00", "EXECUTE", MAINTENANT);

        alertes.enregistrer(new AlerteFraude(1, 1, TypeSignal.ROUND_AMOUNT, 20, "texte", StatutAlerte.OPEN,
                MAINTENANT));

        assertThat(jdbc.sql("select regle || '/' || score || '/' || statut || '/' || description from alertes_fraude")
                .query(String.class).single()).isEqualTo("ROUND_AMOUNT/20/OPEN/texte");
    }

    @Test
    @Tag("FR-027-01")
    void le_verrou_du_client_bloque_un_autre_controle_du_meme_client_jusqu_a_la_fin_de_la_transaction()
            throws Exception {
        CompletableFuture<Void> releve = new CompletableFuture<>();
        CompletableFuture<Void> pris = new CompletableFuture<>();
        CompletableFuture<Void> premier = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(t -> {
            historique.verrouillerClient(1);
            pris.complete(null);
            releve.join();
        }));
        pris.get(10, TimeUnit.SECONDS);

        CompletableFuture<Instant> memeClient = CompletableFuture.supplyAsync(() -> transaction.execute(t -> {
            historique.verrouillerClient(1);
            return Instant.now();
        }));
        CompletableFuture<Instant> autreClient = CompletableFuture.supplyAsync(() -> transaction.execute(t -> {
            historique.verrouillerClient(2);
            return Instant.now();
        }));

        assertThat(autreClient.get(10, TimeUnit.SECONDS)).isNotNull();
        Thread.sleep(Duration.ofMillis(500));
        assertThat(memeClient).isNotDone();
        releve.complete(null);
        premier.get(10, TimeUnit.SECONDS);
        assertThat(memeClient.get(10, TimeUnit.SECONDS)).isNotNull();
    }
}
