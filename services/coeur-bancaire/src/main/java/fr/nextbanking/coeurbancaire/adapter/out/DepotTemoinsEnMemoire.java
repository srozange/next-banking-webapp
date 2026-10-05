package fr.nextbanking.coeurbancaire.adapter.out;

import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant en mémoire du dépôt de témoins (remplacé par la persistance en story 1.4).
 */
@Component
public class DepotTemoinsEnMemoire implements DepotTemoins {

    private final List<Temoin> temoins = new CopyOnWriteArrayList<>();

    @Override
    public void enregistrer(Temoin temoin) {
        temoins.add(temoin);
    }

    public List<Temoin> tous() {
        return List.copyOf(temoins);
    }
}
