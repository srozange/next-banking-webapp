package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Choisit l'adaptateur du port {@link Powens} selon {@code nb.fournisseurs.powens.mode} : simulé sans aucun réglage,
 * réel sur {@code reel}. Le mode réel exige le secret client (paramètre de déploiement) : sans lui, le contexte ne
 * démarre pas. Le client HTTP applique le délai d'attente à la connexion et à la lecture.
 */
@Configuration
@EnableConfigurationProperties(ProprietesPowens.class)
public class ConfigurationPowens {

    @Bean
    ParametresDeploiement parametresDeploiementPowens() {
        return new ParametresDeploiement();
    }

    @Bean
    Powens powens(ProprietesPowens proprietes, ParametresDeploiement parametres,
            ObjectProvider<RestClient.Builder> constructeur) {
        String retour = parametres.urlRetour().orElse(PowensSimule.RETOUR_PAR_DEFAUT);
        if (!proprietes.reel()) {
            return new PowensSimule(retour);
        }
        String secret = parametres.secret().orElseThrow(() -> new IllegalArgumentException(
                "Le mode reel de Powens exige le secret client (variable d'environnement "
                        + ParametresDeploiement.SECRET + ")."));
        String domaine = parametres.domaine().orElse(proprietes.domaine());
        String clientId = parametres.clientId().orElse(proprietes.clientId());
        String banques = parametres.banques().orElse(proprietes.banques());
        RestClient client = constructeur.getIfAvailable(RestClient::builder)
                .requestFactory(fabrique(proprietes.delaiAttente())).build();
        return new PowensReel(client, "https://" + domaine + "/2.0", domaine, clientId, secret, retour, banques,
                proprietes);
    }

    /** Délai de connexion et délai de lecture identiques. */
    static JdkClientHttpRequestFactory fabrique(Duration delai) {
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(delai).build());
        fabrique.setReadTimeout(delai);
        return fabrique;
    }
}
