package fr.nextbanking.openbankingconseiller.infrastructure;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Écouteur de test ajouté aux sources par {@code --spring.main.sources} (sans annotation, donc hors du scan) : garde le contexte démarré par {@code main}
 * pour que le test le ferme.
 */
public class ContexteDemarre implements ApplicationListener<ApplicationReadyEvent> {

    private static final AtomicReference<ConfigurableApplicationContext> DERNIER = new AtomicReference<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent evenement) {
        DERNIER.set(evenement.getApplicationContext());
    }

    static ConfigurableApplicationContext retirer() {
        return DERNIER.getAndSet(null);
    }
}
