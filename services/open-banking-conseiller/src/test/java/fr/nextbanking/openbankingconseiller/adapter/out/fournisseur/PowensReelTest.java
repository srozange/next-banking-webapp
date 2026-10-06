package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import fr.nextbanking.openbankingconseiller.domain.UtilisateurPowens;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.RestClient;

/** Adaptateur réel contre un bouchon local : authentification, lecture, pannes, délais, coupe-circuit. */
@ExtendWith(OutputCaptureExtension.class)
class PowensReelTest {

    private static final String SECRET = "secret-de-test";
    private static final String JETON = "jeton-de-test";

    private final BouchonPowens bouchon = new BouchonPowens();

    @AfterEach
    void arreter() {
        bouchon.close();
    }

    private PowensReel reel(int seuil, Duration delai) {
        ProprietesPowens proprietes = new ProprietesPowens("reel", null, null, null, delai, seuil,
                Duration.ofMinutes(10));
        RestClient client = RestClient.builder().requestFactory(ConfigurationPowens.fabrique(delai)).build();
        return new PowensReel(client, bouchon.base(), "demi-sandbox.biapi.pro", "99961298", SECRET,
                "https://app/retour", "59", proprietes);
    }

    @Test
    @Tag("FR-028-01")
    void l_utilisateur_est_cree_en_basic_et_le_jeton_lu() {
        bouchon.repondre(200, "{\"auth_token\":\"" + JETON + "\",\"id_user\":42,\"type\":\"permanent\"}");

        UtilisateurPowens utilisateur = reel(5, Duration.ofSeconds(2)).creerUtilisateur();

        assertThat(utilisateur).isEqualTo(new UtilisateurPowens(42, JETON));
        assertThat(bouchon.requetes()).singleElement().satisfies(requete -> {
            assertThat(requete.methode()).isEqualTo("POST");
            assertThat(requete.chemin()).isEqualTo("/2.0/auth/init");
            assertThat(requete.autorisation()).isEqualTo("Basic " + Base64.getEncoder()
                    .encodeToString(("99961298:" + SECRET).getBytes(StandardCharsets.UTF_8)));
        });
    }

    @Test
    @Tag("FR-028-01")
    void une_reponse_sans_jeton_rend_un_utilisateur_sans_jeton() {
        bouchon.repondre(200, "{\"id_user\":42}");

        assertThat(reel(5, Duration.ofSeconds(2)).creerUtilisateur()).isEqualTo(new UtilisateurPowens(42, null));
        bouchon.repondre(200, "{\"auth_token\":null}");
        assertThat(reel(5, Duration.ofSeconds(2)).creerUtilisateur().jeton()).isNull();
    }

    @Test
    @Tag("FR-028-01")
    @Tag("FR-028-02")
    void les_autres_appels_s_authentifient_avec_le_jeton_permanent() {
        PowensReel powens = reel(5, Duration.ofSeconds(2));
        bouchon.repondre(200, "{\"code\":\"code-1\"}");
        assertThat(powens.codeTemporaire(JETON)).isEqualTo("code-1");
        bouchon.repondre(200, "{\"code\":null}");
        assertThat(powens.codeTemporaire(JETON)).isNull();
        bouchon.repondre(200, "{\"accounts\":[]}");
        powens.comptes(JETON);
        bouchon.repondre(200, "{\"transactions\":[]}");
        powens.transactions(JETON, 12);

        assertThat(bouchon.requetes()).extracting(BouchonPowens.Requete::chemin).containsExactly(
                "/2.0/auth/token/code", "/2.0/auth/token/code", "/2.0/users/me/accounts",
                "/2.0/users/me/accounts/12/transactions?limit=20");
        assertThat(bouchon.requetes()).extracting(BouchonPowens.Requete::autorisation)
                .containsOnly("Bearer " + JETON);
    }

    @Test
    @Tag("FR-028-02")
    void l_adresse_de_la_page_web_porte_les_parametres_encodes() {
        assertThat(reel(5, Duration.ofSeconds(2)).adresseConnexion("c o+de", "s.t&"))
                .isEqualTo("https://webview.powens.com/connect?domain=demi-sandbox.biapi.pro&client_id=99961298"
                        + "&redirect_uri=https%3A%2F%2Fapp%2Fretour&code=c+o%2Bde&connector_ids=59&state=s.t%26");
    }

    @Test
    @Tag("FR-028-12")
    void les_comptes_sont_lus_avec_un_solde_absent_distinct_de_zero() {
        bouchon.repondre(200, """
                {"accounts":[
                 {"id":1,"name":"Courant","type":"checking","balance":1250.5,"iban":"FR7630004000031234567890143"},
                 {"id":2,"name":"Livret","type":"savings","balance":null},
                 {"id":3,"name":"Carte","type":"card"},
                 {"id":4,"name":"Zero","type":"checking","balance":0}]}
                """);

        List<CompteExterne> comptes = reel(5, Duration.ofSeconds(2)).comptes(JETON);

        assertThat(comptes).containsExactly(
                new CompteExterne(1, "Courant", "checking", new BigDecimal("1250.50"), "FR7630004000031234567890143"),
                new CompteExterne(2, "Livret", "savings", null, ""),
                new CompteExterne(3, "Carte", "card", null, ""),
                new CompteExterne(4, "Zero", "checking", new BigDecimal("0.00"), ""));
    }

    @Test
    @Tag("FR-028-12")
    void les_transactions_sont_limitees_a_vingt_avec_le_libelle_simplifie_sinon_d_origine() {
        String lignes = String.join(",", IntStream.rangeClosed(1, 25).mapToObj(numero ->
                "{\"id\":" + numero + ",\"date\":\"2026-10-01\",\"value\":-1.5,\"simplified_wording\":\""
                        + (numero == 1 ? "Simple" : numero == 2 ? "" : "S" + numero)
                        + "\",\"original_wording\":\"Origine " + numero + "\"}").toList());
        bouchon.repondre(200, "{\"transactions\":[" + lignes + "]}");

        List<TransactionExterne> transactions = reel(5, Duration.ofSeconds(2)).transactions(JETON, 1);

        assertThat(transactions).hasSize(20);
        assertThat(transactions.get(0)).isEqualTo(
                new TransactionExterne(1, LocalDate.of(2026, 10, 1), "Simple", new BigDecimal("-1.50")));
        assertThat(transactions.get(1).libelle()).isEqualTo("Origine 2");
        assertThat(transactions.get(2).libelle()).isEqualTo("S3");
    }

    @Test
    @Tag("FR-028-12")
    void un_transaction_sans_libelle_simplifie_du_tout_prend_le_libelle_d_origine() {
        bouchon.repondre(200, "{\"transactions\":[{\"id\":1,\"date\":\"2026-10-01\",\"value\":2,"
                + "\"original_wording\":\"Origine\"}]}");

        assertThat(reel(5, Duration.ofSeconds(2)).transactions(JETON, 1)).singleElement()
                .extracting(TransactionExterne::libelle).isEqualTo("Origine");
    }

    @Test
    @Tag("FR-028-12")
    void un_corps_de_forme_inattendue_est_une_indisponibilite_pas_une_liste_vide() {
        PowensReel powens = reel(50, Duration.ofSeconds(2));
        for (String corps : new String[] {"pas du json", "[1]", "{\"accounts\":[{\"name\":\"sans id\"}]}",
                "{\"accounts\":[{\"id\":1,\"balance\":\"abc\"}]}"}) {
            bouchon.repondre(200, corps);
            assertThatThrownBy(() -> powens.comptes(JETON)).as(corps).isInstanceOf(PowensIndisponible.class);
        }
        bouchon.repondre(200, "{\"transactions\":[{\"id\":1,\"date\":\"pasunedate\",\"value\":1}]}");
        assertThatThrownBy(() -> powens.transactions(JETON, 1)).isInstanceOf(PowensIndisponible.class);
    }

    @Test
    @Tag("FR-028-12")
    void un_statut_5xx_ou_404_est_une_indisponibilite_sans_secret_dans_les_journaux(CapturedOutput sortie) {
        PowensReel powens = reel(50, Duration.ofSeconds(2));
        for (int statut : new int[] {500, 503, 404}) {
            bouchon.repondre(statut, "{\"description\":\"" + SECRET + JETON + "\"}");

            assertThatThrownBy(() -> powens.comptes(JETON)).isInstanceOf(PowensIndisponible.class)
                    .hasMessage("statut " + statut);
            assertThatThrownBy(() -> powens.transactions(JETON, 1)).isInstanceOf(PowensIndisponible.class);
        }
        assertThat(sortie.getAll()).contains("Powens indisponible").doesNotContain(SECRET).doesNotContain(JETON);
    }

    @Test
    @Tag("FR-028-12")
    void le_delai_de_lecture_est_applique_et_la_panne_est_une_indisponibilite(CapturedOutput sortie) {
        bouchon.repondreApres(Duration.ofSeconds(3), "{\"accounts\":[]}");
        PowensReel powens = reel(5, Duration.ofMillis(300));

        long debut = System.nanoTime();
        assertThatThrownBy(() -> powens.comptes(JETON)).isInstanceOf(PowensIndisponible.class)
                .hasMessage("délai dépassé");

        assertThat(Duration.ofNanos(System.nanoTime() - debut)).isLessThan(Duration.ofSeconds(2));
        assertThat(sortie.getAll()).doesNotContain(JETON);
    }

    @Test
    @Tag("FR-028-12")
    void une_erreur_de_connexion_est_une_indisponibilite() {
        bouchon.close();
        PowensReel powens = reel(5, Duration.ofSeconds(1));

        assertThatThrownBy(() -> powens.comptes(JETON)).isInstanceOf(PowensIndisponible.class)
                .hasMessageStartingWith("erreur d'appel");
    }

    @Test
    @Tag("FR-028-12")
    void un_coupe_circuit_ouvert_est_une_indisponibilite_sans_nouvel_appel() {
        PowensReel powens = reel(3, Duration.ofSeconds(2));
        bouchon.repondre(500, "{}");
        for (int appel = 0; appel < 3; appel++) {
            assertThatThrownBy(() -> powens.comptes(JETON)).hasMessage("statut 500");
        }
        int avant = bouchon.requetes().size();

        assertThatThrownBy(() -> powens.comptes(JETON)).isInstanceOf(PowensIndisponible.class)
                .hasMessage("coupe-circuit ouvert");
        assertThatThrownBy(() -> powens.transactions(JETON, 1)).isInstanceOf(PowensIndisponible.class);
        assertThat(bouchon.requetes()).hasSize(avant);
    }
}
