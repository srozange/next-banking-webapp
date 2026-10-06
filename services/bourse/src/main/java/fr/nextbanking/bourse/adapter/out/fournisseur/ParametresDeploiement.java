package fr.nextbanking.bourse.adapter.out.fournisseur;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Paramètres de déploiement du fournisseur AlphaVantage, hors des fichiers de configuration : variable
 * d'environnement, puis propriété système ; une valeur blanche vaut absence (FR-029-04, FR-029-05). La clé n'a
 * aucune valeur par défaut ; l'adresse vaut {@value #ADRESSE_PAR_DEFAUT} par défaut.
 */
public class ParametresDeploiement {

    static final String CLE = "ALPHA_VANTAGE_API_KEY";
    static final String ADRESSE = "ALPHA_VANTAGE_BASE_URL";
    static final String ADRESSE_PAR_DEFAUT = "https://www.alphavantage.co/query";

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

    /** Clé d'accès, absente si aucune source ne la donne. */
    public Optional<String> cle() {
        return lire(CLE);
    }

    /** Adresse du fournisseur, sinon l'adresse publique d'AlphaVantage. */
    public String adresse() {
        return lire(ADRESSE).orElse(ADRESSE_PAR_DEFAUT);
    }

    Optional<String> lire(String nom) {
        return renseigne(environnement.apply(nom)).or(() -> renseigne(proprietesSysteme.apply(nom)));
    }

    private static Optional<String> renseigne(String valeur) {
        return Optional.ofNullable(valeur).map(String::trim).filter(texte -> !texte.isEmpty());
    }
}
