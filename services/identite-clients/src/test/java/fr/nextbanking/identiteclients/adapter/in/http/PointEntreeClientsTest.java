package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.port.in.ChangerEtatClient;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag("FR-030-07")
class PointEntreeClientsTest {

    private final List<String> appels = new ArrayList<>();
    private final ChangerEtatClient port = new ChangerEtatClient() {
        @Override
        public void desactiver(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp) {
            appels.add("desactiver " + acteurId + " " + rolesActeur + " " + clientId + " " + adresseIp);
        }

        @Override
        public void activer(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp) {
            appels.add("activer " + acteurId + " " + rolesActeur + " " + clientId + " " + adresseIp);
        }
    };
    private final PointEntreeClients point = new PointEntreeClients(port, new ResolveurAdresseIp("10.0.0.0/8"));

    @Test
    void transmet_l_acteur_ses_roles_connus_et_le_client_au_cas_d_usage() {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.setRemoteAddr("203.0.113.7");
        Jwt jeton = jeton("10", List.of("ADMIN", "INCONNU"));

        point.desactiver(jeton, 1L, requete);
        point.activer(jeton, 1L, requete);

        assertThat(appels).containsExactly("desactiver 10 [ADMIN] 1 203.0.113.7", "activer 10 [ADMIN] 1 203.0.113.7");
    }

    @Test
    void sans_revendication_roles_l_ensemble_est_vide() {
        point.desactiver(jeton("10", null), 1L, new MockHttpServletRequest());

        assertThat(appels).singleElement().asString().contains("10 []");
    }

    @Test
    void un_sujet_illisible_rend_401() {
        assertThatThrownBy(() -> point.activer(jeton("abc", List.of("ADMIN")), 1L, new MockHttpServletRequest()))
                .isInstanceOf(AuthentificationRequise.class);
        assertThatThrownBy(() -> point.desactiver(jeton("abc", List.of("ADMIN")), 1L, new MockHttpServletRequest()))
                .isInstanceOf(AuthentificationRequise.class);
    }

    private static Jwt jeton(String sujet, List<String> roles) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256").subject(sujet)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }
}
