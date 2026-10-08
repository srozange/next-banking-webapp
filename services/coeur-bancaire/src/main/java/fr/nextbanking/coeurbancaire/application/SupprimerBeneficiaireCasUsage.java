package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.SupprimerBeneficiaire;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire.Cause;
import java.util.logging.Logger;

/**
 * Cas d'usage : suppression logique d'un bénéficiaire. La propriété est décidée ici (BUG-047) puis la désactivation
 * est une mise à jour conditionnelle : de deux suppressions simultanées, une seule gagne. Le journal ne reçoit que
 * l'IBAN masqué.
 */
public class SupprimerBeneficiaireCasUsage implements SupprimerBeneficiaire {

    private static final Logger JOURNAL = Logger.getLogger(SupprimerBeneficiaireCasUsage.class.getName());

    private final Beneficiaires beneficiaires;

    public SupprimerBeneficiaireCasUsage(Beneficiaires beneficiaires) {
        this.beneficiaires = beneficiaires;
    }

    @Override
    public void supprimer(long clientId, long beneficiaireId) {
        BeneficiaireExterne beneficiaire = beneficiaires.trouverActif(beneficiaireId)
                .orElseThrow(() -> new ErreurBeneficiaire(Cause.INTROUVABLE));
        if (!beneficiaire.appartientA(clientId)) {
            JOURNAL.warning(() -> "Suppression refusée : le bénéficiaire " + beneficiaireId + " vers "
                    + beneficiaire.iban() + " n'est pas celui du client " + clientId);
            throw new ErreurBeneficiaire(Cause.ETRANGER);
        }
        if (!beneficiaires.desactiver(beneficiaireId, clientId)) {
            throw new ErreurBeneficiaire(Cause.INTROUVABLE);
        }
        JOURNAL.info(() -> "Bénéficiaire " + beneficiaireId + " du client " + clientId + " vers "
                + beneficiaire.iban() + " supprimé");
    }
}
