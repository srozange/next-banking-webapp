package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-016-03")
class PretAvecClientTest {

    private static final Pret PRET = new Pret(42L, 5L, TypePret.AUTO, new BigDecimal("9000.00"),
            new BigDecimal("3.9000"), 6, new BigDecimal("1517.11"), new BigDecimal("9102.66"),
            new BigDecimal("102.66"), "Voiture", StatutPret.EN_ATTENTE, LocalDate.of(2026, 10, 1),
            LocalDate.of(2027, 4, 1));

    @Test
    void le_nom_complet_de_la_projection_est_retenu() {
        assertThat(PretAvecClient.de(PRET, "Jean Dupont").nomClient()).isEqualTo("Jean Dupont");
    }

    @Test
    void un_client_absent_de_la_projection_se_replie_sur_son_identifiant_en_texte() {
        assertThat(PretAvecClient.de(PRET, null).nomClient()).isEqualTo("5");
    }
}
