package fr.nextbanking.openbankingconseiller.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-028-02")
class PowensTest {

    private static final Instant EXPIRATION = Instant.parse("2026-10-06T10:15:00Z");

    @Test
    void un_client_sans_connexion_est_non_connecte_sans_jeton_ni_message() {
        EtatPowens etat = EtatPowens.nonConnecte();

        assertThat(etat.connecte()).isFalse();
        assertThat(etat.jeton()).isNull();
        assertThat(etat.message()).isNull();
    }

    @Test
    void le_state_se_relit_tel_qu_il_a_ete_ecrit_meme_avec_des_points_dans_le_sub() {
        StatePowens state = new StatePowens("a.b.c", "nonce1", EXPIRATION);

        assertThat(StatePowens.lire(state.charge())).contains(state);
    }

    @Test
    void un_state_mal_forme_n_est_pas_relu() {
        assertThat(StatePowens.lire("")).isEmpty();
        assertThat(StatePowens.lire("a.b")).isEmpty();
        assertThat(StatePowens.lire("MTA..123")).isEmpty();
        assertThat(StatePowens.lire("***.nonce.123")).isEmpty();
        assertThat(StatePowens.lire("MTA.nonce.pasunnombre")).isEmpty();
        assertThat(StatePowens.lire("MTA.nonce." + Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void le_state_expire_a_son_expiration() {
        StatePowens state = new StatePowens("10", "n", EXPIRATION);

        assertThat(state.estExpire(EXPIRATION.minusSeconds(1))).isFalse();
        assertThat(state.estExpire(EXPIRATION)).isTrue();
        assertThat(state.estExpire(EXPIRATION.plusSeconds(1))).isTrue();
    }
}
