package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-030-07")
class RoleTest {

    @Test
    void reconnait_exactement_les_trois_roles() {
        assertThat(Role.depuis("ADMIN")).contains(Role.ADMIN);
        assertThat(Role.depuis("CLIENT")).contains(Role.CLIENT);
        assertThat(Role.depuis("CONSEILLER")).contains(Role.CONSEILLER);
        assertThat(Role.depuis("admin")).isEmpty();
        assertThat(Role.depuis("ROOT")).isEmpty();
        assertThat(Role.depuis(null)).isEmpty();
    }
}
