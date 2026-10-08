package fr.nextbanking.credits.adapter.out.evenements;

import fr.nextbanking.credits.domain.EntreeAudit;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code credits.audit.v1}
 * (schéma {@code contrats/evenements/credits.audit.v1.schema.json}).
 */
record EnveloppeAudit(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "credits.audit.v1";

    static EnveloppeAudit de(EntreeAudit entree) {
        return new EnveloppeAudit(
                entree.idEvenement().toString(),
                "audit",
                1,
                DateTimeFormatter.ISO_INSTANT.format(entree.horodatage()),
                "credits",
                new Donnees(entree.utilisateur(), entree.action().name(), entree.detail(), entree.adresseIp()));
    }

    /** Données de l'entrée d'audit. */
    record Donnees(String utilisateur, String action, String detail, String adresseIp) {
    }
}
