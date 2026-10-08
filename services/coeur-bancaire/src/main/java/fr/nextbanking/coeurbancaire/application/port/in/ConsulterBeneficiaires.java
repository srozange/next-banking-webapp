package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import java.util.List;

/** Port entrant : bénéficiaires externes actifs du client authentifié. */
public interface ConsulterBeneficiaires {

    /** Bénéficiaires actifs du client, triés par libellé croissant (RG-013-003 à RG-013-005). */
    List<BeneficiaireExterne> beneficiaires(long clientId);
}
