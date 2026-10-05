package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.EcrireTemoin;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import org.springframework.stereotype.Component;

/**
 * Adaptateur entrant : point d'entrée de l'écriture d'un témoin (exposé sur le web en story 1.4).
 */
@Component
public class PointEntreeTemoin {

    private final EcrireTemoin ecrireTemoin;

    public PointEntreeTemoin(EcrireTemoin ecrireTemoin) {
        this.ecrireTemoin = ecrireTemoin;
    }

    public Temoin ecrire(String libelle) {
        return ecrireTemoin.ecrire(libelle);
    }
}
