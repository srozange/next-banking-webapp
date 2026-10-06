package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.port.out.ConversationIa;
import java.net.http.HttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Choisit l'adaptateur du port {@link ConversationIa} selon {@code nb.fournisseurs.azure-openai.mode} : simulé sans
 * aucun réglage, réel sur {@code reel}. Le client HTTP réel applique les délais de connexion et de lecture ; clé,
 * endpoint, déploiement et version viennent de {@link ParametresDeploiementAzure}.
 */
@Configuration
@EnableConfigurationProperties(ProprietesAzureOpenAi.class)
public class ConfigurationAzureOpenAi {

    @Bean
    ParametresDeploiementAzure parametresDeploiementAzure() {
        return new ParametresDeploiementAzure();
    }

    @Bean
    ConversationIa conversationIa(ProprietesAzureOpenAi proprietes, ParametresDeploiementAzure parametres,
            ObjectProvider<RestClient.Builder> constructeur) {
        if (!proprietes.reel()) {
            return new AzureOpenAiSimule(proprietes.refusSimule());
        }
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(proprietes.delaiConnexion()).build());
        fabrique.setReadTimeout(proprietes.delaiLecture());
        RestClient client = constructeur.getIfAvailable(RestClient::builder).requestFactory(fabrique).build();
        return new AzureOpenAiReel(client, parametres, proprietes);
    }
}
