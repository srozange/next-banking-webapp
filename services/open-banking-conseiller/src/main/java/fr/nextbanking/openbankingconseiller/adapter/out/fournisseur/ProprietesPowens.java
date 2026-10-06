package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de Powens sous {@code nb.fournisseurs.powens.*} ; aucun n'est écrit dans les fichiers versionnés, les
 * valeurs par défaut sont ici. Une valeur invalide (mode inconnu, texte vide, durée nulle ou négative, seuil
 * inférieur à 1) empêche le démarrage. Le secret client et l'adresse de retour ne sont pas des propriétés : ce sont
 * des paramètres de déploiement ({@link ParametresDeploiement}).
 *
 * @param mode           {@code simule} (par défaut) ou {@code reel}
 * @param domaine        domaine Powens ({@code demi-sandbox.biapi.pro})
 * @param clientId       identifiant client ({@code 99961298})
 * @param banques        identifiants des banques proposées, séparés par des virgules ({@code 59})
 * @param delaiAttente   délai de connexion et de lecture d'un appel (15 s)
 * @param seuilEchecs    nombre d'échecs consécutifs qui ouvrent le coupe-circuit (5)
 * @param dureeOuverture durée pendant laquelle le coupe-circuit reste ouvert (60 s)
 */
@ConfigurationProperties("nb.fournisseurs.powens")
public record ProprietesPowens(String mode, String domaine, String clientId, String banques, Duration delaiAttente,
        Integer seuilEchecs, Duration dureeOuverture) {

    static final String SIMULE = "simule";
    static final String REEL = "reel";
    private static final String PREFIXE = "nb.fournisseurs.powens.";

    public ProprietesPowens {
        mode = mode == null ? SIMULE : mode;
        domaine = domaine == null ? "demi-sandbox.biapi.pro" : domaine;
        clientId = clientId == null ? "99961298" : clientId;
        banques = banques == null ? "59" : banques;
        delaiAttente = delaiAttente == null ? Duration.ofSeconds(15) : delaiAttente;
        seuilEchecs = seuilEchecs == null ? 5 : seuilEchecs;
        dureeOuverture = dureeOuverture == null ? Duration.ofSeconds(60) : dureeOuverture;
        if (!SIMULE.equals(mode) && !REEL.equals(mode)) {
            throw new IllegalArgumentException(PREFIXE + "mode doit valoir " + SIMULE + " ou " + REEL + " : " + mode);
        }
        renseigne("domaine", domaine);
        renseigne("client-id", clientId);
        renseigne("banques", banques);
        positive("delai-attente", delaiAttente);
        positive("duree-ouverture", dureeOuverture);
        if (seuilEchecs < 1) {
            throw new IllegalArgumentException(PREFIXE + "seuil-echecs doit être au moins 1 : " + seuilEchecs);
        }
    }

    /** Réglages par défaut : adaptateur simulé. */
    public static ProprietesPowens parDefaut() {
        return new ProprietesPowens(null, null, null, null, null, null, null);
    }

    /** Vrai si l'adaptateur réel est demandé. */
    public boolean reel() {
        return REEL.equals(mode);
    }

    private static void renseigne(String nom, String valeur) {
        if (valeur.isBlank()) {
            throw new IllegalArgumentException(PREFIXE + nom + " ne doit pas être vide");
        }
    }

    private static void positive(String nom, Duration duree) {
        if (duree.isZero() || duree.isNegative()) {
            throw new IllegalArgumentException(PREFIXE + nom + " doit être une durée positive : " + duree);
        }
    }
}
