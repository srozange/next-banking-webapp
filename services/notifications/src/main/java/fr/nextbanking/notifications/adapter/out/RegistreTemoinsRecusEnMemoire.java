package fr.nextbanking.notifications.adapter.out;

import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.domain.TemoinRecu;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant en mémoire du registre des témoins reçus (remplacé par la persistance en story 1.4).
 */
@Component
public class RegistreTemoinsRecusEnMemoire implements RegistreTemoinsRecus {

    private final List<TemoinRecu> temoins = new CopyOnWriteArrayList<>();

    @Override
    public void enregistrer(TemoinRecu temoin) {
        temoins.add(temoin);
    }

    public List<TemoinRecu> tous() {
        return List.copyOf(temoins);
    }
}
