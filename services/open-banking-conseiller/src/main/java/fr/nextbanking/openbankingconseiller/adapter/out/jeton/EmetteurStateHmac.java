package fr.nextbanking.openbankingconseiller.adapter.out.jeton;

import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Signature HMAC-SHA256 du {@code state} de Powens : {@code <contenu>.<signature>}. La clé est dérivée du secret
 * client ({@code POWENS_CLIENT_SECRET}, environnement puis propriété système, jamais versionné) ou, sans secret (mode
 * simulé), tirée au hasard au démarrage du processus.
 */
@Component
public class EmetteurStateHmac implements EmetteurState {

    private static final String ALGORITHME = "HmacSHA256";
    private static final String SECRET = "POWENS_CLIENT_SECRET";

    private final SecretKeySpec cle;

    /** Clé dérivée du secret client des paramètres de déploiement du processus, sinon aléatoire. */
    @Autowired
    public EmetteurStateHmac() {
        this(System::getenv, System::getProperty);
    }

    /** Sources injectables (tests). */
    EmetteurStateHmac(UnaryOperator<String> environnement, UnaryOperator<String> proprietesSysteme) {
        this(secret(environnement, proprietesSysteme).map(EmetteurStateHmac::deriver)
                .orElseGet(EmetteurStateHmac::aleatoire));
    }

    public EmetteurStateHmac(byte[] cle) {
        this.cle = new SecretKeySpec(cle, ALGORITHME);
    }

    @Override
    public String emettre(StatePowens state) {
        String charge = state.charge();
        return charge + "." + signer(charge);
    }

    @Override
    public Optional<StatePowens> verifier(String texte) {
        int separateur = texte.lastIndexOf('.');
        if (separateur < 0) {
            return Optional.empty();
        }
        String charge = texte.substring(0, separateur);
        byte[] attendue = signer(charge).getBytes(StandardCharsets.UTF_8);
        byte[] recue = texte.substring(separateur + 1).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(attendue, recue) ? StatePowens.lire(charge) : Optional.empty();
    }

    private static Optional<String> secret(UnaryOperator<String> environnement,
            UnaryOperator<String> proprietesSysteme) {
        return Stream.of(environnement.apply(SECRET), proprietesSysteme.apply(SECRET))
                .filter(valeur -> valeur != null && !valeur.isBlank()).map(String::trim).findFirst();
    }

    private static byte[] deriver(String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHME));
            return mac.doFinal("state-powens".getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Dérivation de la clé du state impossible", e);
        }
    }

    private static byte[] aleatoire() {
        byte[] cle = new byte[32];
        new SecureRandom().nextBytes(cle);
        return cle;
    }

    private String signer(String charge) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(cle);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(charge.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signature du state impossible", e);
        }
    }
}
