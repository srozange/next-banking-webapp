package fr.nextbanking.identiteclients.adapter;

import static fr.nextbanking.identiteclients.adapter.OutilsHttp.MDP_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.infrastructure.ApplicationIdentiteClients;
import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Adresse IP d'origine par HTTP, quand l'appelant (boucle locale) est un mandataire de confiance : le premier
 * élément de {@code X-Forwarded-For} est retenu ; un en-tête absent, vide ou illisible retient l'adresse distante.
 */
@SpringBootTest(classes = ApplicationIdentiteClients.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nb.reseau.mandataires-de-confiance=127.0.0.0/8,::1/128")
@DirtiesContext
@Tag("FR-001-13")
@Tag("FR-026-05")
@Tag("BUG-083")
class AdresseIpIT {

    static final List<String> BOUCLE_LOCALE = List.of("127.0.0.1", "0:0:0:0:0:0:0:1");
    static final String USER1 = "{\"identifiant\":\"user1\",\"motDePasse\":\"" + MDP_CLIENT + "\"}";

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void retient_le_premier_element_de_l_en_tete_venant_d_un_mandataire_de_confiance() throws Exception {
        assertThat(adresseAuditee("203.0.113.7, 10.0.0.1", USER1)).isEqualTo("203.0.113.7");
        assertThat(adresseAuditee("203.0.113.8", "{\"identifiant\":\"inconnu\",\"motDePasse\":\"x\"}"))
                .isEqualTo("203.0.113.8");
    }

    @Test
    void un_en_tete_absent_vide_ou_illisible_retient_l_adresse_distante() throws Exception {
        for (String enTete : new String[] {null, "", " , 10.0.0.1", "pas-une-ip"}) {
            assertThat(adresseAuditee(enTete, USER1)).as(String.valueOf(enTete)).isIn(BOUCLE_LOCALE);
        }
    }

    /** Se connecte avec l'en-tête donné (absent si {@code null}) et rend l'adresse de l'entrée d'audit écrite. */
    static String adresseAuditee(int port, JdbcClient jdbc, String enTete, String corps) throws Exception {
        long depuis = LignesAudit.dernierId(jdbc);
        Map<String, String> entetes = new HashMap<>(Map.of("Content-Type", "application/json"));
        if (enTete != null) {
            entetes.put("X-Forwarded-For", enTete);
        }
        OutilsHttp.envoyer(port, "POST", "/api/identite-clients/connexion", corps, entetes);
        return LignesAudit.depuis(jdbc, depuis).getFirst().donnees().get("adresseIp").asString();
    }

    private String adresseAuditee(String enTete, String corps) throws Exception {
        return adresseAuditee(port, jdbc, enTete, corps);
    }
}
