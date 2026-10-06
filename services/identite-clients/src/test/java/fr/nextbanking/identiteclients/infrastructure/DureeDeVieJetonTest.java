package fr.nextbanking.identiteclients.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Durée de vie du jeton : 1 800 s par défaut, 3 600 s en profil {@code rejeu}. */
@Tag("FR-001-10")
@Tag("FR-030-05")
class DureeDeVieJetonTest {

    @Test
    void la_duree_de_vie_est_de_1800_secondes_par_defaut() throws IOException {
        assertThat(duree("/application.properties")).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    void la_duree_de_vie_est_de_3600_secondes_en_profil_rejeu() throws IOException {
        assertThat(duree("/application-rejeu.properties")).isEqualTo(Duration.ofSeconds(3600));
    }

    private static Duration duree(String ressource) throws IOException {
        Properties proprietes = new Properties();
        try (InputStream flux = DureeDeVieJetonTest.class.getResourceAsStream(ressource)) {
            assertThat(flux).as(ressource).isNotNull();
            proprietes.load(flux);
        }
        return Duration.parse(proprietes.getProperty("nb.jeton.duree-de-vie"));
    }
}
