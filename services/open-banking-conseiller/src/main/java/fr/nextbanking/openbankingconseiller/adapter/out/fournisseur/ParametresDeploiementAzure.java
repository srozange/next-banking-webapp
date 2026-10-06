package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Paramètres de déploiement d'Azure OpenAI, hors des fichiers de configuration : variable d'environnement, puis
 * propriété système, puis valeur par défaut du legacy (RG-010-013) ; une valeur blanche vaut absence. La clé n'a
 * aucune valeur par défaut.
 */
public class ParametresDeploiementAzure {

    static final String CLE = "AZURE_OPENAI_API_KEY";
    static final String ENDPOINT = "AZURE_OPENAI_ENDPOINT";
    static final String DEPLOIEMENT = "AZURE_OPENAI_DEPLOYMENT";
    static final String VERSION = "AZURE_OPENAI_API_VERSION";
    // Le legacy ne documente pas d'endpoint utilisable (« endpoint démo ») : adresse d'exemple à remplacer.
    static final String ENDPOINT_PAR_DEFAUT = "https://demo-bank.cognitiveservices.azure.com";
    static final String DEPLOIEMENT_PAR_DEFAUT = "gpt-5.4-nano";
    static final String VERSION_PAR_DEFAUT = "2024-12-01-preview";

    private final UnaryOperator<String> environnement;
    private final UnaryOperator<String> proprietesSysteme;

    /** Lit l'environnement et les propriétés système du processus. */
    public ParametresDeploiementAzure() {
        this(System::getenv, System::getProperty);
    }

    /** Sources injectables (tests). */
    public ParametresDeploiementAzure(UnaryOperator<String> environnement, UnaryOperator<String> proprietesSysteme) {
        this.environnement = environnement;
        this.proprietesSysteme = proprietesSysteme;
    }

    /** Clé d'accès, absente si aucune source ne la donne. */
    public Optional<String> cle() {
        return lire(CLE);
    }

    public String endpoint() {
        return lire(ENDPOINT).orElse(ENDPOINT_PAR_DEFAUT);
    }

    public String deploiement() {
        return lire(DEPLOIEMENT).orElse(DEPLOIEMENT_PAR_DEFAUT);
    }

    public String version() {
        return lire(VERSION).orElse(VERSION_PAR_DEFAUT);
    }

    private Optional<String> lire(String nom) {
        return renseigne(environnement.apply(nom)).or(() -> renseigne(proprietesSysteme.apply(nom)));
    }

    private static Optional<String> renseigne(String valeur) {
        return Optional.ofNullable(valeur).map(String::trim).filter(texte -> !texte.isEmpty());
    }
}
