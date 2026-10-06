package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-003-02")
@Tag("FR-023-02")
class ErreurVirementTest {

    @Test
    void le_message_est_le_texte_de_la_cause_sans_detail_technique() {
        for (Cause cause : Cause.values()) {
            ErreurVirement erreur = new ErreurVirement(cause, new IllegalStateException("détail interne"));

            assertThat(erreur).hasMessage(cause.texte());
            assertThat(erreur.motif()).isEqualTo(cause);
        }
        assertThat(new ErreurVirement(Cause.REMISE_IMPOSSIBLE).getCause()).isNull();
    }

    @Test
    @Tag("BUG-073")
    void le_texte_d_echec_de_remise_est_celui_de_a_023_1() {
        assertThat(Cause.REMISE_IMPOSSIBLE.texte())
                .isEqualTo("Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard");
    }
}
