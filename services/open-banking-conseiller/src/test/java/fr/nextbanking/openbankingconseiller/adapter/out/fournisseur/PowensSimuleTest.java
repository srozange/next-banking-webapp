package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.openbankingconseiller.adapter.out.fournisseur.PowensSimule.ModeErreur;
import fr.nextbanking.openbankingconseiller.application.PowensIndisponible;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import java.math.BigDecimal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Adaptateur simulé : réponses fixes et modes d'erreur sélectionnables (AR-61). */
@Tag("FR-028-01")
@Tag("FR-028-12")
class PowensSimuleTest {

    private final PowensSimule powens = new PowensSimule("http://app/retour");

    @Test
    void expose_les_connecteurs_bnp_paribas_et_credit_agricole() {
        assertThat(powens.connecteurs()).contains("BNP Paribas", "Credit Agricole");
    }

    @Test
    void cree_un_utilisateur_avec_un_jeton_et_rend_un_code_fixe() {
        assertThat(powens.creerUtilisateur().jeton()).isNotBlank();
        assertThat(powens.codeTemporaire("x")).isEqualTo(powens.codeTemporaire("y")).isNotBlank();
    }

    @Test
    void l_adresse_est_celle_du_retour_avec_un_connection_id_fixe_et_le_state() {
        assertThat(powens.adresseConnexion("code", "a.b c"))
                .isEqualTo("http://app/retour?connection_id=1&state=a.b+c");
    }

    @Test
    void comptes_et_transactions_sont_fixes() {
        assertThat(powens.comptes("j")).hasSize(2).isEqualTo(powens.comptes("j"));
        assertThat(powens.transactions("j", 101)).isNotEmpty();
    }

    @Test
    void le_refus_de_creation_et_la_reponse_sans_jeton_sont_selectionnables() {
        powens.mode(ModeErreur.REFUS_CREATION);
        assertThatThrownBy(powens::creerUtilisateur).isInstanceOf(PowensIndisponible.class);
        powens.mode(ModeErreur.REPONSE_SANS_JETON);
        assertThat(powens.creerUtilisateur().jeton()).isNull();
    }

    @Test
    void les_pannes_aux_comptes_et_aux_transactions_sont_selectionnables() {
        for (ModeErreur mode : new ModeErreur[] {ModeErreur.COMPTES_500, ModeErreur.COMPTES_503}) {
            powens.mode(mode);
            assertThatThrownBy(() -> powens.comptes("j")).isInstanceOf(PowensIndisponible.class)
                    .hasMessage("statut " + mode.name().substring(8));
        }
        for (ModeErreur mode : new ModeErreur[] {ModeErreur.TRANSACTIONS_404, ModeErreur.TRANSACTIONS_500}) {
            powens.mode(mode);
            assertThatThrownBy(() -> powens.transactions("j", 101)).isInstanceOf(PowensIndisponible.class);
        }
    }

    @Test
    void les_comptes_sans_solde_au_solde_nul_ou_absents_sont_selectionnables() {
        powens.mode(ModeErreur.COMPTE_SANS_SOLDE);
        assertThat(powens.comptes("j")).singleElement().extracting(CompteExterne::solde).isNull();
        powens.mode(ModeErreur.COMPTE_SOLDE_NUL);
        assertThat(powens.comptes("j")).singleElement().extracting(CompteExterne::solde)
                .isEqualTo(new BigDecimal("0.00"));
        powens.mode(ModeErreur.AUCUN_COMPTE);
        assertThat(powens.comptes("j")).isEmpty();
    }
}
