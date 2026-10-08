package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.AjouterBeneficiaire;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.DemandeBeneficiaire;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Cas d'usage : ajout d'un bénéficiaire externe. La saisie est validée et normalisée par le domaine avant tout
 * enregistrement ; la date de création vient de l'{@link Horloge}. Le journal ne reçoit que l'IBAN masqué.
 */
public class AjouterBeneficiaireCasUsage implements AjouterBeneficiaire {

    private static final Logger JOURNAL = Logger.getLogger(AjouterBeneficiaireCasUsage.class.getName());

    private final Beneficiaires beneficiaires;
    private final Horloge horloge;

    public AjouterBeneficiaireCasUsage(Beneficiaires beneficiaires, Horloge horloge) {
        this.beneficiaires = beneficiaires;
        this.horloge = horloge;
    }

    @Override
    public BeneficiaireExterne ajouter(long clientId, String libelle, String titulaire, String iban, String bic,
            String banque) {
        DemandeBeneficiaire demande = DemandeBeneficiaire.valider(libelle, titulaire, iban, bic, banque);
        Instant creeLe = horloge.aujourdhui().atStartOfDay(horloge.fuseau()).toInstant();
        BeneficiaireExterne ajoute = beneficiaires.ajouter(clientId, demande, creeLe);
        JOURNAL.info(() -> "Bénéficiaire " + ajoute.id() + " du client " + clientId + " vers " + ajoute.iban()
                + " ajouté");
        return ajoute;
    }
}
