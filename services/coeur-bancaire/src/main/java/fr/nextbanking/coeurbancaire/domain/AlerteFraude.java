package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/** Alerte de fraude écrite pour un signal d'un virement dont le score cumulé dépasse le seuil. */
public record AlerteFraude(long clientId, long virementId, TypeSignal regle, int score, String description,
        StatutAlerte statut, Instant creeLe) {

    public AlerteFraude {
        if (regle == null || description == null || statut == null || creeLe == null) {
            throw new IllegalArgumentException("Une alerte porte une règle, une description, un statut et une date.");
        }
    }
}
