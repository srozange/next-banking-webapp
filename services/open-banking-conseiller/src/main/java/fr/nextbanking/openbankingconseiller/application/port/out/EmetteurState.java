package fr.nextbanking.openbankingconseiller.application.port.out;

import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.util.Optional;

/** Port sortant : signature et vérification du {@code state} de Powens. */
public interface EmetteurState {

    /** Forme signée du {@code state}. */
    String emettre(StatePowens state);

    /** Contenu d'un {@code state} dont la signature est valide, vide sinon. */
    Optional<StatePowens> verifier(String texte);
}
