package fr.nextbanking.audit.infrastructure;

import fr.nextbanking.audit.application.EnregistrerEntreeAuditCasUsage;
import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit;
import fr.nextbanking.audit.application.port.out.EvenementsTraites;
import fr.nextbanking.audit.application.port.out.JournalAudit;
import fr.nextbanking.audit.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble le cas d'usage d'enregistrement des entrées d'audit, écrit en Java pur.
 */
@Configuration
public class AssemblageEnregistrementAudit {

    @Bean
    EnregistrerEntreeAudit enregistrerEntreeAudit(JournalAudit journalAudit, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        return new EnregistrerEntreeAuditCasUsage(journalAudit, evenementsTraites, transaction);
    }
}
