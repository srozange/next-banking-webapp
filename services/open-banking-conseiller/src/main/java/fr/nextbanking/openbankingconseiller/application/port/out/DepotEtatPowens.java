package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.util.Optional;

/** Port sortant : état Powens par client et {@code state} à usage unique. */
public interface DepotEtatPowens {

    /** État du client, vide s'il n'a jamais lancé de connexion. */
    Optional<EtatPowens> lire(String sub);

    /** Enregistre le jeton permanent du client ; l'indicateur de connexion n'est pas modifié. */
    void enregistrerJeton(String sub, String jeton);

    /** Enregistre le nonce d'un {@code state} émis, utilisable une seule fois jusqu'à son expiration. */
    void enregistrerState(StatePowens state);
}
