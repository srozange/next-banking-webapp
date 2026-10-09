package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Consigne, dans la transaction de l'écriture, un événement {@code compte-modifie} par compte dont le solde vient de
 * changer : l'état relu après le débit ou le crédit, avec sa version d'agrégat.
 */
final class EvenementsComptes {

    private final Comptes comptes;
    private final EvenementsSortants evenements;

    EvenementsComptes(Comptes comptes, EvenementsSortants evenements) {
        this.comptes = comptes;
        this.evenements = evenements;
    }

    /** Lignes d'outbox des comptes donnés, dans l'ordre donné, à publier après validation. */
    List<Long> consigner(Instant horodatage, CompteId... modifies) {
        return Arrays.stream(modifies)
                .map(id -> evenements.consigner(
                        new CompteModifie(UUID.randomUUID(), comptes.etat(id), horodatage)))
                .toList();
    }
}
