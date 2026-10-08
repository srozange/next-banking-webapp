package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Demande de création d'un virement programmé, validée et normalisée (FR-004-04 à 06). La première règle violée, dans
 * l'ordre compte source, numéro du bénéficiaire, libellé, montant, date vide, format de date, puis fréquence et
 * longueurs, lève {@link ErreurVirementProgramme}. {@code motif} est nul quand il est blanc.
 */
public record DemandeVirementProgramme(long compteSourceId, String numeroBeneficiaire, String libelleBeneficiaire,
        Montant montant, FrequenceVirement frequence, String motif, LocalDate premiereExecution) {

    /** Longueur maximale du numéro du bénéficiaire. */
    public static final int NUMERO_MAX = 34;
    /** Longueur maximale du libellé du bénéficiaire. */
    public static final int LIBELLE_MAX = 100;
    /** Longueur maximale du motif. */
    public static final int MOTIF_MAX = 255;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Valide la saisie brute puis la normalise : textes rognés, montant à 2 décimales, date ISO stricte. */
    public static DemandeVirementProgramme valider(Long compteSourceId, String numeroBeneficiaire,
            String libelleBeneficiaire, String montant, String frequence, String motif, String datePremiereExecution) {
        if (compteSourceId == null) {
            throw new ErreurVirementProgramme(Cause.COMPTE_SOURCE_OBLIGATOIRE);
        }
        if (estBlanc(numeroBeneficiaire)) {
            throw new ErreurVirementProgramme(Cause.NUMERO_OBLIGATOIRE);
        }
        if (estBlanc(libelleBeneficiaire)) {
            throw new ErreurVirementProgramme(Cause.LIBELLE_OBLIGATOIRE);
        }
        Montant montantLu = lireMontant(montant);
        if (estBlanc(datePremiereExecution)) {
            throw new ErreurVirementProgramme(Cause.DATE_OBLIGATOIRE);
        }
        LocalDate date = lireDate(datePremiereExecution.strip());
        FrequenceVirement frequenceLue = lireFrequence(frequence);
        String numero = numeroBeneficiaire.strip();
        String libelle = libelleBeneficiaire.strip();
        String motifNormalise = estBlanc(motif) ? null : motif.strip();
        if (numero.length() > NUMERO_MAX || libelle.length() > LIBELLE_MAX
                || motifNormalise != null && motifNormalise.length() > MOTIF_MAX) {
            throw new ErreurVirementProgramme(Cause.TROP_LONG);
        }
        return new DemandeVirementProgramme(compteSourceId, numero, libelle, montantLu, frequenceLue, motifNormalise,
                date);
    }

    private static Montant lireMontant(String montant) {
        try {
            return Montant.lire(montant);
        } catch (ErreurVirement e) {
            throw new ErreurVirementProgramme(Cause.MONTANT_INVALIDE);
        }
    }

    private static LocalDate lireDate(String date) {
        try {
            return LocalDate.parse(date, FORMAT_DATE);
        } catch (DateTimeParseException e) {
            throw new ErreurVirementProgramme(Cause.DATE_INVALIDE);
        }
    }

    private static FrequenceVirement lireFrequence(String frequence) {
        if (frequence != null) {
            for (FrequenceVirement candidate : FrequenceVirement.values()) {
                if (candidate.name().equals(frequence)) {
                    return candidate;
                }
            }
        }
        throw new ErreurVirementProgramme(Cause.FREQUENCE_INVALIDE);
    }

    private static boolean estBlanc(String texte) {
        return texte == null || texte.isBlank();
    }
}
