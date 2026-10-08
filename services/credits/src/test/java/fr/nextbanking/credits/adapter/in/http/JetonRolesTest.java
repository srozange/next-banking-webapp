package fr.nextbanking.credits.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag("FR-030-07")
class JetonRolesTest {

    private static Jwt jeton(Object roles) {
        Jwt.Builder construction = Jwt.withTokenValue("t").header("alg", "RS256").subject("1")
                .issuedAt(Instant.parse("2026-10-09T10:00:00Z")).expiresAt(Instant.parse("2026-10-09T11:00:00Z"));
        if (roles != null) {
            construction.claim("roles", roles);
        }
        return construction.build();
    }

    @Test
    void lit_les_roles_connus_et_ignore_les_autres() {
        assertThat(JetonRoles.depuis(jeton(List.of("ADMIN", "ROOT")))).containsExactly(Role.ADMIN);
        assertThat(JetonRoles.depuis(jeton(List.of("CLIENT")))).containsExactly(Role.CLIENT);
    }

    @Test
    void un_jeton_sans_revendication_roles_n_a_aucun_role() {
        assertThat(JetonRoles.depuis(jeton(null))).isEmpty();
    }
}
