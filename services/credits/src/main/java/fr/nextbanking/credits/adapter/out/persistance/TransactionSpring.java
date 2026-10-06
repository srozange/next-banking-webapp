package fr.nextbanking.credits.adapter.out.persistance;

import fr.nextbanking.credits.application.port.out.Transaction;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Adaptateur sortant : transactions JDBC par {@link TransactionTemplate}.
 */
@Component
public class TransactionSpring implements Transaction {

    private final TransactionTemplate courante;
    private final TransactionTemplate independante;

    public TransactionSpring(PlatformTransactionManager gestionnaire) {
        this.courante = new TransactionTemplate(gestionnaire);
        this.independante = new TransactionTemplate(gestionnaire);
        this.independante.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T executer(Supplier<T> travail) {
        return courante.execute(statut -> travail.get());
    }

    @Override
    public <T> T executerIndependamment(Supplier<T> travail) {
        return independante.execute(statut -> travail.get());
    }
}
