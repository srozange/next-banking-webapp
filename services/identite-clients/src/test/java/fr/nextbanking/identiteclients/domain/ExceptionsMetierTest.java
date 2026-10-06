package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-030-07")
class ExceptionsMetierTest {

    @Test
    void les_messages_ne_portent_aucun_identifiant() {
        assertThat(new AccesRefuse()).hasMessage("Accès refusé : rôle insuffisant");
        assertThat(new ClientIntrouvable()).hasMessage("Client introuvable");
    }
}
