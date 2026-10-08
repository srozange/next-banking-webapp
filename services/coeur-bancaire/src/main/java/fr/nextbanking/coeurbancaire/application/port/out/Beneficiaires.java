package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import java.util.List;
import java.util.Optional;

/** Port sortant : bénéficiaires externes du client. */
public interface Beneficiaires {

    /** Vrai si le numéro est celui d'un bénéficiaire actif du client. */
    boolean estActif(long clientId, String numero);

    /** Bénéficiaires actifs du client. */
    List<BeneficiaireExterne> actifsDuClient(long clientId);

    /** Bénéficiaire actif d'identifiant donné, quel que soit son client ; vide s'il est absent ou désactivé. */
    Optional<BeneficiaireExterne> trouverActif(long beneficiaireId);

    /**
     * Désactive le bénéficiaire par mise à jour conditionnelle (actif du client) : vrai pour la seule transition
     * actif vers inactif, faux si un autre traitement a déjà désactivé la ligne. La ligne est conservée.
     */
    boolean desactiver(long beneficiaireId, long clientId);
}
