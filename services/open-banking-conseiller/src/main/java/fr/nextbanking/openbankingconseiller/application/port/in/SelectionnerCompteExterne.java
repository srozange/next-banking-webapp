package fr.nextbanking.openbankingconseiller.application.port.in;

import fr.nextbanking.openbankingconseiller.domain.SituationPowens;

/** Cas d'usage : sélectionner un compte externe et charger ses transactions (FEAT-014). */
public interface SelectionnerCompteExterne {

    /**
     * @throws fr.nextbanking.openbankingconseiller.application.CompteExterneIntrouvable si le compte n'est pas au client
     * @throws fr.nextbanking.openbankingconseiller.application.PowensIndisponible        si Powens est en panne
     */
    SituationPowens selectionner(String sub, long compteId);
}
