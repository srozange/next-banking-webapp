package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seuils {@code nb.fraude.*} lus dans la configuration de déploiement et validés au démarrage par le domaine
 * ({@link SeuilsFraude}) : un seuil absent, illisible, nul ou négatif empêche le service de démarrer (NFR-SEC-08).
 */
@Configuration
public class ConfigurationFraude {

    @Bean
    SeuilsFraude seuilsFraude(@Value("${nb.fraude.seuil-attente}") BigDecimal seuilAttente,
            @Value("${nb.fraude.seuil-alerte}") BigDecimal seuilAlerte) {
        return new SeuilsFraude(seuilAttente, seuilAlerte);
    }
}
