package fr.nextbanking.documents.domain;

import java.util.List;

/** Relevé mensuel d'un compte : son type, la période et les lignes du mois, par instant croissant. */
public record Releve(TypeCompte typeCompte, int annee, int mois, List<LigneReleve> lignes) {

    public Releve {
        lignes = List.copyOf(lignes);
    }
}
