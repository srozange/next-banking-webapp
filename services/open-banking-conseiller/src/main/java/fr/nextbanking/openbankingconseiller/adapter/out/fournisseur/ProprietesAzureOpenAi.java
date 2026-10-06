package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages du fournisseur Azure OpenAI, sous {@code nb.fournisseurs.azure-openai.*} ; aucun n'est écrit dans les
 * fichiers versionnés, les valeurs par défaut sont ici. Une valeur invalide empêche le démarrage.
 *
 * @param mode           {@code simule} (par défaut) ou {@code reel}
 * @param delaiConnexion délai de connexion (15 s, RG-010-019)
 * @param delaiLecture   délai de lecture (30 s, RG-010-019)
 * @param seuilEchecs    échecs consécutifs qui ouvrent le coupe-circuit (5)
 * @param dureeOuverture durée d'ouverture du coupe-circuit (60 s)
 * @param refusSimule    statut de refus (401 ou 403) que l'adaptateur simulé simule ; absent : aucun refus
 */
@ConfigurationProperties("nb.fournisseurs.azure-openai")
public record ProprietesAzureOpenAi(String mode, Duration delaiConnexion, Duration delaiLecture, Integer seuilEchecs,
        Duration dureeOuverture, Integer refusSimule) {

    static final String SIMULE = "simule";
    static final String REEL = "reel";
    private static final String PREFIXE = "nb.fournisseurs.azure-openai.";

    public ProprietesAzureOpenAi {
        mode = mode == null ? SIMULE : mode;
        delaiConnexion = delaiConnexion == null ? Duration.ofSeconds(15) : delaiConnexion;
        delaiLecture = delaiLecture == null ? Duration.ofSeconds(30) : delaiLecture;
        seuilEchecs = seuilEchecs == null ? 5 : seuilEchecs;
        dureeOuverture = dureeOuverture == null ? Duration.ofSeconds(60) : dureeOuverture;
        if (!SIMULE.equals(mode) && !REEL.equals(mode)) {
            throw new IllegalArgumentException(PREFIXE + "mode doit valoir " + SIMULE + " ou " + REEL + " : " + mode);
        }
        positive("delai-connexion", delaiConnexion);
        positive("delai-lecture", delaiLecture);
        positive("duree-ouverture", dureeOuverture);
        if (seuilEchecs < 1) {
            throw new IllegalArgumentException(PREFIXE + "seuil-echecs doit être au moins 1 : " + seuilEchecs);
        }
        if (refusSimule != null && refusSimule != 401 && refusSimule != 403) {
            throw new IllegalArgumentException(PREFIXE + "refus-simule doit valoir 401 ou 403 : " + refusSimule);
        }
    }

    /** Vrai si l'adaptateur réel est demandé. */
    public boolean reel() {
        return REEL.equals(mode);
    }

    private static void positive(String nom, Duration duree) {
        if (duree.isZero() || duree.isNegative()) {
            throw new IllegalArgumentException(PREFIXE + nom + " doit être une durée positive : " + duree);
        }
    }
}
