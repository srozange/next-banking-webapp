package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.ErreurReleve;
import fr.nextbanking.coeurbancaire.domain.ErreurReleve.Cause;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterReleveCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterReleveCasUsage cas = new ConsulterReleveCasUsage(memoire, memoire);

    private void comptes() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, BigDecimal.TEN));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, BigDecimal.TEN));
    }

    private static Operation operation(long compteId, String description, String date) {
        return new Operation(compteId, TypeOperation.CREDIT, new BigDecimal("1.00"), new BigDecimal("11.00"),
                description, Instant.parse(date), 0);
    }

    @Test
    @Tag("FR-005-05")
    void toutes_les_operations_du_compte_sont_rendues_par_date_decroissante() {
        comptes();
        memoire.operations.add(operation(1, "ancienne", "2026-11-01T08:00:00Z"));
        memoire.operations.add(operation(3, "autre compte", "2026-11-05T08:00:00Z"));
        memoire.operations.add(operation(1, "recente", "2026-11-03T08:00:00Z"));
        memoire.operations.add(operation(1, "milieu", "2026-11-02T08:00:00Z"));

        assertThat(cas.operations(7, 1)).extracting(Operation::description)
                .containsExactly("recente", "milieu", "ancienne");
    }

    @Test
    @Tag("FR-005-05")
    void a_date_egale_l_ordre_du_port_est_conserve() {
        comptes();
        memoire.operations.add(operation(1, "inscrite en premier", "2026-11-03T08:00:00Z"));
        memoire.operations.add(operation(1, "inscrite en second", "2026-11-03T08:00:00Z"));

        assertThat(cas.operations(7, 1)).extracting(Operation::description)
                .containsExactly("inscrite en premier", "inscrite en second");
    }

    @Test
    @Tag("FR-005-05")
    void un_compte_sans_operation_rend_une_liste_vide() {
        comptes();

        assertThat(cas.operations(7, 1)).isEmpty();
    }

    @Test
    @Tag("FR-005-05")
    void le_compte_d_un_autre_client_est_refuse_sans_aucune_operation() {
        comptes();
        memoire.operations.add(operation(3, "secrete", "2026-11-05T08:00:00Z"));

        assertThatThrownBy(() -> cas.operations(7, 3)).isInstanceOfSatisfying(ErreurReleve.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.COMPTE_ETRANGER));
    }

    @Test
    @Tag("FR-005-05")
    void un_compte_inexistant_est_introuvable() {
        comptes();

        assertThatThrownBy(() -> cas.operations(7, 999)).isInstanceOfSatisfying(ErreurReleve.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.COMPTE_INTROUVABLE));
    }
}
