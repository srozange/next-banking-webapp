package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.ConnexionRefusee;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SeConnecterCasUsageTest {

    private static final String BON = "bon-mot-de-passe";

    private final Map<String, Client> clients = new HashMap<>();
    private final List<String> comparaisons = new ArrayList<>();
    private final List<Client> jetonsEmis = new ArrayList<>();

    private final DepotClients depot = new DepotClients() {
        @Override
        public Optional<Client> parIdentifiant(String identifiant) {
            return Optional.ofNullable(clients.get(identifiant));
        }

        @Override
        public Optional<Client> parId(long clientId) {
            throw new UnsupportedOperationException();
        }
    };

    // Empreinte de test : "h:" + mot de passe ; une empreinte absente ne correspond jamais.
    private final MotsDePasse motsDePasse = (motDePasse, empreinte) -> {
        comparaisons.add(empreinte.orElse("factice"));
        return empreinte.map(e -> e.equals("h:" + motDePasse)).orElse(false);
    };

    private final EmetteurJeton emetteur = client -> {
        jetonsEmis.add(client);
        return "jeton-" + client.id();
    };

    private final SeConnecterCasUsage casUsage = new SeConnecterCasUsage(depot, motsDePasse, emetteur);

    SeConnecterCasUsageTest() {
        clients.put("user1", new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "h:" + BON));
        clients.put("inactif", new Client(5L, "inactif", "Compte Désactivé", Role.CLIENT, false, "h:" + BON));
    }

    @Test
    @Tag("FR-001-04")
    void connecte_un_client_actif_aux_bons_identifiants() {
        SeConnecter.Connexion connexion = casUsage.seConnecter("user1", BON);

        assertThat(connexion.clientId()).isEqualTo(1L);
        assertThat(connexion.jeton()).isEqualTo("jeton-1");
        assertThat(jetonsEmis).extracting(Client::identifiant).containsExactly("user1");
    }

    @Test
    @Tag("FR-001-04")
    void ne_montre_jamais_le_jeton() {
        assertThat(casUsage.seConnecter("user1", BON).toString()).contains("clientId=1").doesNotContain("jeton-1");
    }

    @Test
    @Tag("FR-001-06")
    void refuse_un_mauvais_mot_de_passe() {
        assertRefus(() -> casUsage.seConnecter("user1", "mauvais"), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
        assertThat(jetonsEmis).isEmpty();
    }

    @Test
    @Tag("FR-001-06")
    void refuse_un_identifiant_inconnu_apres_avoir_quand_meme_compare_le_mot_de_passe() {
        assertRefus(() -> casUsage.seConnecter("retro-compte-inexistant", "retro-mauvais-mot-de-passe"),
                ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
        assertThat(comparaisons).containsExactly("factice");
        assertThat(jetonsEmis).isEmpty();
    }

    @Test
    @Tag("FR-001-07")
    @Tag("BUG-001")
    void refuse_un_compte_desactive_aux_bons_identifiants() {
        assertRefus(() -> casUsage.seConnecter("inactif", BON), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
        assertThat(comparaisons).containsExactly("h:" + BON);
        assertThat(jetonsEmis).isEmpty();
    }

    @Test
    @Tag("FR-001-07")
    @Tag("BUG-001")
    void refuse_un_compte_desactive_au_mauvais_mot_de_passe() {
        assertRefus(() -> casUsage.seConnecter("inactif", "mauvais"), ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
    }

    @Test
    @Tag("FR-001-03")
    void controle_les_champs_requis_avant_toute_recherche() {
        assertRefus(() -> casUsage.seConnecter(" ", BON), ConnexionRefusee.Motif.IDENTIFIANT_REQUIS);
        assertRefus(() -> casUsage.seConnecter("user1", ""), ConnexionRefusee.Motif.MOT_DE_PASSE_REQUIS);
        assertThat(comparaisons).isEmpty();
    }

    private static void assertRefus(Runnable connexion, ConnexionRefusee.Motif motif) {
        assertThatThrownBy(connexion::run).isInstanceOfSatisfying(ConnexionRefusee.class,
                refus -> assertThat(refus.motif()).isEqualTo(motif));
    }
}
