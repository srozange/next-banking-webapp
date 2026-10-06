package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.domain.DelaiAccesHistorique;
import fr.nextbanking.coeurbancaire.domain.ParametresControleFraude;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
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
            @Value("${nb.fraude.seuil-alerte}") BigDecimal seuilAlerte,
            @Value("${nb.fraude.seuil-fraude}") BigDecimal seuilFraude) {
        return new SeuilsFraude(seuilAttente, seuilAlerte, seuilFraude);
    }

    @Bean
    DelaiAccesHistorique delaiAccesHistorique(@Value("${nb.fraude.acces-historique.delai}") String delai) {
        return new DelaiAccesHistorique(DurationStyle.detectAndParse(delai));
    }

    @Bean
    ParametresControleFraude parametresControleFraude(@Value("${nb.fraude.score-cumule-max}") int scoreCumuleMax,
            @Value("${nb.fraude.montant-eleve}") BigDecimal montantEleve,
            @Value("${nb.fraude.montant-tres-eleve}") BigDecimal montantTresEleve,
            @Value("${nb.fraude.plafond-quotidien}") int plafondQuotidien,
            @Value("${nb.fraude.succession-rapide-secondes}") int successionRapideSecondes,
            @Value("${nb.fraude.velocite-fenetre-minutes}") int velociteFenetreMinutes,
            @Value("${nb.fraude.velocite-nombre-max}") int velociteNombreMax,
            @Value("${nb.fraude.velocite-montant}") BigDecimal velociteMontant) {
        return new ParametresControleFraude(scoreCumuleMax, montantEleve, montantTresEleve, plafondQuotidien,
                successionRapideSecondes, velociteFenetreMinutes, velociteNombreMax, velociteMontant);
    }
}
