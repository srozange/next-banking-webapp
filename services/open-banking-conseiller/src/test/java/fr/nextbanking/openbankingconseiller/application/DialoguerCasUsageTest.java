package fr.nextbanking.openbankingconseiller.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.domain.ConsigneSophie;
import fr.nextbanking.openbankingconseiller.domain.Echange;
import fr.nextbanking.openbankingconseiller.domain.RoleEchange;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DialoguerCasUsageTest {

    private final List<List<Echange>> historiques = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private final List<String> consignes = new ArrayList<>();
    private final DialoguerCasUsage cas = new DialoguerCasUsage((consigne, historique, message) -> {
        consignes.add(consigne);
        historiques.add(historique);
        messages.add(message);
        return "réponse";
    });

    @Test
    @Tag("FR-010-05")
    @Tag("FR-010-06")
    void le_premier_message_part_avec_un_historique_vide_puis_avec_les_echanges() {
        assertThat(cas.dialoguer("Bonjour", null)).isEqualTo("réponse");
        List<Echange> echanges = List.of(new Echange(RoleEchange.UTILISATEUR, "Bonjour"),
                new Echange(RoleEchange.ASSISTANT, "réponse"));
        cas.dialoguer("Et les crédits ?", echanges);

        assertThat(historiques).containsExactly(List.of(), echanges);
        assertThat(messages).containsExactly("Bonjour", "Et les crédits ?");
    }

    @Test
    @Tag("FR-010-07")
    void la_consigne_de_sophie_accompagne_chaque_requete() {
        cas.dialoguer("Bonjour", List.of());

        assertThat(consignes).containsExactly(ConsigneSophie.TEXTE);
    }

    @Test
    @Tag("FR-010-05")
    void un_message_vide_ou_blanc_est_refuse_sans_appel() {
        assertThatThrownBy(() -> cas.dialoguer(null, List.of())).isInstanceOf(MessageVide.class);
        assertThatThrownBy(() -> cas.dialoguer("  \n", List.of())).isInstanceOf(MessageVide.class);
        assertThat(messages).isEmpty();
    }

    @Test
    @Tag("FR-010-09")
    void les_natures_d_echec_portent_leur_statut() {
        assertThat(ServiceIaIndisponible.authentification(403).nature())
                .isEqualTo(ServiceIaIndisponible.Nature.AUTHENTIFICATION);
        assertThat(ServiceIaIndisponible.statut(500).statutRecu()).isEqualTo(500);
        assertThat(ServiceIaIndisponible.indisponible("délai").nature())
                .isEqualTo(ServiceIaIndisponible.Nature.INDISPONIBLE);
    }
}
