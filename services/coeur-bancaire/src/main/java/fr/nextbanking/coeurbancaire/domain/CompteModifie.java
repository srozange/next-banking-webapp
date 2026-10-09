package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement d'un compte modifié : l'état complet du compte après l'écriture et sa {@code versionAgregat}, strictement
 * croissante. Topic {@code coeur-bancaire.compte-modifie.v1}, clé {@code compteId}.
 */
public record CompteModifie(UUID idEvenement, EtatCompte etat, Instant horodatage) {

    public CompteModifie {
        if (idEvenement == null || etat == null || horodatage == null) {
            throw new IllegalArgumentException("L'identifiant, l'état du compte et l'horodatage sont obligatoires.");
        }
    }
}
