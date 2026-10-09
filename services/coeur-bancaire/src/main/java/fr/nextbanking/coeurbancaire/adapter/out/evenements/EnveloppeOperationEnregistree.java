package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.OperationEnregistree;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.operation-enregistree.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json}).
 */
record EnveloppeOperationEnregistree(String idEvenement, String type, int version, String horodatage,
        String producteur, Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.operation-enregistree.v1";

    static EnveloppeOperationEnregistree de(OperationEnregistree evenement) {
        Operation operation = evenement.operation();
        return new EnveloppeOperationEnregistree(
                evenement.idEvenement().toString(),
                "operation-enregistree",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(evenement.operationId(), operation.compteId(),
                        DateTimeFormatter.ISO_INSTANT.format(operation.date()), operation.description(),
                        operation.type().name(), operation.montant().texte(), operation.soldeApres().texte(),
                        evenement.versionAgregat()));
    }

    /** Une opération ; montant et solde après sont des chaînes à deux décimales. */
    record Donnees(long operationId, long compteId, String instant, String libelle, String type, String montant,
            String soldeApres, long versionAgregat) {
    }
}
