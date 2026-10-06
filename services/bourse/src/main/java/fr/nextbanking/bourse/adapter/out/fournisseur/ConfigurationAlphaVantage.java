package fr.nextbanking.bourse.adapter.out.fournisseur;

import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import java.net.http.HttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Choisit l'adaptateur du port {@link Cotations} selon {@code nb.fournisseurs.alphavantage.mode} : simulé sans aucun
 * réglage, réel sur {@code reel}. Le client HTTP du mode réel applique le délai d'attente à la connexion et à la
 * lecture ; clé et adresse viennent de {@link ParametresDeploiement}, lus au démarrage.
 */
@Configuration
@EnableConfigurationProperties(ProprietesAlphaVantage.class)
public class ConfigurationAlphaVantage {

    @Bean
    ParametresDeploiement parametresDeploiement() {
        return new ParametresDeploiement();
    }

    @Bean
    Cotations cotations(ProprietesAlphaVantage proprietes, Horloge horloge, ParametresDeploiement parametres,
            ObjectProvider<RestClient.Builder> constructeur) {
        if (!proprietes.reel()) {
            return new AlphaVantageSimule(horloge);
        }
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(proprietes.delaiAttente()).build());
        fabrique.setReadTimeout(proprietes.delaiAttente());
        RestClient client = constructeur.getIfAvailable(RestClient::builder).requestFactory(fabrique).build();
        return new AlphaVantageCotations(client, parametres.adresse(), parametres.cle(), proprietes);
    }
}
