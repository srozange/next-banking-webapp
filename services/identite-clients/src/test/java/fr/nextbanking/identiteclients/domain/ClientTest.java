package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ClientTest {

    private static final String EMPREINTE = "{bcrypt}$2a$10$empreinte";

    @Test
    void porte_le_client_enregistre() {
        Client client = new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, EMPREINTE);

        assertThat(client.id()).isEqualTo(1L);
        assertThat(client.identifiant()).isEqualTo("user1");
        assertThat(client.nomComplet()).isEqualTo("Jean Dupont");
        assertThat(client.role()).isEqualTo(Role.CLIENT);
        assertThat(client.actif()).isTrue();
        assertThat(client.empreinteMotDePasse()).isEqualTo(EMPREINTE);
    }

    @Test
    void refuse_un_identifiant_numerique_nul_ou_negatif() {
        assertThatThrownBy(() -> new Client(0L, "user1", "Jean Dupont", Role.CLIENT, true, EMPREINTE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Client(-1L, "user1", "Jean Dupont", Role.CLIENT, true, EMPREINTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_identifiant_de_connexion_absent_ou_blanc() {
        assertThatThrownBy(() -> new Client(1L, null, "Jean Dupont", Role.CLIENT, true, EMPREINTE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Client(1L, " ", "Jean Dupont", Role.CLIENT, true, EMPREINTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refuse_un_role_absent() {
        assertThatThrownBy(() -> new Client(1L, "user1", "Jean Dupont", null, true, EMPREINTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("BUG-002")
    @Tag("FR-001-08")
    void ne_montre_jamais_l_empreinte_du_mot_de_passe() {
        assertThat(new Client(1L, "user1", "Jean Dupont", Role.ADMIN, false, EMPREINTE).toString())
                .contains("user1", "ADMIN", "actif=false")
                .doesNotContain(EMPREINTE);
    }
}
