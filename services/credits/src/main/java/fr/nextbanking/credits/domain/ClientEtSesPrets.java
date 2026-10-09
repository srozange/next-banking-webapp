package fr.nextbanking.credits.domain;

import java.util.List;

/** Client du portefeuille d'un conseiller et ses prêts, date de début décroissante (liste vide s'il n'en a pas). */
public record ClientEtSesPrets(long clientId, List<Pret> prets) {

    public ClientEtSesPrets {
        prets = List.copyOf(prets);
    }
}
