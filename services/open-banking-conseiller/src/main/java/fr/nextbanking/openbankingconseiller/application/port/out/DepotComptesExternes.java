package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.util.List;
import java.util.Optional;

/** Port sortant : comptes externes, compte sélectionné et transactions d'un client. */
public interface DepotComptesExternes {

    /** Comptes externes enregistrés du client. */
    List<CompteExterne> comptes(String sub);

    /** Compte sélectionné du client, absent s'il n'y en a pas. */
    Optional<Long> compteSelectionne(String sub);

    /** Transactions enregistrées du compte sélectionné. */
    List<TransactionExterne> transactions(String sub);

    /** Vrai si le compte est un compte externe de ce client (jamais celui d'un autre). */
    boolean appartient(String sub, long compteId);

    /** Retient le compte sélectionné et remplace les transactions enregistrées ; le message n'est pas modifié. */
    void enregistrerSelection(String sub, long compteId, List<TransactionExterne> transactions);
}
