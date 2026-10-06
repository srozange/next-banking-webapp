package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.DeposerDemandePret;
import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.DemandePret;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretDemande;
import java.util.UUID;

/**
 * Cas d'usage : valide la demande (FR-007-05 à FR-007-07), enregistre le prêt EN_ATTENTE avec sa ligne d'outbox
 * dans une même transaction, puis publie l'événement après validation. Une panne de la ligne d'outbox annule tout
 * ({@link TransmissionEnEchec}) ; toute autre panne d'enregistrement devient {@link DemandePretEnEchec}.
 */
public class DeposerDemandePretCasUsage implements DeposerDemandePret {

    private final PretRepository prets;
    private final EvenementsSortants evenements;
    private final Transaction transaction;
    private final Horloge horloge;

    public DeposerDemandePretCasUsage(PretRepository prets, EvenementsSortants evenements, Transaction transaction,
            Horloge horloge) {
        this.prets = prets;
        this.evenements = evenements;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public Pret deposer(long clientId, String type, String montant, String tauxAnnuel, String dureeMois,
            String objet) {
        DemandePret demande = DemandePret.lire(type, montant, tauxAnnuel, dureeMois, objet);
        NouveauPret nouveau = NouveauPret.creer(clientId, demande, horloge.aujourdhui());
        Ecriture ecriture;
        try {
            ecriture = transaction.executer(() -> {
                long pretId = prets.creer(nouveau);
                return new Ecriture(nouveau.avecId(pretId), consigner(pretId));
            });
        } catch (TransmissionEnEchec echec) {
            throw echec;
        } catch (RuntimeException panne) {
            throw new DemandePretEnEchec(panne);
        }
        try {
            evenements.publier(ecriture.idLigne());
        } catch (RuntimeException panne) {
            // La demande est transmise dès que la ligne d'outbox est validée : le relais republiera la ligne.
        }
        return ecriture.pret();
    }

    private long consigner(long pretId) {
        try {
            return evenements.consigner(new PretDemande(UUID.randomUUID(), pretId, horloge.instant()));
        } catch (RuntimeException panne) {
            throw new TransmissionEnEchec(panne);
        }
    }

    private record Ecriture(Pret pret, long idLigne) {
    }
}
