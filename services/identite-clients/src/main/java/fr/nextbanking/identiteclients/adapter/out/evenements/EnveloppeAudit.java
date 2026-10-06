package fr.nextbanking.identiteclients.adapter.out.evenements;

import fr.nextbanking.identiteclients.domain.EntreeAudit;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code identite-clients.audit.v1}
 * (schéma {@code contrats/evenements/identite-clients.audit.v1.schema.json}).
 */
record EnveloppeAudit(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "identite-clients.audit.v1";

    static EnveloppeAudit de(EntreeAudit entree) {
        return new EnveloppeAudit(
                entree.idEvenement().toString(),
                "audit",
                1,
                DateTimeFormatter.ISO_INSTANT.format(entree.horodatage()),
                "identite-clients",
                new Donnees(entree.utilisateur(), entree.action().name(), entree.detail(), entree.adresseIp()));
    }

    /** Données de l'entrée d'audit ; {@code adresseIp} est écrite {@code null} quand elle est absente. */
    record Donnees(String utilisateur, String action, String detail, String adresseIp) {
    }
}
