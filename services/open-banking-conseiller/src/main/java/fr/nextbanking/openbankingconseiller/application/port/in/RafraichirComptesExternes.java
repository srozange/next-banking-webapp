package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.domain.SituationPowens;

/** Cas d'usage : relire les comptes chez Powens (FEAT-012). */
public interface RafraichirComptesExternes {

    /**
     * @throws fr.nextbanking.openbankingconseiller.application.PowensNonConnecte si le client n'a pas de jeton
     * @throws fr.nextbanking.openbankingconseiller.application.PowensIndisponible si Powens est en panne (rien n'est écrit)
     */
    SituationPowens rafraichir(String sub);
}
