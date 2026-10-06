package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.RejeterEcheances;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch.Statut;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.TextesVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cas d'usage planifié : rejette sans débit ni notification les virements suspendus dont l'heure butoir est atteinte
 * (à l'heure butoir exacte, pas avant), y compris la nuit et le week-end. Une ligne {@code SUSPENDED_TRANSFERS_EXPIRY}
 * n'est écrite au journal d'exécution que si au moins un virement est rejeté ou si le traitement échoue.
 */
public class EcheanceRevueCasUsage implements RejeterEcheances {

    private static final Logger JOURNAL = Logger.getLogger(EcheanceRevueCasUsage.class.getName());
    static final String JOB = "SUSPENDED_TRANSFERS_EXPIRY";

    private final DepotVirements virements;
    private final JournalBatch journalBatch;
    private final Horloge horloge;
    private final RejetAutomatique rejetAutomatique;

    public EcheanceRevueCasUsage(DepotVirements virements, JournalAudit audit, JournalBatch journalBatch,
            Transaction transaction, Horloge horloge) {
        this.virements = virements;
        this.journalBatch = journalBatch;
        this.horloge = horloge;
        this.rejetAutomatique = new RejetAutomatique(virements, transaction, horloge, audit);
    }

    @Override
    public int rejeterLesEchus() {
        Instant debut = horloge.instant();
        List<Virement> echus;
        try {
            echus = virements.suspendusEchus(debut);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Lecture des virements suspendus échus en erreur", echec);
            journaliser(debut, 0, Statut.FAILED, TextesVirement.detailsEcheanceEchec(0, 0));
            return 0;
        }
        int rejetes = 0;
        int echecs = 0;
        for (Virement echu : echus) {
            try {
                if (rejetAutomatique.rejeter(echu)) {
                    rejetes++;
                }
            } catch (RuntimeException echec) {
                echecs++;
                JOURNAL.log(Level.SEVERE, "Rejet automatique du virement " + echu.id() + " en erreur", echec);
            }
        }
        if (echecs > 0) {
            journaliser(debut, rejetes, rejetes == 0 ? Statut.FAILED : Statut.PARTIAL,
                    TextesVirement.detailsEcheanceEchec(rejetes, echecs));
        } else if (rejetes > 0) {
            journaliser(debut, rejetes, Statut.SUCCESS, TextesVirement.detailsEcheance(rejetes));
        }
        return rejetes;
    }

    private void journaliser(Instant debut, int rejetes, Statut statut, String details) {
        try {
            journalBatch.enregistrer(JOB, debut, horloge.instant(), rejetes, statut, details);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Ligne du journal d'exécution " + JOB + " non écrite", echec);
        }
    }
}
