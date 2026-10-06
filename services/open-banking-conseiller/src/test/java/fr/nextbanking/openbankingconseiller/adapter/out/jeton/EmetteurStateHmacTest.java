package fr.nextbanking.openbankingconseiller.adapter.out.jeton;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-028-02")
class EmetteurStateHmacTest {

    private static final StatePowens STATE = new StatePowens("10", "nonce", Instant.parse("2026-10-06T10:00:00Z"));
    private final EmetteurStateHmac emetteur = new EmetteurStateHmac("cle-de-test-cle-de-test".getBytes());

    @Test
    void un_state_emis_se_verifie_et_rend_son_contenu() {
        assertThat(emetteur.verifier(emetteur.emettre(STATE))).contains(STATE);
    }

    @Test
    void un_state_modifie_ou_signe_par_une_autre_cle_est_refuse() {
        String emis = emetteur.emettre(STATE);
        String autre = new EmetteurStateHmac("autre-cle-autre-cle".getBytes()).emettre(STATE);

        assertThat(emetteur.verifier(autre)).isEmpty();
        assertThat(emetteur.verifier(emis.replace("nonce", "nonc2"))).isEmpty();
        assertThat(emetteur.verifier(emis + "x")).isEmpty();
        assertThat(emetteur.verifier("sans-separateur")).isEmpty();
        assertThat(emetteur.verifier("")).isEmpty();
    }

    @Test
    void une_signature_valide_sur_un_contenu_mal_forme_est_refusee() {
        String charge = "pas.un.state";
        String signe = emetteur.emettre(new StatePowens("x", "n", Instant.EPOCH));
        String signature = signe.substring(signe.lastIndexOf('.') + 1);

        assertThat(emetteur.verifier(charge + "." + signature)).isEmpty();
    }

    @Test
    void la_cle_derive_du_secret_client_de_l_environnement_puis_des_proprietes_systeme() {
        EmetteurStateHmac environnement = new EmetteurStateHmac(nom -> "secret-1", nom -> "secret-2");
        EmetteurStateHmac systeme = new EmetteurStateHmac(nom -> " ", nom -> "secret-2");

        assertThat(environnement.verifier(new EmetteurStateHmac(nom -> "secret-1", nom -> null).emettre(STATE)))
                .contains(STATE);
        assertThat(systeme.verifier(new EmetteurStateHmac(nom -> null, nom -> "secret-2").emettre(STATE)))
                .contains(STATE);
        assertThat(environnement.verifier(systeme.emettre(STATE))).isEmpty();
    }

    @Test
    void sans_secret_la_cle_est_aleatoire_et_propre_au_processus() {
        EmetteurStateHmac premier = new EmetteurStateHmac(nom -> null, nom -> null);
        EmetteurStateHmac second = new EmetteurStateHmac(nom -> null, nom -> null);

        assertThat(premier.verifier(premier.emettre(STATE))).contains(STATE);
        assertThat(premier.verifier(second.emettre(STATE))).isEmpty();
        assertThat(new EmetteurStateHmac()).isNotNull();
    }
}
