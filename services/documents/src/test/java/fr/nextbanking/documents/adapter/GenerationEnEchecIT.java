package fr.nextbanking.documents.adapter;

import static fr.nextbanking.documents.adapter.OutilsHttp.avecJeton;
import static fr.nextbanking.documents.adapter.OutilsHttp.jetonValide;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.infrastructure.ApplicationDocuments;
import fr.nextbanking.documents.infrastructure.Conteneurs;
import java.net.http.HttpResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Panne de l'adaptateur PDF : 500 au texte unique, sans message d'exception, trace ni nom de composant. */
@SpringBootTest(classes = {ApplicationDocuments.class, ConfigurationHorlogeFixe.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class GenerationEnEchecIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @MockitoBean
    private GenerateurPdf generateur;

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
        when(generateur.generer(any(), any())).thenThrow(
                new IllegalStateException("détail secret : com.lowagie.text.DocumentException FR76-0001-0001"));
    }

    @Test
    @Tag("FR-017-10")
    @Tag("BUG-059")
    void une_panne_de_generation_rend_500_au_texte_unique_et_n_incremente_rien() throws Exception {
        long avant = jdbc.sql("select sum(download_count) from documents").query(Long.class).single();

        for (String type : new String[] {"RIB", "ATTESTATION"}) {
            HttpResponse<String> reponse = avecJeton(port, "GET",
                    "/api/documents/telechargement?type=" + type + "&compteId=1", jetonValide("1"));

            assertThat(reponse.statusCode()).as(type).isEqualTo(500);
            assertThat(reponse.body()).as(type)
                    .isEqualTo("{\"message\":\"Erreur lors de la generation du document\"}");
            assertThat(reponse.headers().firstValue("Content-Type")).hasValueSatisfying(contenu ->
                    assertThat(contenu).startsWith("application/json"));
            assertThat(reponse.body()).doesNotContain("secret", "lowagie", "Exception", "FR76", "at fr.nextbanking");
        }
        assertThat(jdbc.sql("select sum(download_count) from documents").query(Long.class).single()).isEqualTo(avant);
        assertThat(jdbc.sql("select count(*) from documents where document_type in ('RIB', 'ATTESTATION')")
                .query(Long.class).single()).isZero();
    }
}
