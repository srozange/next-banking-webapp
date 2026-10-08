package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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

/** Décompte des plafonds sur une vraie base : par client et par mois. */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-006-01")
@Tag("FR-006-04")
class PlafondsJdbcIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Plafonds plafonds;
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

    private void plafond(long clientId, String categorie, String mois) {
        jdbc.sql("insert into plafonds_budget (client_id, categorie, mois, plafond) values (:c, :k, :m, 500.00)")
                .param("c", clientId).param("k", categorie).param("m", mois).update();
    }

    @Test
    void seuls_les_plafonds_du_client_et_du_mois_sont_comptes() {
        plafond(1, "ALIMENTATION", "2026-03");
        plafond(1, "LOGEMENT", "2026-03");
        plafond(1, "LOGEMENT", "2026-04");
        plafond(8, "LOGEMENT", "2026-03");

        assertThat(plafonds.compter(1, "2026-03")).isEqualTo(2);
        assertThat(plafonds.compter(1, "2026-04")).isEqualTo(1);
        assertThat(plafonds.compter(1, "2026-05")).isZero();
        assertThat(plafonds.compter(10, "2026-03")).isZero();
    }
}
