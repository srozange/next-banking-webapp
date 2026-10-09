package fr.nextbanking.coeurbancaire.domain;

import java.util.List;

/** Client du portefeuille d'un conseiller et ses comptes, triés par identifiant (liste vide s'il n'en a pas). */
public record ClientEtSesComptes(long clientId, List<Compte> comptes) {

    public ClientEtSesComptes {
        comptes = List.copyOf(comptes);
    }
}
