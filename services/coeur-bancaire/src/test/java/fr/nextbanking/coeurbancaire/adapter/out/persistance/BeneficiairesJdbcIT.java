package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.DemandeBeneficiaire;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Bénéficiaires sur une vraie base : liste des actifs du client triée par libellé, lecture par identifiant, et
 * désactivation conditionnelle qui conserve la ligne.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-013-02")
@Tag("FR-013-09")
@Tag("FR-013-07")
class BeneficiairesJdbcIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Beneficiaires beneficiaires;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource source;

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @Test
    void la_liste_ne_rend_que_les_actifs_du_client_par_libelle() {
        ajouter(1, "Zoe", true);
        ajouter(1, "Ancien", false);
        ajouter(8, "Autre", true);

        assertThat(beneficiaires.actifsDuClient(1)).extracting(BeneficiaireExterne::libelle)
                .containsExactly("Loyer", "Zoe");
        assertThat(beneficiaires.actifsDuClient(8)).extracting(BeneficiaireExterne::libelle).containsExactly("Autre");
        assertThat(beneficiaires.actifsDuClient(99)).isEmpty();
    }

    @Test
    void la_lecture_par_identifiant_ne_rend_que_les_actifs() {
        long inactif = ajouter(1, "Ancien", false);

        BeneficiaireExterne loyer = beneficiaires.trouverActif(1).orElseThrow();
        assertThat(loyer.libelle()).isEqualTo("Loyer");
        assertThat(loyer.clientId()).isEqualTo(1);
        assertThat(loyer.iban().enClair()).isEqualTo("FR7630006000011234567890189");
        assertThat(loyer.bic()).isEqualTo("AGRIFRPP");
        assertThat(loyer.banque()).isEqualTo("Banque Exemple");
        assertThat(beneficiaires.trouverActif(inactif)).isEmpty();
        assertThat(beneficiaires.trouverActif(999)).isEmpty();
    }

    @Test
    void la_desactivation_est_conditionnelle_au_client_et_a_l_etat_et_conserve_la_ligne() {
        assertThat(beneficiaires.desactiver(1, 8)).isFalse();
        assertThat(beneficiaires.estActif(1, "FR7630006000011234567890189")).isTrue();

        assertThat(beneficiaires.desactiver(1, 1)).isTrue();
        assertThat(beneficiaires.desactiver(1, 1)).isFalse();

        assertThat(beneficiaires.estActif(1, "FR7630006000011234567890189")).isFalse();
        assertThat(jdbc.sql("select count(*) from beneficiaires_externes where id = 1").query(Long.class).single())
                .isEqualTo(1L);
    }

    @Test
    void l_insertion_enregistre_un_beneficiaire_actif_avec_sa_zone_et_sa_date() {
        Instant creeLe = Instant.parse("2026-11-16T23:00:00Z");

        BeneficiaireExterne cree = beneficiaires.ajouter(1, DemandeBeneficiaire.valider("Istanbul", "Titulaire",
                "tr33 0006 1005 1978 6457 8413 26", "abcdtr2a", " Banque "), creeLe);

        assertThat(beneficiaires.trouverActif(cree.id())).contains(cree);
        assertThat(cree.banque()).isEqualTo("Banque");
        var ligne = jdbc.sql("select zone_sepa, actif, cree_le from beneficiaires_externes where id = :id")
                .param("id", cree.id()).query().singleRow();
        assertThat(ligne.get("zone_sepa")).isEqualTo(false);
        assertThat(ligne.get("actif")).isEqualTo(true);
        assertThat(((java.sql.Timestamp) ligne.get("cree_le")).toInstant()).isEqualTo(creeLe);
        BeneficiaireExterne sansOptions = beneficiaires.ajouter(1, DemandeBeneficiaire.valider("Sans", "Titulaire",
                "FR3530004000000123456789010", null, null), creeLe);
        assertThat(beneficiaires.trouverActif(sansOptions.id()).orElseThrow().bic()).isNull();
    }

    private long ajouter(long clientId, String libelle, boolean actif) {
        return jdbc.sql("""
                insert into beneficiaires_externes (client_id, titulaire, iban, libelle, zone_sepa, actif, cree_le)
                values (:client, 'Titulaire', 'DE89370400440532013000', :libelle, true, :actif,
                    '2026-10-01T08:00:00Z')
                returning id""")
                .param("client", clientId).param("libelle", libelle).param("actif", actif)
                .query(Long.class).single();
    }
}
