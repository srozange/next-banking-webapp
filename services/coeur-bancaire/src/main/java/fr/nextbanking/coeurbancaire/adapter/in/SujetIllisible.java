package fr.nextbanking.coeurbancaire.adapter.in;

/** Sujet du jeton non numérique : la requête est traitée comme non authentifiée (401). */
class SujetIllisible extends RuntimeException {

    SujetIllisible() {
        super("Sujet du jeton illisible");
    }
}
