package fr.nextbanking.coeurbancaire.domain;

/** Signal de fraude levé : son type, son score de risque et sa description. */
public record Signal(TypeSignal type, int score, String description) {

    public Signal {
        if (type == null || description == null) {
            throw new IllegalArgumentException("Un signal porte un type et une description.");
        }
    }
}
