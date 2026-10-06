package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTableauDeBord.TableauDeBord;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterTableauDeBordCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterTableauDeBordCasUsage cas = new ConsulterTableauDeBordCasUsage(memoire, memoire);

    private void compte(long id, long client, TypeCompte type, String solde) {
        memoire.comptes.put(id, new Compte(id, client, "FR76-000" + id, type, new BigDecimal(solde)));
    }

    private void operation(long compteId, String description, String date) {
        memoire.operations.add(new Operation(compteId, TypeOperation.CREDIT, new BigDecimal("1.00"),
                new BigDecimal("11.00"), description, Instant.parse(date), 0));
    }

    @Test
    @Tag("FR-002-03")
    void le_solde_total_est_la_somme_des_soldes_de_tous_les_comptes_du_client() {
        compte(1, 7, TypeCompte.COURANT, "2130.00");
        compte(2, 7, TypeCompte.EPARGNE, "15200.50");
        compte(3, 8, TypeCompte.COURANT, "999.00");

        TableauDeBord vue = cas.tableauDeBord(7);

        assertThat(vue.comptes()).extracting(Compte::id).containsExactly(1L, 2L);
        assertThat(vue.soldeTotal()).isEqualTo(new BigDecimal("17330.50"));
    }

    @Test
    @Tag("FR-002-04")
    void un_client_sans_compte_a_un_solde_total_de_zero_et_aucune_operation() {
        TableauDeBord vue = cas.tableauDeBord(7);

        assertThat(vue.comptes()).isEmpty();
        assertThat(vue.soldeTotal()).isEqualTo(new BigDecimal("0.00"));
        assertThat(vue.dernieresOperations()).isEmpty();
    }

    @Test
    @Tag("FR-002-05")
    void un_client_sans_compte_courant_n_a_aucune_operation() {
        compte(1, 7, TypeCompte.EPARGNE, "10.00");
        operation(1, "epargne", "2026-11-01T08:00:00Z");

        TableauDeBord vue = cas.tableauDeBord(7);

        assertThat(vue.dernieresOperations()).isEmpty();
        assertThat(vue.soldeTotal()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    @Tag("FR-002-06")
    @Tag("BUG-005")
    void les_operations_sont_celles_du_compte_courant_meme_si_l_epargne_a_ete_creee_avant() {
        compte(1, 7, TypeCompte.EPARGNE, "10.00");
        compte(2, 7, TypeCompte.COURANT, "20.00");
        operation(1, "epargne", "2026-11-09T08:00:00Z");
        for (int jour = 1; jour <= 7; jour++) {
            operation(2, "courant " + jour, "2026-11-0" + jour + "T08:00:00Z");
        }

        TableauDeBord vue = cas.tableauDeBord(7);

        assertThat(vue.dernieresOperations()).extracting(Operation::description)
                .containsExactly("courant 7", "courant 6", "courant 5", "courant 4", "courant 3");
    }

    @Test
    @Tag("FR-002-06")
    void a_date_egale_l_ordre_du_port_est_conserve_et_moins_de_cinq_operations_sont_toutes_rendues() {
        compte(1, 7, TypeCompte.COURANT, "20.00");
        operation(1, "premiere", "2026-11-03T08:00:00Z");
        operation(1, "seconde", "2026-11-03T08:00:00Z");

        assertThat(cas.tableauDeBord(7).dernieresOperations()).extracting(Operation::description)
                .containsExactly("premiere", "seconde");
    }
}
