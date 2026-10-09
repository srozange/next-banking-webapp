package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import java.time.format.DateTimeFormatter;

/**
 * Enveloppe JSON de l'événement {@code coeur-bancaire.compte-modifie.v1}
 * (schéma {@code contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json}).
 */
record EnveloppeCompteModifie(String idEvenement, String type, int version, String horodatage, String producteur,
        Donnees donnees) {

    static final String TOPIC = "coeur-bancaire.compte-modifie.v1";

    /** BIC unique de la banque de démonstration (valeur du legacy). */
    static final String BIC = "DEMOFR21XXX";

    static EnveloppeCompteModifie de(CompteModifie evenement) {
        Compte compte = evenement.etat().compte();
        return new EnveloppeCompteModifie(
                evenement.idEvenement().toString(),
                "compte-modifie",
                1,
                DateTimeFormatter.ISO_INSTANT.format(evenement.horodatage()),
                "coeur-bancaire",
                new Donnees(compte.id(), compte.clientId(), compte.numero(), compte.type().name(), compte.numero(), BIC,
                        compte.solde().texte(), evenement.etat().versionAgregat()));
    }

    /** État complet du compte ; l'IBAN est le numéro du compte, le solde une chaîne à deux décimales. */
    record Donnees(long compteId, long clientId, String numero, String type, String iban, String bic, String solde,
            long versionAgregat) {
    }
}
