package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.ParametresControleFraude;
import java.math.BigDecimal;

/** Paramètres du contrôle de fraude des tests : valeurs par défaut de la story 3.2. */
final class ControleFraudeTestes {

    static final ParametresControleFraude PARAMETRES = new ParametresControleFraude(75, new BigDecimal("5000"),
            new BigDecimal("15000"), 8, 30, 60, 5, new BigDecimal("500"));

    private ControleFraudeTestes() {
    }
}
