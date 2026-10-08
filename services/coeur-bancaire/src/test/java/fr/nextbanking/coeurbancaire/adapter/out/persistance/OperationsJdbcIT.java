package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
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

/** Lectures du budget sur les opérations, sur une vraie base : mois en Europe/Paris, tous les comptes du client. */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-006-01")
@Tag("FR-006-02")
@Tag("FR-006-03")
class OperationsJdbcIT {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Operations operations;
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

    private void operation(long compteId, String type, String montant, String description, String date) {
        jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation)
                values (:c, :t, :m, 1.00, :d, :date)""")
                .param("c", compteId).param("t", type).param("m", new java.math.BigDecimal(montant))
                .param("d", description).param("date", java.sql.Timestamp.from(Instant.parse(date))).update();
    }

    @Test
    void les_mois_sont_calcules_en_europe_paris_sur_tous_les_comptes_du_client_du_plus_recent_au_plus_ancien() {
        operation(1, "DEBIT", "5.00", "a", "2026-02-28T23:30:00Z");
        operation(2, "CREDIT", "5.00", "b", "2026-01-10T10:00:00Z");
        operation(1, "DEBIT", "5.00", "c", "2026-01-20T10:00:00Z");
        operation(3, "DEBIT", "5.00", "autrui", "2026-06-20T10:00:00Z");

        assertThat(operations.moisAvecOperations(1, PARIS, 24))
                .containsExactly(YearMonth.parse("2026-03"), YearMonth.parse("2026-01"));
        assertThat(operations.moisAvecOperations(1, PARIS, 1)).containsExactly(YearMonth.parse("2026-03"));
        assertThat(operations.moisAvecOperations(10, PARIS, 24)).isEmpty();
    }

    @Test
    void les_operations_de_la_periode_sont_celles_de_tous_les_comptes_du_client_bornes_semi_ouvertes() {
        operation(1, "DEBIT", "1.00", "debut", "2026-03-01T00:00:00Z");
        operation(2, "CREDIT", "2.00", "milieu", "2026-03-15T00:00:00Z");
        operation(1, "DEBIT", "3.00", "fin", "2026-04-01T00:00:00Z");
        operation(1, "DEBIT", "4.00", "avant", "2026-02-28T23:59:59Z");
        operation(3, "DEBIT", "5.00", "autrui", "2026-03-15T00:00:00Z");

        assertThat(operations.duClientSurPeriode(1, Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"))).extracting(o -> o.description())
                .containsExactly("debut", "milieu");
    }
}
