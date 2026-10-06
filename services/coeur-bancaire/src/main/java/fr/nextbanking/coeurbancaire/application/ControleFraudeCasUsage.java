package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.ControleFraude;
import fr.nextbanking.coeurbancaire.domain.ControleFraude.Evaluation;
import fr.nextbanking.coeurbancaire.domain.HistoriqueClient;
import fr.nextbanking.coeurbancaire.domain.Iban;
import fr.nextbanking.coeurbancaire.domain.ParametresControleFraude;
import fr.nextbanking.coeurbancaire.domain.Signal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôle de fraude d'un virement, appelé par le cas d'usage de virement dans la transaction de soumission : prend le
 * verrou du client, lit l'historique par le seul port {@code HistoriqueVirements}, évalue les huit signaux dans le
 * domaine, puis écrit les alertes. Aucun appel réseau ; aucun point d'entrée public (RP-08). Le journal ne reçoit que
 * l'{@link Iban} masqué.
 */
public class ControleFraudeCasUsage {

    private static final Logger JOURNAL = Logger.getLogger(ControleFraudeCasUsage.class.getName());

    private final HistoriqueVirements historique;
    private final Beneficiaires beneficiaires;
    private final AlertesFraude alertes;
    private final Horloge horloge;
    private final ParametresControleFraude parametres;

    public ControleFraudeCasUsage(HistoriqueVirements historique, Beneficiaires beneficiaires, AlertesFraude alertes,
            Horloge horloge, ParametresControleFraude parametres) {
        this.historique = historique;
        this.beneficiaires = beneficiaires;
        this.alertes = alertes;
        this.horloge = horloge;
        this.parametres = parametres;
    }

    /**
     * Évalue le virement. Une erreur d'accès à l'historique est journalisée puis relancée : elle fait échouer la
     * transaction (le refus pour erreur de contrôle est la story 3.4).
     */
    Evaluation evaluer(long clientId, BigDecimal montant, String numeroDestinataire, Instant soumisLe) {
        Iban destinataire = new Iban(numeroDestinataire);
        try {
            historique.verrouillerClient(clientId);
            Instant debutDuJour = horloge.aujourdhui().atStartOfDay(horloge.fuseau()).toInstant();
            HistoriqueClient lu = new HistoriqueClient(
                    historique.operationsDepuis(clientId, debutDuJour),
                    historique.dernierVirement(clientId).orElse(null),
                    historique.virementsExecutesAuDessus(clientId, parametres.velociteMontant(),
                            soumisLe.minusSeconds(parametres.velociteFenetreMinutes() * 60L)));
            Evaluation evaluation = ControleFraude.evaluer(parametres, montant, destinataire,
                    beneficiaires.estActif(clientId, numeroDestinataire), lu, soumisLe, horloge.fuseau());
            JOURNAL.info(() -> "Contrôle de fraude du virement du client " + clientId + " vers " + destinataire
                    + " : score " + evaluation.score() + ", signaux "
                    + evaluation.signaux().stream().map(Signal::type).toList());
            return evaluation;
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Contrôle de fraude du virement du client " + clientId + " vers "
                    + destinataire + " en erreur", echec);
            throw echec;
        }
    }

    /** Écrit une alerte OPEN par signal levé si la somme des scores dépasse le seuil ; le virement est déjà enregistré. */
    void enregistrerAlertes(Evaluation evaluation, long clientId, long virementId, Instant creeLe) {
        List<AlerteFraude> aEcrire = evaluation.alertes(clientId, virementId, creeLe);
        aEcrire.forEach(alertes::enregistrer);
    }
}
