package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnexionRefuseeTest {

    @Test
    void porte_son_motif_sans_autre_detail() {
        ConnexionRefusee refus = new ConnexionRefusee(ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);

        assertThat(refus.motif()).isEqualTo(ConnexionRefusee.Motif.IDENTIFIANTS_INCORRECTS);
        assertThat(refus.getMessage()).isEqualTo("Connexion refusée : IDENTIFIANTS_INCORRECTS");
    }
}
