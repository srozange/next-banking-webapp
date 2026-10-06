package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.MDP_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.SeConnecterCasUsage;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
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
 * Cas d'usage de connexion réel (base, outbox d'audit, transactions Spring), avec un double du port
 * {@link Transaction} qui annule la connexion après l'écriture d'audit : l'entrée reste dans {@code outbox_audit},
 * car elle a été écrite par {@link Transaction#executerIndependamment}.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class)
@DirtiesContext
@Tag("FR-026-04")
@Tag("FR-001-12")
class AuditConnexionIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DepotClients depot;

    @Autowired
    private MotsDePasse motsDePasse;

    @Autowired
    private EmetteurJeton emetteur;

    @Autowired
    private JournalAudit journal;

    @Autowired
    private Transaction transaction;

    @Autowired
    private Horloge horloge;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void une_connexion_annulee_apres_l_ecriture_d_audit_garde_son_entree_login() {
        long depuis = LignesAudit.dernierId(jdbc);

        assertThatThrownBy(() -> casUsageQuiAnnule().seConnecter("user1", MDP_CLIENT, "203.0.113.7"))
                .hasMessage("annulation voulue");

        List<LignesAudit.Ligne> lignes = LignesAudit.depuis(jdbc, depuis);
        assertThat(lignes).singleElement().satisfies(ligne -> {
            assertThat(ligne.cle()).isEqualTo("1");
            assertThat(ligne.donnees().get("action").asString()).isEqualTo("LOGIN");
            assertThat(ligne.donnees().get("utilisateur").asString()).isEqualTo("1");
            assertThat(ligne.donnees().get("adresseIp").asString()).isEqualTo("203.0.113.7");
        });
    }

    @Test
    void une_tentative_echouee_annulee_garde_son_entree_login_failed() {
        long depuis = LignesAudit.dernierId(jdbc);

        assertThatThrownBy(() -> casUsageQuiAnnule().seConnecter("retro-compte-inexistant", "x", "203.0.113.7"))
                .hasMessage("annulation voulue");

        assertThat(LignesAudit.depuis(jdbc, depuis)).singleElement().satisfies(ligne -> {
            assertThat(ligne.cle()).isEqualTo("retro-compte-inexistant");
            assertThat(ligne.donnees().get("action").asString()).isEqualTo("LOGIN_FAILED");
        });
    }

    @Test
    void sans_annulation_l_entree_est_ecrite_puis_le_refus_rendu() {
        long depuis = LignesAudit.dernierId(jdbc);
        SeConnecterCasUsage casUsage = new SeConnecterCasUsage(depot, motsDePasse, emetteur, journal, transaction,
                horloge);

        assertThatThrownBy(() -> casUsage.seConnecter("user1", "mauvais", "203.0.113.7"))
                .isInstanceOf(ConnexionRefusee.class);

        assertThat(LignesAudit.depuis(jdbc, depuis)).singleElement()
                .satisfies(ligne -> assertThat(ligne.donnees().get("action").asString()).isEqualTo("LOGIN_FAILED"));
    }

    private SeConnecterCasUsage casUsageQuiAnnule() {
        Transaction quiAnnule = new Transaction() {
            @Override
            public <T> T executer(Supplier<T> travail) {
                return transaction.executer(() -> {
                    travail.get();
                    throw new IllegalStateException("annulation voulue");
                });
            }

            @Override
            public <T> T executerIndependamment(Supplier<T> travail) {
                return transaction.executerIndependamment(travail);
            }
        };
        return new SeConnecterCasUsage(depot, motsDePasse, emetteur, journal, quiAnnule, horloge);
    }
}
