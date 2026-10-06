package fr.nextbanking.openbankingconseiller.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConversationTest {

    @Test
    @Tag("FR-010-07")
    void la_consigne_est_le_texte_valide_de_sophie() {
        assertThat(ConsigneSophie.TEXTE).startsWith("Tu es Sophie, conseillère de Demo Bank.")
                .contains("six domaines", "jamais de conseil d'investissement ferme", "les placements comportent des risques",
                        "exclusivement en français", "3 à 5 phrases");
    }

    @Test
    void un_echange_porte_un_role_et_un_contenu() {
        assertThat(new Echange(RoleEchange.ASSISTANT, "Bonjour").contenu()).isEqualTo("Bonjour");
        assertThatThrownBy(() -> new Echange(null, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Echange(RoleEchange.UTILISATEUR, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
