package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Paramètres de déploiement de Powens, hors des fichiers de configuration : variable d'environnement, puis propriété
 * système ; une valeur blanche vaut absence (NFR-SEC-06). Le secret client n'a aucune valeur par défaut ; il n'est
 * jamais une propriété Spring ni n'apparaît dans un journal. Domaine, identifiant client et banques, s'ils sont
 * donnés, l'emportent sur les réglages {@code nb.fournisseurs.powens.*}.
 */
public class ParametresDeploiement {

    static final String SECRET = "POWENS_CLIENT_SECRET";
    static final String URL_RETOUR = "POWENS_URL_RETOUR";
    static final String DOMAINE = "POWENS_DOMAINE";
    static final String CLIENT_ID = "POWENS_CLIENT_ID";
    static final String CONNECTOR_ID = "POWENS_CONNECTOR_ID";

    private final UnaryOperator<String> environnement;
    private final UnaryOperator<String> proprietesSysteme;

    /** Lit l'environnement et les propriétés système du processus. */
    public ParametresDeploiement() {
        this(System::getenv, System::getProperty);
    }

    /** Sources injectables (tests). */
    public ParametresDeploiement(UnaryOperator<String> environnement, UnaryOperator<String> proprietesSysteme) {
        this.environnement = environnement;
        this.proprietesSysteme = proprietesSysteme;
    }

    /** Secret client, absent si aucune source ne le donne. */
    public Optional<String> secret() {
        return lire(SECRET);
    }

    /** Adresse de retour de Powens vers l'application. */
    public Optional<String> urlRetour() {
        return lire(URL_RETOUR);
    }

    public Optional<String> domaine() {
        return lire(DOMAINE);
    }

    public Optional<String> clientId() {
        return lire(CLIENT_ID);
    }

    public Optional<String> banques() {
        return lire(CONNECTOR_ID);
    }

    Optional<String> lire(String nom) {
        return renseigne(environnement.apply(nom)).or(() -> renseigne(proprietesSysteme.apply(nom)));
    }

    private static Optional<String> renseigne(String valeur) {
        return Optional.ofNullable(valeur).map(String::trim).filter(texte -> !texte.isEmpty());
    }
}
