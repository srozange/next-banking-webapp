package fr.nextbanking.openbankingconseiller.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.nextbanking.openbankingconseiller.application.MessageVide;
import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import fr.nextbanking.openbankingconseiller.application.port.in.Dialoguer;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import fr.nextbanking.openbankingconseiller.domain.RoleEchange;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Matrice HTTP de l'échange avec Sophie, sans base ni réseau. */
class PointEntreeChatTest {

    private final List<List<Echange>> historiques = new ArrayList<>();
    private RuntimeException echec;
    private final Dialoguer dialoguer = (message, historique) -> {
        if (message == null || message.isBlank()) {
            throw new MessageVide();
        }
        if (echec != null) {
            throw echec;
        }
        historiques.add(historique);
        return "Bonjour " + message;
    };
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PointEntreeChat(dialoguer))
            .setControllerAdvice(new GestionErreurs()).build();

    private ResultActions poster(String corps) throws Exception {
        return mvc.perform(post(PointEntreeChat.CHEMIN).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    @Tag("FR-010-05")
    void un_message_rend_200_et_la_reponse() throws Exception {
        poster("{\"message\":\"x\"}").andExpect(status().isOk()).andExpect(jsonPath("$.reponse").value("Bonjour x"));

        assertThat(historiques).containsExactly(List.of());
    }

    @Test
    @Tag("FR-010-06")
    void l_historique_est_traduit_en_echanges() throws Exception {
        poster("{\"message\":\"x\",\"historique\":[{\"role\":\"utilisateur\",\"contenu\":\"a\"},"
                + "{\"role\":\"assistant\",\"contenu\":\"b\"}]}").andExpect(status().isOk());

        assertThat(historiques).containsExactly(List.of(new Echange(RoleEchange.UTILISATEUR, "a"),
                new Echange(RoleEchange.ASSISTANT, "b")));
    }

    @Test
    @Tag("FR-010-05")
    void un_message_blanc_ou_absent_rend_400() throws Exception {
        for (String corps : new String[] {"{\"message\":\"  \"}", "{}", "{\"message\":null}"}) {
            poster(corps).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Requête invalide"));
        }
    }

    @Test
    void un_historique_mal_forme_ou_un_corps_illisible_rend_400() throws Exception {
        for (String corps : new String[] {
                "{\"message\":\"x\",\"historique\":[{\"role\":\"systeme\",\"contenu\":\"a\"}]}",
                "{\"message\":\"x\",\"historique\":[{\"role\":\"assistant\"}]}", "pas du json"}) {
            poster(corps).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Requête invalide"));
        }
    }

    @Test
    @Tag("FR-010-09")
    @Tag("BUG-038")
    void un_refus_d_authentification_rend_503_sans_code() throws Exception {
        for (int statut : new int[] {401, 403}) {
            echec = ServiceIaIndisponible.authentification(statut);
            String corps = poster("{\"message\":\"x\"}").andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Je suis momentanement indisponible. "
                            + "Veuillez reessayer ou contacter votre conseiller en agence."))
                    .andReturn().getResponse().getContentAsString();
            assertThat(corps).doesNotContain("401", "403");
        }
    }

    @Test
    @Tag("FR-010-09")
    void un_autre_statut_rend_502_avec_ce_statut() throws Exception {
        echec = ServiceIaIndisponible.statut(500);

        poster("{\"message\":\"x\"}").andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Je suis momentanement indisponible (HTTP 500). Veuillez reessayer."));
    }

    @Test
    @Tag("FR-010-09")
    void une_indisponibilite_rend_503_generique() throws Exception {
        echec = ServiceIaIndisponible.indisponible("délai dépassé");

        poster("{\"message\":\"x\"}").andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message")
                .value("Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence."));
    }
}
