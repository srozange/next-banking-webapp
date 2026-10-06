package fr.nextbanking.identiteclients.adapter.out.motdepasse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("FR-001-08")
@Tag("BUG-002")
class MotsDePasseSpringTest {

    private static final String EMPREINTE =
            PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("bon-mot-de-passe");

    private final MotsDePasseSpring motsDePasse = new MotsDePasseSpring();

    @Test
    void l_empreinte_est_bcrypt_et_ne_contient_pas_le_mot_de_passe() {
        assertThat(EMPREINTE).startsWith("{bcrypt}").doesNotContain("bon-mot-de-passe");
    }

    @Test
    void reconnait_le_bon_mot_de_passe() {
        assertThat(motsDePasse.correspond("bon-mot-de-passe", Optional.of(EMPREINTE))).isTrue();
    }

    @Test
    void refuse_un_mauvais_mot_de_passe() {
        assertThat(motsDePasse.correspond("mauvais", Optional.of(EMPREINTE))).isFalse();
    }

    @Test
    void refuse_sans_empreinte_quel_que_soit_le_mot_de_passe() {
        assertThat(motsDePasse.correspond("bon-mot-de-passe", Optional.empty())).isFalse();
        assertThat(motsDePasse.correspond("", Optional.empty())).isFalse();
    }

    @Test
    void refuse_un_mot_de_passe_en_clair_enregistre_sans_algorithme() {
        assertThat(motsDePasse.correspond("mot-de-passe-en-clair", Optional.of("mot-de-passe-en-clair"))).isFalse();
    }

    @Test
    void sans_empreinte_compare_quand_meme_le_mot_de_passe_a_l_empreinte_factice() {
        List<String> comparees = new ArrayList<>();
        PasswordEncoder compteur = new PasswordEncoder() {
            @Override
            public String encode(CharSequence motDePasse) {
                return "{factice}empreinte-factice";
            }

            @Override
            public boolean matches(CharSequence motDePasse, String empreinte) {
                comparees.add(motDePasse + "|" + empreinte);
                return true;
            }
        };

        boolean resultat = new MotsDePasseSpring(compteur).correspond("saisi", Optional.empty());

        assertThat(resultat).isFalse();
        assertThat(comparees).containsExactly("saisi|{factice}empreinte-factice");
    }
}
