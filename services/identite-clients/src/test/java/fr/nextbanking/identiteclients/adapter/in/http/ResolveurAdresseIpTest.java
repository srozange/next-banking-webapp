package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("FR-001-13")
@Tag("FR-026-05")
@Tag("BUG-083")
class ResolveurAdresseIpTest {

    private static final String MANDATAIRE = "172.30.0.5";

    private final ResolveurAdresseIp resolveur = new ResolveurAdresseIp("172.30.0.0/24");

    @Test
    void retient_le_premier_element_rogne_de_l_en_tete_venant_d_un_mandataire_de_confiance() {
        assertThat(resolveur.resoudre(MANDATAIRE, "203.0.113.7, 10.0.0.1")).isEqualTo("203.0.113.7");
        assertThat(resolveur.resoudre(MANDATAIRE, "  203.0.113.7  ")).isEqualTo("203.0.113.7");
        assertThat(resolveur.resoudre(MANDATAIRE, "2001:db8::7, 10.0.0.1")).isEqualTo("2001:db8::7");
    }

    @Test
    void ignore_l_en_tete_venant_d_une_adresse_hors_plage() {
        assertThat(resolveur.resoudre("198.51.100.9", "203.0.113.7")).isEqualTo("198.51.100.9");
        assertThat(resolveur.resoudre("172.30.1.5", "203.0.113.7")).isEqualTo("172.30.1.5");
        assertThat(resolveur.resoudre("2001:db8::1", "203.0.113.7")).isEqualTo("2001:db8::1");
    }

    @Test
    void retient_l_adresse_distante_si_l_en_tete_est_absent_vide_ou_illisible() {
        for (String enTete : new String[] {null, "", " , 10.0.0.1", "pas-une-ip", "256.1.1.1", "1.2.3", "01.2.3.4",
                "exemple.fr", "1:2", "fe80::1%eth0", ".:", ".1::", "g::1"}) {
            assertThat(resolveur.resoudre(MANDATAIRE, enTete)).as(String.valueOf(enTete)).isEqualTo(MANDATAIRE);
        }
    }

    @Test
    void une_adresse_distante_illisible_n_est_jamais_de_confiance() {
        assertThat(resolveur.resoudre("inconnue", "203.0.113.7")).isEqualTo("inconnue");
        assertThat(resolveur.resoudre(null, "203.0.113.7")).isNull();
    }

    @Test
    void lit_la_requete_servlet() {
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/api/identite-clients/connexion");
        requete.setRemoteAddr(MANDATAIRE);
        requete.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        assertThat(resolveur.resoudre(requete)).isEqualTo("203.0.113.7");
    }

    @Test
    void accepte_plusieurs_plages_ipv4_et_ipv6_et_ne_compare_jamais_les_familles_entre_elles() {
        ResolveurAdresseIp plusieurs = new ResolveurAdresseIp(" 127.0.0.0/8 , ::1/128,10.1.2.3/32,0.0.0.0/0 ");

        assertThat(plusieurs.resoudre("::1", "203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(plusieurs.resoudre("8.8.8.8", "203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(plusieurs.resoudre("::2", "203.0.113.7")).isEqualTo("::2");

        ResolveurAdresseIp ipv6 = new ResolveurAdresseIp("2001:db8::/33");
        assertThat(ipv6.resoudre("2001:db8:7fff::1", "203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(ipv6.resoudre("2001:db8:8000::1", "203.0.113.7")).isEqualTo("2001:db8:8000::1");
        assertThat(ipv6.resoudre("10.0.0.1", "203.0.113.7")).isEqualTo("10.0.0.1");
    }

    @Test
    void refuse_une_plage_absente_ou_illisible() {
        for (String plage : new String[] {null, "", "  "}) {
            assertThatThrownBy(() -> new ResolveurAdresseIp(plage))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nb.reseau.mandataires-de-confiance est obligatoire");
        }
        for (String plage : new String[] {"172.30.0.0", "172.30.0.0/", "172.30.0.0/33", "reseau/24", "172.30.0.0/x",
                "172.30.0.0/24,", "::1/129", "172.30.0.0/1234"}) {
            assertThatThrownBy(() -> new ResolveurAdresseIp(plage)).as(plage)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("plage CIDR illisible");
        }
    }
}
