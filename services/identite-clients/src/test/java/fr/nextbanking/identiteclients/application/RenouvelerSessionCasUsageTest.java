package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-001-10")
@Tag("FR-030-05")
class RenouvelerSessionCasUsageTest {

    private static final Client ACTIF = new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "h");
    private static final Client INACTIF = new Client(5L, "inactif", "Compte Désactivé", Role.CLIENT, false, "h");

    private final List<Client> emis = new ArrayList<>();

    private final DepotClients depot = new DepotClients() {
        @Override
        public Optional<Client> parIdentifiant(String identifiant) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Client> parId(long clientId) {
            return switch ((int) clientId) {
                case 1 -> Optional.of(ACTIF);
                case 5 -> Optional.of(INACTIF);
                default -> Optional.empty();
            };
        }

        @Override
        public void changerActif(long clientId, boolean actif) {
            throw new UnsupportedOperationException();
        }
    };

    private final EmetteurJeton emetteur = client -> {
        emis.add(client);
        return "jeton-neuf-" + client.id();
    };

    private final RenouvelerSessionCasUsage casUsage = new RenouvelerSessionCasUsage(depot, emetteur);

    @Test
    void emet_un_jeton_neuf_pour_un_client_actif() {
        assertThat(casUsage.renouveler(1L)).contains("jeton-neuf-1");
        assertThat(emis).containsExactly(ACTIF);
    }

    @Test
    void n_emet_rien_pour_un_client_desactive_ou_inconnu() {
        assertThat(casUsage.renouveler(5L)).isEmpty();
        assertThat(casUsage.renouveler(99L)).isEmpty();
        assertThat(emis).isEmpty();
    }
}
