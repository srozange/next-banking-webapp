package fr.nextbanking.audit.adapter.out.persistance;

import fr.nextbanking.audit.application.port.out.JournalAudit;
import fr.nextbanking.audit.domain.EntreeAudit;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code audit_logs}, en insertion seule.
 */
@Component
public class JournalAuditJdbc implements JournalAudit {

    private final JdbcClient jdbc;

    public JournalAuditJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enregistrer(EntreeAudit entree) {
        jdbc.sql("""
                insert into audit_logs (action, action_date, details, ipaddress, username)
                values (:action, :actionDate, :details, :ipaddress, :username)""")
                .param("action", entree.action().name())
                .param("actionDate", OffsetDateTime.ofInstant(entree.horodatage(), ZoneOffset.UTC))
                .param("details", entree.detail())
                .param("ipaddress", entree.adresseIp())
                .param("username", entree.utilisateur())
                .update();
    }
}
