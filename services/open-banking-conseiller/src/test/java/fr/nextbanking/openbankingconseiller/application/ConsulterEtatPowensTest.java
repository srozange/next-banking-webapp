package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.SituationPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-012-17")
@Tag("FR-012-10")
class ConsulterEtatPowensTest {

    private static DepotEtatPowens depot(Optional<EtatPowens> etat) {
        return new DepotEtatPowens() {
            @Override
            public Optional<EtatPowens> lire(String sub) {
                return etat;
            }

            @Override
            public void enregistrerJeton(String sub, String jeton) {
                throw new AssertionError("non attendu");
            }

            @Override
            public void enregistrerState(StatePowens state) {
                throw new AssertionError("non attendu");
            }

            @Override
            public boolean consommerState(StatePowens state, Instant maintenant) {
                throw new AssertionError("non attendu");
            }

            @Override
            public void remettreAZero(String sub) {
                throw new AssertionError("non attendu");
            }

            @Override
            public void poserMessage(String sub, String message) {
                throw new AssertionError("non attendu");
            }

            @Override
            public void enregistrerConnexion(String sub, List<CompteExterne> comptes, Long selectionne,
                    List<TransactionExterne> transactions, String message) {
                throw new AssertionError("non attendu");
            }
        };
    }

    private static DepotComptesExternes comptes(List<CompteExterne> comptes, Long selectionne,
            List<TransactionExterne> transactions) {
        return new DepotComptesExternes() {
            @Override
            public List<CompteExterne> comptes(String sub) {
                return comptes;
            }

            @Override
            public Optional<Long> compteSelectionne(String sub) {
                return Optional.ofNullable(selectionne);
            }

            @Override
            public List<TransactionExterne> transactions(String sub) {
                return transactions;
            }

            @Override
            public boolean appartient(String sub, long compteId) {
                throw new AssertionError("non attendu");
            }

            @Override
            public void enregistrerSelection(String sub, long compteId, List<TransactionExterne> transactions) {
                throw new AssertionError("non attendu");
            }
        };
    }

    @Test
    void sans_ligne_en_base_le_client_est_non_connecte() {
        SituationPowens situation = new ConsulterEtatPowensCasUsage(depot(Optional.empty()),
                comptes(List.of(), null, List.of())).consulter("10");

        assertThat(situation.etat()).isEqualTo(EtatPowens.nonConnecte());
        assertThat(situation.comptes()).isEmpty();
        assertThat(situation.patrimoineTotal()).hasToString("0.00");
    }

    @Test
    void l_etat_les_comptes_la_selection_et_les_transactions_sont_rendus_tels_quels() {
        EtatPowens etat = new EtatPowens("jeton", true, "message");
        List<CompteExterne> liste = List.of(new CompteExterne(1, "A", "checking", new BigDecimal("5.00"), null));
        List<TransactionExterne> transactions = List.of(
                new TransactionExterne(9, LocalDate.of(2026, 10, 1), "Salaire", new BigDecimal("1.00")));

        SituationPowens situation = new ConsulterEtatPowensCasUsage(depot(Optional.of(etat)),
                comptes(liste, 1L, transactions)).consulter("10");

        assertThat(situation).isEqualTo(new SituationPowens(etat, liste, 1L, transactions));
    }
}
