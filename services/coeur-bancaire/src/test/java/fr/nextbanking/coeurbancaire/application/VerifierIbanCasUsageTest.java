package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.VerificationIban.Statut;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Vérification d'un IBAN : résultat structuré du domaine, sans texte. */
@Tag("FR-013-04")
class VerifierIbanCasUsageTest {

    private final VerifierIbanCasUsage cas = new VerifierIbanCasUsage();

    @Test
    void rend_le_resultat_du_domaine() {
        assertThat(cas.verifier("").statut()).isEqualTo(Statut.VIDE);
        assertThat(cas.verifier("FR3530004000000123456789011").statut()).isEqualTo(Statut.INVALIDE);
        assertThat(cas.verifier("FR3530004000000123456789010").codePays()).isEqualTo("FR");
    }
}
