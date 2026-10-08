package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.CreerVirementProgramme;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Cas d'usage : création d'un virement programmé (FR-004-04 à 07, FR-004-13). Ordre des contrôles : saisie, compte
 * source, appartenance du compte source, existence du compte destinataire par son numéro (BUG-015). La prochaine
 * exécution est la date saisie à 00:00 dans le fuseau métier ; la date de création vient de l'{@link Horloge}. Le
 * journal ne reçoit ni le numéro du bénéficiaire ni son libellé.
 */
public class CreerVirementProgrammeCasUsage implements CreerVirementProgramme {

    private static final Logger JOURNAL = Logger.getLogger(CreerVirementProgrammeCasUsage.class.getName());

    private final VirementsProgrammes virements;
    private final Comptes comptes;
    private final Horloge horloge;

    public CreerVirementProgrammeCasUsage(VirementsProgrammes virements, Comptes comptes, Horloge horloge) {
        this.virements = virements;
        this.comptes = comptes;
        this.horloge = horloge;
    }

    @Override
    public LigneVirementProgramme creer(long clientId, Long compteSourceId, String numeroBeneficiaire,
            String libelleBeneficiaire, String montant, String frequence, String motif,
            String datePremiereExecution) {
        DemandeVirementProgramme demande = DemandeVirementProgramme.valider(compteSourceId, numeroBeneficiaire,
                libelleBeneficiaire, montant, frequence, motif, datePremiereExecution);
        Compte source = comptes.trouver(demande.compteSourceId())
                .orElseThrow(() -> new ErreurVirementProgramme(Cause.COMPTE_SOURCE_INTROUVABLE));
        if (!source.appartientA(clientId)) {
            throw new ErreurVirementProgramme(Cause.COMPTE_SOURCE_ETRANGER);
        }
        if (comptes.parNumero(demande.numeroBeneficiaire()).isEmpty()) {
            throw new ErreurVirementProgramme(Cause.DESTINATAIRE_INTROUVABLE);
        }
        Instant prochaine = demande.premiereExecution().atStartOfDay(horloge.fuseau()).toInstant();
        VirementProgramme cree = virements.enregistrer(clientId, demande, prochaine, horloge.instant());
        JOURNAL.info(() -> "Virement programmé " + cree.id() + " du client " + clientId + " créé");
        return ConsulterVirementsProgrammesCasUsage.ligne(cree, source.numero(), horloge);
    }
}
