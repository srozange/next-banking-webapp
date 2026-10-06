package fr.nextbanking.openbankingconseiller.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Contenu du {@code state} de Powens : client, nonce à usage unique et expiration. Sa forme écrite est
 * {@code <sub en base64url>.<nonce>.<expiration en secondes>} ; la signature est ajoutée par l'émetteur.
 *
 * @param sub        identifiant du client ({@code sub} du jeton d'accès)
 * @param nonce      valeur aléatoire enregistrée, consommée une seule fois
 * @param expiration instant limite de validité
 */
public record StatePowens(String sub, String nonce, Instant expiration) {

    private static final Base64.Encoder ENCODEUR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODEUR = Base64.getUrlDecoder();

    /** Forme écrite, sans signature. */
    public String charge() {
        return ENCODEUR.encodeToString(sub.getBytes(StandardCharsets.UTF_8)) + "." + nonce + "."
                + expiration.getEpochSecond();
    }

    /** Relit une forme écrite ; vide si elle est mal formée. */
    public static Optional<StatePowens> lire(String charge) {
        String[] morceaux = charge.split("\\.", -1);
        if (morceaux.length != 3 || morceaux[1].isEmpty()) {
            return Optional.empty();
        }
        try {
            String sub = new String(DECODEUR.decode(morceaux[0]), StandardCharsets.UTF_8);
            return Optional.of(new StatePowens(sub, morceaux[1], Instant.ofEpochSecond(Long.parseLong(morceaux[2]))));
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            return Optional.empty();
        }
    }

    /** Vrai si l'instant donné est à l'expiration ou après. */
    public boolean estExpire(Instant maintenant) {
        return !maintenant.isBefore(expiration);
    }
}
