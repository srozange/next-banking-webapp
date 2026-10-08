package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;

/**
 * Saisie contrôlée d'un virement : comptes source et destinataire, montant et motif.
 */
public record DemandeVirement(CompteId source, CompteId destinataire, Montant montant, Motif motif) {

    /**
     * Contrôle la saisie dans l'ordre de FR-003-07 : comptes obligatoires, comptes distincts, montant, motif.
     *
     * @throws ErreurVirement comptes absents ou identiques, montant illisible, nul, négatif ou trop précis, motif
     *         trop long
     */
    public static DemandeVirement lire(Long source, Long destinataire, String montant, String motif) {
        if (source == null || destinataire == null) {
            throw new ErreurVirement(Cause.COMPTES_OBLIGATOIRES);
        }
        if (source.equals(destinataire)) {
            throw new ErreurVirement(Cause.COMPTES_IDENTIQUES);
        }
        Montant lu = Montant.lire(montant);
        return new DemandeVirement(new CompteId(source), new CompteId(destinataire), lu, Motif.lire(motif));
    }
}
