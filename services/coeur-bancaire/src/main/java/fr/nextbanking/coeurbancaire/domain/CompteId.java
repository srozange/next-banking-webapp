package fr.nextbanking.coeurbancaire.domain;

/**
 * Identifiant numérique d'un compte : la seule référence à un compte échangée entre services. Il ne porte jamais le
 * numéro de compte.
 */
public record CompteId(long valeur) {

    @Override
    public String toString() {
        return Long.toString(valeur);
    }
}
