package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire.Cause;
import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire.Champ;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Demande d'ajout d'un bénéficiaire externe, validée et normalisée (FR-013-05 à 07). {@code bic} et {@code banque}
 * sont nuls quand ils sont blancs. La première règle violée, dans l'ordre libellé, IBAN vide, IBAN MOD-97, titulaire,
 * BIC, puis les longueurs, lève {@link ErreurSaisieBeneficiaire}.
 */
public record DemandeBeneficiaire(String libelle, String titulaire, Iban iban, String bic, String banque) {

    /** Longueur maximale du libellé, du titulaire et de la banque (FR-013-06). */
    public static final int LONGUEUR_MAX = 100;

    private static final Pattern FORMAT_BIC = Pattern.compile("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");

    /** Valide la saisie brute puis la normalise : IBAN sans blancs en majuscules, BIC et banque rognés. */
    public static DemandeBeneficiaire valider(String libelle, String titulaire, String iban, String bic,
            String banque) {
        if (estBlanc(libelle)) {
            throw refus(Champ.LIBELLE, Cause.OBLIGATOIRE, ErreurSaisieBeneficiaire.LIBELLE_OBLIGATOIRE);
        }
        String ibanNormalise = Iban.normaliser(iban);
        if (ibanNormalise.isEmpty()) {
            throw refus(Champ.IBAN, Cause.OBLIGATOIRE, ErreurSaisieBeneficiaire.IBAN_OBLIGATOIRE);
        }
        Iban numero = new Iban(ibanNormalise);
        if (!numero.controleMod97()) {
            throw refus(Champ.IBAN, Cause.IBAN_INVALIDE, ErreurSaisieBeneficiaire.IBAN_INVALIDE);
        }
        if (estBlanc(titulaire)) {
            throw refus(Champ.TITULAIRE, Cause.OBLIGATOIRE, ErreurSaisieBeneficiaire.TITULAIRE_OBLIGATOIRE);
        }
        String bicNormalise = bic == null || bic.isBlank() ? null : bic.strip().toUpperCase(Locale.ROOT);
        if (bicNormalise != null && !FORMAT_BIC.matcher(bicNormalise).matches()) {
            throw refus(Champ.BIC, Cause.BIC_INVALIDE, ErreurSaisieBeneficiaire.BIC_INVALIDE);
        }
        String banqueNormalisee = banque == null || banque.isBlank() ? null : banque.strip();
        verifierLongueur(Champ.LIBELLE, libelle);
        verifierLongueur(Champ.TITULAIRE, titulaire);
        verifierLongueur(Champ.BANQUE, banqueNormalisee);
        return new DemandeBeneficiaire(libelle, titulaire, numero, bicNormalise, banqueNormalisee);
    }

    /** Appartenance à la zone SEPA, calculée depuis le code pays de l'IBAN normalisé (RG-013-025). */
    public boolean zoneSepa() {
        return iban.zoneSepa();
    }

    private static boolean estBlanc(String texte) {
        return texte == null || texte.isBlank();
    }

    private static void verifierLongueur(Champ champ, String texte) {
        if (texte != null && texte.length() > LONGUEUR_MAX) {
            throw refus(champ, Cause.TROP_LONG, ErreurSaisieBeneficiaire.TROP_LONG);
        }
    }

    private static ErreurSaisieBeneficiaire refus(Champ champ, Cause cause, String message) {
        return new ErreurSaisieBeneficiaire(champ, cause, message);
    }
}
