package fr.nextbanking.credits.adapter;

import static fr.nextbanking.credits.adapter.OutilsHttp.avecJeton;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Liste des emprunts : persistance JDBC (Testcontainers) et {@code GET /api/credits/prets} sur les données de
 * référence ; seuls les prêts du client du jeton, date de début décroissante.
 */
@SpringBootTest(classes = ApplicationCredits.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Tag("FR-007-08")
class PretsHttpIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        // Ces tests constatent le prêt EN_ATTENTE : le scoring (écouteur) ne doit pas le décider.
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private PretRepository prets;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    @Test
    void user1_voit_ses_deux_prets_en_cours_du_plus_recent_au_plus_ancien() {
        List<Pret> liste = prets.parClient(1L);

        assertThat(liste).extracting(Pret::id).containsExactly(2L, 1L);
        assertThat(liste).extracting(Pret::statut).containsOnly(StatutPret.EN_COURS);
        assertThat(liste).extracting(Pret::type).containsExactly(TypePret.TRAVAUX, TypePret.AUTO);
        assertThat(liste).allSatisfy(pret -> {
            assertThat(pret.clientId()).isEqualTo(1L);
            assertThat(pret.montant().scale()).isEqualTo(2);
            assertThat(pret.dateFin()).isAfter(pret.dateDebut());
        });
        assertThat(liste.get(0).dateDebut()).isAfter(liste.get(1).dateDebut());
    }

    @Test
    void un_client_sans_pret_a_une_liste_vide() {
        assertThat(prets.parClient(3L)).isEmpty();
        assertThat(prets.parId(999L)).isEmpty();
    }

    @Test
    void la_liste_http_ne_contient_que_les_prets_du_jeton_au_format_du_back() throws Exception {
        HttpResponse<String> reponse = avecJeton(port, "GET", "/api/credits/prets", OutilsHttp.jetonValide("1"));

        assertThat(reponse.statusCode()).isEqualTo(200);
        assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("application/json"));
        assertThat(reponse.body()).startsWith("[{\"id\":2,\"type\":\"TRAVAUX\",\"montant\":\"5000.00\","
                + "\"tauxAnnuel\":\"4.9000\",\"dureeMois\":24,");
        assertThat(reponse.body()).contains("\"id\":1,\"type\":\"AUTO\",\"montant\":\"12000.00\"",
                "\"statut\":\"EN_COURS\"");
        assertThat(reponse.body().split("\"id\"", -1)).hasSize(3);

        HttpResponse<String> autre = avecJeton(port, "GET", "/api/credits/prets", OutilsHttp.jetonValide("3"));
        assertThat(autre.statusCode()).isEqualTo(200);
        assertThat(autre.body()).isEqualTo("[]");
    }
}
