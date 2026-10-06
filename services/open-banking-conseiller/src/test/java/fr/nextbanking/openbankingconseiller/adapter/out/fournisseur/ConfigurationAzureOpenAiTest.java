package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Choix de l'adaptateur et validation des réglages au démarrage, sans base ni réseau. */
class ConfigurationAzureOpenAiTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ConfigurationAzureOpenAi.class);

    @Test
    @Tag("FR-010-08")
    void sans_aucune_propriete_nb_fournisseurs_l_adaptateur_simule_est_actif() {
        contexte.run(demarre -> {
            assertThat(demarre).hasNotFailed();
            assertThat(demarre.getBean(ConversationIa.class)).isInstanceOf(AzureOpenAiSimule.class);
        });
    }

    @Test
    void le_mode_reel_active_l_adaptateur_reel() {
        contexte.withPropertyValues("nb.fournisseurs.azure-openai.mode=reel")
                .run(demarre -> assertThat(demarre.getBean(ConversationIa.class)).isInstanceOf(AzureOpenAiReel.class));
    }

    @Test
    void le_refus_simule_se_regle_par_propriete() {
        contexte.withPropertyValues("nb.fournisseurs.azure-openai.refus-simule=401").run(demarre ->
                assertThat(demarre.getBean(ConversationIa.class)).isInstanceOf(AzureOpenAiSimule.class));
    }

    @Test
    void un_mode_inconnu_empeche_le_demarrage() {
        contexte.withPropertyValues("nb.fournisseurs.azure-openai.mode=autre")
                .run(demarre -> assertThat(demarre).hasFailed());
    }
}
