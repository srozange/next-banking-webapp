package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBeneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import java.util.Comparator;
import java.util.List;

/** Cas d'usage : liste des bénéficiaires actifs du client, triés par libellé croissant puis identifiant (RG-013-005). */
public class ConsulterBeneficiairesCasUsage implements ConsulterBeneficiaires {

    private final Beneficiaires beneficiaires;

    public ConsulterBeneficiairesCasUsage(Beneficiaires beneficiaires) {
        this.beneficiaires = beneficiaires;
    }

    @Override
    public List<BeneficiaireExterne> beneficiaires(long clientId) {
        return beneficiaires.actifsDuClient(clientId).stream()
                .sorted(Comparator.comparing(BeneficiaireExterne::libelle)
                        .thenComparingLong(BeneficiaireExterne::id))
                .toList();
    }
}
