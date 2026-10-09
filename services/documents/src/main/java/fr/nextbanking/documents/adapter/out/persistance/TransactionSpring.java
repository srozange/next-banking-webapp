package fr.nextbanking.documents.adapter.out.persistance;

import fr.nextbanking.documents.application.port.out.Transaction;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Adaptateur sortant : transactions JDBC par {@link TransactionTemplate}.
 */
@Component
public class TransactionSpring implements Transaction {

    private final TransactionTemplate courante;

    public TransactionSpring(PlatformTransactionManager gestionnaire) {
        this.courante = new TransactionTemplate(gestionnaire);
    }

    @Override
    public <T> T executer(Supplier<T> travail) {
        return courante.execute(statut -> travail.get());
    }
}
