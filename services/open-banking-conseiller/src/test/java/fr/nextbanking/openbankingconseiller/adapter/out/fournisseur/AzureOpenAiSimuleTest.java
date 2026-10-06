package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.application.ServiceIaIndisponible;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class AzureOpenAiSimuleTest {

    @Test
    @Tag("FR-010-05")
    void la_reponse_est_fixe() {
        AzureOpenAiSimule simule = new AzureOpenAiSimule(null);

        assertThat(simule.repondre("consigne", List.of(), "a")).isEqualTo(AzureOpenAiSimule.REPONSE);
        assertThat(simule.repondre("consigne", List.of(), "b")).isEqualTo(AzureOpenAiSimule.REPONSE);
    }

    @Test
    @Tag("FR-010-09")
    @Tag("BUG-038")
    void le_refus_simule_est_une_erreur_d_authentification() {
        for (int statut : new int[] {401, 403}) {
            assertThatThrownBy(() -> new AzureOpenAiSimule(statut).repondre("c", List.of(), "a"))
                    .isInstanceOfSatisfying(ServiceIaIndisponible.class, e -> {
                        assertThat(e.nature()).isEqualTo(ServiceIaIndisponible.Nature.AUTHENTIFICATION);
                        assertThat(e.statutRecu()).isEqualTo(statut);
                    });
        }
    }
}
