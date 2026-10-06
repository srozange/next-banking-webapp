package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.Compte;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : comptes bancaires. Seul le cœur bancaire modifie un solde.
 */
public interface Comptes {

    /** Comptes du client, par identifiant. */
    List<Compte> duClient(long clientId);

    /** Compte d'identifiant donné, s'il existe. */
    Optional<Compte> trouver(long compteId);

    /**
     * Lit les comptes demandés qui existent et les verrouille jusqu'à la fin de la transaction courante, dans l'ordre
     * de leur identifiant (pas d'interblocage entre deux virements croisés).
     */
    List<Compte> verrouiller(long premierId, long secondId);

    /** Débite le compte si, et seulement si, son solde couvre le montant (mise à jour conditionnelle) ; faux sinon. */
    boolean debiter(long compteId, BigDecimal montant);

    /** Crédite le compte. */
    void crediter(long compteId, BigDecimal montant);
}
