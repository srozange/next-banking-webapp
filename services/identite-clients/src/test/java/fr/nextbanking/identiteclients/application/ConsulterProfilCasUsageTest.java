package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsulterProfilCasUsageTest {

    private static final Client USER1 = new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "h");

    private final DepotClients depot = new DepotClients() {
        @Override
        public Optional<Client> parIdentifiant(String identifiant) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Client> parId(long clientId) {
            return clientId == 1L ? Optional.of(USER1) : Optional.empty();
        }
    };

    private final ConsulterProfilCasUsage casUsage = new ConsulterProfilCasUsage(depot);

    @Test
    void rend_le_client_designe() {
        assertThat(casUsage.consulter(1L)).contains(USER1);
    }

    @Test
    void ne_rend_rien_pour_un_client_absent() {
        assertThat(casUsage.consulter(99L)).isEmpty();
    }
}
