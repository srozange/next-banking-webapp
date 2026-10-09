package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag("FR-015-01")
@Tag("FR-015-02")
class PointEntreePortefeuilleTest {

    private final List<String> appels = new ArrayList<>();
    private final PointEntreePortefeuille point = new PointEntreePortefeuille((acteurId, roles) -> {
        appels.add(acteurId + " " + roles);
        return List.of(new ClientDuPortefeuille(1L, "Jean Dupont", "user1", "jean.dupont@example.com", null, false));
    });

    @Test
    void transmet_le_sujet_et_les_roles_connus_puis_rend_les_clients() {
        List<PointEntreePortefeuille.ClientJson> reponse = point.portefeuille(jeton("11", List.of("CONSEILLER", "X")));

        assertThat(appels).containsExactly("11 [CONSEILLER]");
        assertThat(reponse).containsExactly(new PointEntreePortefeuille.ClientJson(1L, "Jean Dupont", "user1",
                "jean.dupont@example.com", null, false));
    }

    @Test
    void un_sujet_illisible_rend_401() {
        assertThatThrownBy(() -> point.portefeuille(jeton("abc", List.of("CONSEILLER"))))
                .isInstanceOf(AuthentificationRequise.class);
        assertThat(appels).isEmpty();
    }

    private static Jwt jeton(String sujet, List<String> roles) {
        Jwt.Builder jeton = Jwt.withTokenValue("x").header("alg", "none").subject(sujet)
                .issuedAt(Instant.parse("2026-10-09T09:00:00Z"));
        if (roles != null) {
            jeton.claim("roles", roles);
        }
        return jeton.build();
    }
}
