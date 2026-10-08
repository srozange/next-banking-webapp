package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBudget.Budget;
import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterBudgetCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterBudgetCasUsage cas = new ConsulterBudgetCasUsage(memoire, memoire, memoire);

    @BeforeEach
    void comptes() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("10.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("10.00")));
    }

    private void operation(long compteId, TypeOperation type, String montant, String description, String date) {
        memoire.operations.add(new Operation(compteId, type, new Montant(new java.math.BigDecimal(montant)), Montant.lire("1.00"),
                description, Instant.parse(date), 0));
    }

    @Test
    @Tag("FR-006-02")
    void sans_operation_la_periode_est_le_mois_de_la_date_du_jour_du_service() {
        memoire.maintenant = Instant.parse("2026-11-17T09:00:00Z");

        Budget budget = cas.budget(7, null);

        assertThat(budget.aujourdhui()).isEqualTo(LocalDate.parse("2026-11-17"));
        assertThat(budget.periode()).isEqualTo(YearMonth.parse("2026-11"));
        assertThat(budget.periodes()).isEmpty();
        assertThat(budget.totalDepenses().texte()).isEqualTo("0.00");
        assertThat(budget.totalRevenus().texte()).isEqualTo("0.00");
        assertThat(budget.plafondsDefinis()).isZero();
        assertThat(budget.repartition()).isEmpty();
    }

    @Test
    @Tag("FR-006-02")
    void les_periodes_sont_les_24_mois_les_plus_recents_avec_operations_et_le_plus_recent_est_pris() {
        for (int i = 0; i < 30; i++) {
            YearMonth mois = YearMonth.parse("2024-01").plusMonths(i);
            operation(1, TypeOperation.DEBIT, "1.00", "loyer", mois.atDay(10) + "T10:00:00Z");
        }

        Budget budget = cas.budget(7, null);

        assertThat(budget.periodes()).hasSize(24);
        assertThat(budget.periodes().get(0)).isEqualTo(YearMonth.parse("2026-06"));
        assertThat(budget.periodes().get(23)).isEqualTo(YearMonth.parse("2024-07"));
        assertThat(budget.periode()).isEqualTo(YearMonth.parse("2026-06"));
    }

    @Test
    @Tag("FR-006-02")
    void une_periode_illisible_ou_inexistante_est_remplacee_sans_erreur_par_le_mois_le_plus_recent() {
        operation(1, TypeOperation.DEBIT, "1.00", "loyer", "2026-03-10T10:00:00Z");
        operation(1, TypeOperation.DEBIT, "1.00", "loyer", "2026-02-10T10:00:00Z");

        assertThat(cas.budget(7, "abc").periode()).isEqualTo(YearMonth.parse("2026-03"));
        assertThat(cas.budget(7, "2026-13").periode()).isEqualTo(YearMonth.parse("2026-03"));
        assertThat(cas.budget(7, "2026-3").periode()).isEqualTo(YearMonth.parse("2026-03"));
        assertThat(cas.budget(7, "2026-02").periode()).isEqualTo(YearMonth.parse("2026-02"));
    }

    @Test
    @Tag("FR-006-02")
    void les_mois_sont_ceux_d_europe_paris() {
        operation(1, TypeOperation.DEBIT, "5.00", "loyer", "2026-02-28T23:30:00Z");

        Budget mars = cas.budget(7, "2026-03");
        Budget fevrier = cas.budget(7, "2026-02");

        assertThat(mars.periodes()).containsExactly(YearMonth.parse("2026-03"));
        assertThat(mars.totalDepenses().texte()).isEqualTo("5.00");
        assertThat(fevrier.totalDepenses().texte()).isEqualTo("0.00");
    }

    @Test
    @Tag("FR-006-03")
    void seuls_les_debits_sont_ventiles_par_categorie_dans_l_ordre_et_les_categories_nulles_sont_absentes() {
        operation(1, TypeOperation.DEBIT, "800.00", "LOYER mars", "2026-03-01T08:00:00Z");
        operation(2, TypeOperation.DEBIT, "50.50", "Carrefour", "2026-03-02T08:00:00Z");
        operation(1, TypeOperation.DEBIT, "20.00", "Carrefour Market", "2026-03-03T08:00:00Z");
        operation(1, TypeOperation.DEBIT, "7.00", "xyz", "2026-03-04T08:00:00Z");
        operation(1, TypeOperation.DEBIT, "0.00", "sncf", "2026-03-05T08:00:00Z");
        operation(1, TypeOperation.CREDIT, "2000.00", "Salaire", "2026-03-06T08:00:00Z");

        Budget budget = cas.budget(7, "2026-03");

        assertThat(budget.repartition()).extracting(l -> l.categorie(), l -> l.montant().texte()).containsExactly(
                org.assertj.core.api.Assertions.tuple(CategorieBudget.LOGEMENT, "800.00"),
                org.assertj.core.api.Assertions.tuple(CategorieBudget.ALIMENTATION, "70.50"),
                org.assertj.core.api.Assertions.tuple(CategorieBudget.AUTRES, "7.00"));
    }

    @Test
    @Tag("FR-006-04")
    void les_totaux_sont_les_sommes_des_debits_et_des_credits_et_les_plafonds_sont_comptes() {
        operation(1, TypeOperation.DEBIT, "1000.00", "loyer", "2026-03-01T08:00:00Z");
        operation(2, TypeOperation.DEBIT, "234.50", "xyz", "2026-03-31T21:59:59Z");
        operation(1, TypeOperation.CREDIT, "2000.00", "virement", "2026-03-06T08:00:00Z");
        operation(1, TypeOperation.DEBIT, "99.00", "loyer", "2026-04-01T08:00:00Z");
        memoire.enregistrer(7, CategorieBudget.LOGEMENT, "2026-03", Montant.lire("500.00"));
        memoire.enregistrer(7, CategorieBudget.ALIMENTATION, "2026-03", Montant.lire("100.00"));

        Budget budget = cas.budget(7, "2026-03");

        assertThat(budget.totalDepenses().texte()).isEqualTo("1234.50");
        assertThat(budget.totalRevenus().texte()).isEqualTo("2000.00");
        assertThat(budget.plafondsDefinis()).isEqualTo(2);
        assertThat(cas.budget(7, "2026-04").plafondsDefinis()).isZero();
    }

    @Test
    @Tag("FR-006-01")
    void rien_d_un_autre_client_n_est_compte() {
        operation(1, TypeOperation.DEBIT, "10.00", "loyer", "2026-03-01T08:00:00Z");
        operation(3, TypeOperation.DEBIT, "999.00", "loyer", "2026-03-01T08:00:00Z");
        operation(3, TypeOperation.CREDIT, "500.00", "salaire", "2026-05-01T08:00:00Z");
        memoire.enregistrer(8, CategorieBudget.LOGEMENT, "2026-03", Montant.lire("5.00"));

        Budget budget = cas.budget(7, "2026-03");

        assertThat(budget.periodes()).containsExactly(YearMonth.parse("2026-03"));
        assertThat(budget.totalDepenses().texte()).isEqualTo("10.00");
        assertThat(budget.totalRevenus().texte()).isEqualTo("0.00");
        assertThat(budget.plafondsDefinis()).isZero();
    }

    @Test
    @Tag("FR-006-11")
    void les_plafonds_de_la_periode_portent_la_depense_de_leur_categorie_dans_l_ordre_des_categories() {
        operation(1, TypeOperation.DEBIT, "400.00", "loyer", "2026-03-01T08:00:00Z");
        operation(1, TypeOperation.DEBIT, "10.00", "xyz", "2026-03-02T08:00:00Z");
        memoire.enregistrer(7, CategorieBudget.AUTRES, "2026-03", Montant.lire("50.00"));
        memoire.enregistrer(7, CategorieBudget.TRANSPORT, "2026-03", Montant.lire("80.00"));
        memoire.enregistrer(7, CategorieBudget.LOGEMENT, "2026-03", Montant.lire("500.00"));
        memoire.enregistrer(7, CategorieBudget.LOGEMENT, "2026-04", Montant.lire("1.00"));

        Budget budget = cas.budget(7, "2026-03");

        assertThat(budget.plafonds()).extracting(p -> p.categorie(), p -> p.plafond().texte(),
                p -> p.depense().texte()).containsExactly(
                org.assertj.core.api.Assertions.tuple(CategorieBudget.LOGEMENT, "500.00", "400.00"),
                org.assertj.core.api.Assertions.tuple(CategorieBudget.TRANSPORT, "80.00", "0.00"),
                org.assertj.core.api.Assertions.tuple(CategorieBudget.AUTRES, "50.00", "10.00"));
        assertThat(budget.plafondsDefinis()).isEqualTo(3);
    }
}
