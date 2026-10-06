package fr.nextbanking.openbankingconseiller.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.AzureOpenAiSimule;
import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import fr.nextbanking.openbankingconseiller.infrastructure.ApplicationOpenBankingConseiller;
import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Échange avec Sophie sur le service complet, adaptateur simulé par défaut. */
@SpringBootTest(classes = ApplicationOpenBankingConseiller.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ChatIT {

    private static final String CHEMIN = "/api/open-banking-conseiller/chat/messages";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ConversationIa conversation;

    @Test
    @Tag("FR-010-08")
    void sans_propriete_nb_fournisseurs_le_port_est_l_adaptateur_simule() {
        assertThat(conversation).isInstanceOf(AzureOpenAiSimule.class);
    }

    @Test
    @Tag("FR-010-05")
    void un_message_rend_la_reponse_fixe_au_format_json() throws Exception {
        HttpResponse<String> reponse = envoyer("{\"message\":\"Bonjour\",\"historique\":[]}", true);

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.body()).isEqualTo("{\"reponse\":\"Réponse simulée de Sophie.\"}");
    }

    @Test
    @Tag("FR-010-05")
    void un_message_blanc_rend_400() throws Exception {
        HttpResponse<String> reponse = envoyer("{\"message\":\" \"}", true);

        assertThat(reponse.statusCode()).isEqualTo(400);
        assertThat(reponse.body()).isEqualTo("{\"message\":\"Requête invalide\"}");
    }

    @Test
    void sans_jeton_l_acces_est_refuse() throws Exception {
        assertThat(envoyer("{\"message\":\"Bonjour\"}", false).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> envoyer(String corps, boolean avecJeton) throws Exception {
        Instant maintenant = Instant.now();
        String jeton = avecJeton
                ? Jetons.jeton(Conteneurs.CLES, "CLIENT", maintenant, maintenant.plusSeconds(1800)) : null;
        return Jetons.envoyer(port, "POST", CHEMIN, jeton, corps);
    }
}
