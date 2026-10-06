package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port sortant : état Powens par client et {@code state} à usage unique. */
public interface DepotEtatPowens {

    /** État du client, vide s'il n'a jamais lancé de connexion. */
    Optional<EtatPowens> lire(String sub);

    /** Enregistre le jeton permanent du client ; l'indicateur de connexion n'est pas modifié. */
    void enregistrerJeton(String sub, String jeton);

    /** Enregistre le nonce d'un {@code state} émis, utilisable une seule fois jusqu'à son expiration. */
    void enregistrerState(StatePowens state);

    /**
     * Consomme le nonce du {@code state} de façon atomique : vrai une seule fois, et seulement s'il est enregistré pour ce
     * client et non expiré à {@code maintenant}.
     */
    boolean consommerState(StatePowens state, Instant maintenant);

    /** Efface jeton, comptes, compte sélectionné, transactions, indicateur de connexion et message du client. */
    void remettreAZero(String sub);

    /** Pose le message d'état du client sans toucher au reste. */
    void poserMessage(String sub, String message);

    /**
     * Enregistre la connexion établie : remplace comptes et transactions, marque le client connecté, retient le compte
     * sélectionné (absent s'il n'y a aucun compte) et le message ; le jeton est conservé.
     */
    void enregistrerConnexion(String sub, List<CompteExterne> comptes, Long selectionne,
            List<TransactionExterne> transactions, String message);
}
