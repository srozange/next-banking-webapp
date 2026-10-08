package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
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
 * Comptes sur une vraie base : le débit est une mise à jour conditionnelle {@code solde >= montant}, le verrouillage
 * ne rend que les comptes qui existent, dans l'ordre de leur identifiant.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-003-06")
@Tag("BUG-008")
class ComptesJdbcIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Comptes comptes;

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
    void le_debit_conditionnel_refuse_un_montant_superieur_au_solde_et_laisse_le_solde_intact() {
        assertThat(comptes.debiter(new CompteId(1), Montant.lire("9999.00"))).isFalse();
        assertThat(solde(1)).isEqualByComparingTo("2130.00");

        assertThat(comptes.debiter(new CompteId(1), Montant.lire("2130.00"))).isTrue();
        assertThat(solde(1)).isEqualByComparingTo("0.00");
        assertThat(comptes.debiter(new CompteId(1), Montant.lire("0.01"))).isFalse();
        assertThat(solde(1)).isEqualByComparingTo("0.00");
    }

    @Test
    void le_credit_ajoute_au_solde() {
        comptes.crediter(new CompteId(2), Montant.lire("0.50"));

        assertThat(solde(2)).isEqualByComparingTo("15200.50");
    }

    @Test
    void le_verrouillage_rend_les_comptes_existants_par_identifiant_et_la_liste_d_un_client() {
        assertThat(comptes.verrouiller(2, 1)).extracting(Compte::id).containsExactly(1L, 2L);
        assertThat(comptes.verrouiller(1, 999)).extracting(Compte::id).containsExactly(1L);
        assertThat(comptes.duClient(1)).extracting(Compte::numero).containsExactly("FR76-0001-0001", "FR76-0001-0002");
        assertThat(comptes.duClient(10)).isEmpty();
    }

    @Test
    @Tag("FR-005-04")
    void un_compte_se_lit_par_identifiant_avec_son_solde_s_il_existe() {
        assertThat(comptes.trouver(1)).hasValueSatisfying(compte -> {
            assertThat(compte.numero()).isEqualTo("FR76-0001-0001");
            assertThat(compte.clientId()).isEqualTo(1L);
            assertThat(compte.solde().texte()).isEqualTo("2130.00");
        });
        assertThat(comptes.trouver(999)).isEmpty();
    }

    private BigDecimal solde(long id) {
        return jdbc.sql("select solde from comptes where id = :id").param("id", id).query(BigDecimal.class).single();
    }
}
