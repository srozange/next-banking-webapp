package fr.nextbanking.bourse.adapter.out.fournisseur;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages du fournisseur AlphaVantage, sous {@code nb.fournisseurs.alphavantage.*} ; aucun n'est écrit dans les
 * fichiers versionnés, les valeurs par défaut sont ici. Une valeur invalide (mode inconnu, durée nulle ou négative,
 * seuil inférieur à 1) empêche le démarrage.
 *
 * @param mode         {@code simule} (par défaut) ou {@code reel}
 * @param delaiAttente délai de connexion et de lecture d'un appel (5 s)
 * @param seuilEchecs  nombre d'échecs consécutifs qui ouvrent le coupe-circuit (5)
 * @param dureeOuverture durée pendant laquelle le coupe-circuit reste ouvert (60 s)
 * @param dureeCache   durée de conservation d'une série reçue (12 h)
 */
@ConfigurationProperties("nb.fournisseurs.alphavantage")
public record ProprietesAlphaVantage(String mode, Duration delaiAttente, Integer seuilEchecs, Duration dureeOuverture,
        Duration dureeCache) {

    static final String SIMULE = "simule";
    static final String REEL = "reel";
    private static final String PREFIXE = "nb.fournisseurs.alphavantage.";

    public ProprietesAlphaVantage {
        mode = mode == null ? SIMULE : mode;
        delaiAttente = delaiAttente == null ? Duration.ofSeconds(5) : delaiAttente;
        seuilEchecs = seuilEchecs == null ? 5 : seuilEchecs;
        dureeOuverture = dureeOuverture == null ? Duration.ofSeconds(60) : dureeOuverture;
        dureeCache = dureeCache == null ? Duration.ofHours(12) : dureeCache;
        if (!SIMULE.equals(mode) && !REEL.equals(mode)) {
            throw new IllegalArgumentException(PREFIXE + "mode doit valoir " + SIMULE + " ou " + REEL + " : " + mode);
        }
        positive("delai-attente", delaiAttente);
        positive("duree-ouverture", dureeOuverture);
        positive("duree-cache", dureeCache);
        if (seuilEchecs < 1) {
            throw new IllegalArgumentException(PREFIXE + "seuil-echecs doit être au moins 1 : " + seuilEchecs);
        }
    }

    /** Réglages par défaut : adaptateur simulé. */
    public static ProprietesAlphaVantage parDefaut() {
        return new ProprietesAlphaVantage(null, null, null, null, null);
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
