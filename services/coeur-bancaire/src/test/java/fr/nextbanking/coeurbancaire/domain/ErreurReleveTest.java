package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ErreurReleve.Cause;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-005-05")
class ErreurReleveTest {

    @Test
    void le_message_est_le_texte_de_la_cause() {
        for (Cause cause : Cause.values()) {
            ErreurReleve erreur = new ErreurReleve(cause);

            assertThat(erreur).hasMessage(cause.texte());
            assertThat(erreur.motif()).isEqualTo(cause);
        }
        assertThat(Cause.COMPTE_INTROUVABLE.texte()).isEqualTo("Ressource introuvable");
        assertThat(Cause.COMPTE_ETRANGER.texte()).isEqualTo("Accès refusé");
    }
}
