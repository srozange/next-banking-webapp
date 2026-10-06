package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.audit.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.audit.v1.schema.json}).
 */
record EnveloppeAudit(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.audit.v1";

    static EnveloppeAudit de(EntreeAudit entree) {
        return new EnveloppeAudit(
                entree.idEvenement().toString(),
                "audit",
                1,
                DateTimeFormatter.ISO_INSTANT.format(entree.horodatage()),
                "coeur-bancaire",
                new Donnees(entree.utilisateur(), entree.action().name(), entree.detail(), entree.adresseIp()));
    }

    /** Données de l'entrée d'audit ; {@code adresseIp} est écrite {@code null} quand elle est absente. */
    record Donnees(String utilisateur, String action, String detail, String adresseIp) {
    }
}
