package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement interne {@code coeur-bancaire.virement-a-traiter.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.virement-a-traiter.v1.schema.json}).
 */
record EnveloppeVirementATraiter(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.virement-a-traiter.v1";

    static EnveloppeVirementATraiter de(VirementATraiter evenement) {
        return new EnveloppeVirementATraiter(
                evenement.idEvenement().toString(),
                "virement-a-traiter",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(evenement.virementId(), evenement.montant().texte(), evenement.emetteurId(),
                        evenement.compteDestinataireId().valeur(), evenement.motif().texte()));
    }

    /** Données de l'événement ; le montant est une chaîne décimale à point, 2 décimales. */
    record Donnees(long virementId, String montant, long emetteurId, long compteDestinataireId, String motif) {
    }
}
