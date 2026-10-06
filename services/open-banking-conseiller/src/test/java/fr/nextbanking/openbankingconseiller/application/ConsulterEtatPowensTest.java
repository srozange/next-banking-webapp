package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-012-17")
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
        };
    }

    @Test
    void sans_ligne_en_base_le_client_est_non_connecte() {
        assertThat(new ConsulterEtatPowensCasUsage(depot(Optional.empty())).consulter("10"))
                .isEqualTo(EtatPowens.nonConnecte());
    }

    @Test
    void l_etat_enregistre_est_rendu_tel_quel() {
        EtatPowens etat = new EtatPowens("jeton", true, "message");

        assertThat(new ConsulterEtatPowensCasUsage(depot(Optional.of(etat))).consulter("10")).isEqualTo(etat);
    }
}
