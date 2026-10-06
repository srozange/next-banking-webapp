package fr.nextbanking.openbankingconseiller.application;

/** Le compte n'est pas un compte externe du client (inconnu ou d'un autre client). */
public class CompteExterneIntrouvable extends RuntimeException {

    public CompteExterneIntrouvable(long compteId) {
        super("compte externe " + compteId, null, false, false);
    }
}
