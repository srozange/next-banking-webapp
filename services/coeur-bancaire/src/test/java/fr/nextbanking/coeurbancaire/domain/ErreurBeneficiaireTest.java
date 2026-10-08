package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire.Cause;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-013-10")
@Tag("BUG-047")
class ErreurBeneficiaireTest {

    @Test
    void le_message_est_unique_quelle_que_soit_la_cause() {
        for (Cause cause : Cause.values()) {
            ErreurBeneficiaire erreur = new ErreurBeneficiaire(cause);

            assertThat(erreur).hasMessage("Erreur lors de la suppression.");
            assertThat(erreur.motif()).isEqualTo(cause);
        }
    }

    @Test
    void un_beneficiaire_n_appartient_qu_a_son_client() {
        BeneficiaireExterne beneficiaire = new BeneficiaireExterne(5, 1, "Loyer", "Marie",
                new Iban("FR3530004000000123456789010"), null, null);

        assertThat(beneficiaire.appartientA(1)).isTrue();
        assertThat(beneficiaire.appartientA(2)).isFalse();
    }
}
