package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Refus d'authentification simulé : le client n'en voit jamais le code (BUG-038). */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nb.fournisseurs.azure-openai.refus-simule=403")
@DirtiesContext
class ChatRefusIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Test
    @Tag("FR-010-09")
    @Tag("BUG-038")
    void un_refus_simule_rend_503_sans_code_technique() throws Exception {
        Instant maintenant = Instant.now();
        String jeton = Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant, maintenant.plusSeconds(1800));

        HttpResponse<String> reponse = Jetons.envoyer(port, "POST", "/api/open-banking-conseiller/chat/messages",
                jeton, "{\"message\":\"Bonjour\"}");

        assertThat(reponse.statusCode()).isEqualTo(503);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Je suis momentanement indisponible. "
                + "Veuillez reessayer ou contacter votre conseiller en agence.\"}").doesNotContain("403", "401");
    }
}
