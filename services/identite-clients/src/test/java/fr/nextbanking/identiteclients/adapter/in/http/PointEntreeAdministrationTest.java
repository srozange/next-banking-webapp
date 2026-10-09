package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.application.port.in.ConsulterAdministration;
import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.IndicateursUtilisateurs;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag("FR-016-02")
@Tag("FR-016-08")
@Tag("FR-016-09")
@Tag("FR-016-10")
@Tag("BUG-055")
@Tag("FR-016-13")
class PointEntreeAdministrationTest {

    private static final FicheUtilisateur JEAN = new FicheUtilisateur(1L, "Jean Dupont", "user1",
            "jean.dupont@example.com", null, Role.CLIENT, false, null, LocalDate.of(2024, 2, 1));

    private final List<String> appels = new ArrayList<>();
    private final PointEntreeAdministration point = new PointEntreeAdministration(new ConsulterAdministration() {
        @Override
        public IndicateursUtilisateurs indicateurs(Set<Role> roles) {
            appels.add("indicateurs " + roles);
            return new IndicateursUtilisateurs(4, 3, 2, 7);
        }

        @Override
        public List<FicheUtilisateur> clients(Set<Role> roles) {
            appels.add("clients " + roles);
            return List.of(JEAN);
        }

        @Override
        public List<FicheUtilisateur> utilisateurs(Set<Role> roles) {
            appels.add("utilisateurs " + roles);
            return List.of(JEAN);
        }
    });

    @Test
    void transmet_les_roles_connus_et_rend_les_trois_formes() {
        Jwt jeton = jeton(List.of("ADMIN", "X"));

        assertThat(point.indicateurs(jeton)).isEqualTo(new PointEntreeAdministration.IndicateursJson(4, 3, 2, 7));
        assertThat(point.clients(jeton)).containsExactly(new PointEntreeAdministration.ClientJson(1L, "Jean Dupont",
                "user1", "jean.dupont@example.com", null, "CLIENT", false, null, "2024-02-01"));
        assertThat(point.utilisateurs(jeton)).containsExactly(new PointEntreeAdministration.UtilisateurJson(1L,
                "Jean Dupont", "user1", "jean.dupont@example.com", "CLIENT", false));
        assertThat(appels).containsExactly("indicateurs [ADMIN]", "clients [ADMIN]", "utilisateurs [ADMIN]");
    }

    private static Jwt jeton(List<String> roles) {
        return Jwt.withTokenValue("x").header("alg", "none").subject("10")
                .issuedAt(Instant.parse("2026-10-09T09:00:00Z")).claim("roles", roles).build();
    }
}
