package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
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

/**
 * Machine à cinq états sur une vraie base : {@code UPDATE … WHERE id = ? AND etat = ?}. Toute transition hors de la
 * table (REJETÉ → LIBÉRÉ, EXÉCUTÉ → SUSPENDU…) est refusée sans effet, zéro ligne ; aucun solde ne change.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Tag("FR-027-16")
class MachineEtatsVirementJdbcTest {

    private static final Instant DATE = Instant.parse("2026-11-17T09:00:00Z");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private DepotVirements depot;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private void virement(EtatVirement etat) {
        jdbc.sql("truncate table virements restart identity cascade").update();
        jdbc.sql("""
                insert into virements (id, client_id, compte_source_id, compte_destinataire_id, montant, etat, statut,
                                       soumis_le)
                values (12, 1, 1, 2, 100.00, :etat, 'SUCCESS', timestamptz '2026-11-17 09:00:00+00')""")
                .param("etat", etat.name()).update();
    }

    private String etat() {
        return jdbc.sql("select etat from virements where id = 12").query(String.class).single();
    }

    @Test
    void seules_les_transitions_de_la_table_modifient_une_ligne() {
        for (EtatVirement de : EtatVirement.values()) {
            for (EtatVirement vers : EtatVirement.values()) {
                virement(de);

                boolean transite = depot.transiter(12, de, vers,
                        new DecisionRevue(IssueRevue.REJETE, "99", "motif", DATE));

                assertThat(transite).as(de + " -> " + vers).isEqualTo(de.peutTransiterVers(vers));
                assertThat(etat()).isEqualTo(transite ? vers.name() : de.name());
            }
        }
        assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                .isEqualTo("2130.00");
    }

    @Test
    void une_transition_depuis_un_autre_etat_que_celui_attendu_est_refusee_sans_effet() {
        virement(EtatVirement.REJETE);

        assertThat(depot.transiter(12, EtatVirement.SUSPENDU, EtatVirement.LIBERE, null)).isFalse();
        assertThat(depot.transiter(12, EtatVirement.LIBERE, EtatVirement.EXECUTE, null)).isFalse();
        assertThat(etat()).isEqualTo("REJETE");
    }

    @Test
    void la_decision_est_enregistree_avec_la_transition_et_une_transition_sans_decision_ne_l_ecrase_pas() {
        virement(EtatVirement.SUSPENDU);

        depot.transiter(12, EtatVirement.SUSPENDU, EtatVirement.LIBERE,
                new DecisionRevue(IssueRevue.LIBERE, "99", "vérifié", DATE));
        depot.transiter(12, EtatVirement.LIBERE, EtatVirement.EXECUTE, null);

        assertThat(jdbc.sql("select etat || '|' || issue || '|' || decideur || '|' || motif_decision from virements")
                .query(String.class).single()).isEqualTo("EXECUTE|LIBERE|99|vérifié");
    }
}
