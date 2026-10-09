package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.EtatCompte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : comptes bancaires. Seul le cœur bancaire modifie un solde.
 */
public interface Comptes {

    /** Comptes du client, par identifiant. */
    List<Compte> duClient(long clientId);

    /** Comptes des clients demandés, en une lecture, triés par client puis par identifiant de compte. */
    List<Compte> desClients(Collection<Long> clientIds);

    /** Compte d'identifiant donné, s'il existe. */
    Optional<Compte> trouver(long compteId);

    /** Compte de numéro donné (le numéro est unique), s'il existe. */
    Optional<Compte> parNumero(String numero);

    /**
     * Lit les comptes demandés qui existent et les verrouille jusqu'à la fin de la transaction courante, dans l'ordre
     * de leur identifiant (pas d'interblocage entre deux virements croisés).
     */
    List<Compte> verrouiller(long premierId, long secondId);

    /** Débite le compte si, et seulement si, son solde couvre le montant (mise à jour conditionnelle) ; faux sinon. */
    boolean debiter(CompteId compteId, Montant montant);

    /** Crédite le compte. */
    void crediter(CompteId compteId, Montant montant);

    /**
     * Relit un compte existant, avec sa version d'agrégat (incrémentée à chaque débit et crédit), dans la transaction
     * courante : appelée après une écriture sur un compte verrouillé.
     */
    EtatCompte etat(CompteId compteId);
}
