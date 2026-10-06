package fr.nextbanking.coeurbancaire.application.port.out;

import java.time.Instant;

/** Port sortant : journal d'exécution des traitements planifiés ({@code BATCH_JOB_LOGS}, NFR-BATCH-02). */
public interface JournalBatch {

    /** Statut d'une exécution. */
    enum Statut {
        SUCCESS, FAILED, PARTIAL
    }

    /** Écrit une ligne : job, début, fin, enregistrements traités, statut et détails. */
    void enregistrer(String job, Instant debut, Instant fin, int enregistrements, Statut statut, String details);
}
