package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.ErreurPlafond;
import fr.nextbanking.coeurbancaire.domain.ErreurPlafond.Cause;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class GererPlafondsCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final GererPlafondsCasUsage cas = new GererPlafondsCasUsage(memoire);

    private void refuse(String categorie, String mois, String plafond, Cause cause) {
        assertThatThrownBy(() -> cas.enregistrer(7, categorie, mois, plafond)).isInstanceOfSatisfying(
                ErreurPlafond.class, e -> assertThat(e.motif()).isEqualTo(cause));
        assertThat(memoire.plafonds).isEmpty();
    }

    @Test
    @Tag("FR-006-07")
    void l_enregistrement_cree_puis_remplace_le_plafond_unique_du_client_de_la_categorie_et_du_mois() {
        Plafond cree = cas.enregistrer(7, "Alimentation", "2026-10", "500");
        Plafond remplace = cas.enregistrer(7, "Alimentation", "2026-10", "300.5");

        assertThat(cree.montant().texte()).isEqualTo("500.00");
        assertThat(remplace.id()).isEqualTo(cree.id());
        assertThat(remplace.categorie()).isEqualTo(CategorieBudget.ALIMENTATION);
        assertThat(remplace.mois()).isEqualTo("2026-10");
        assertThat(memoire.duMois(7, "2026-10")).singleElement().satisfies(p -> assertThat(p.montant().texte())
                .isEqualTo("300.50"));
    }

    @Test
    @Tag("FR-006-07")
    void un_autre_mois_une_autre_categorie_ou_un_autre_client_donnent_un_autre_plafond() {
        cas.enregistrer(7, "Alimentation", "2026-10", "500");
        cas.enregistrer(7, "Alimentation", "2026-11", "500");
        cas.enregistrer(7, "Autres", "2026-10", "500");
        cas.enregistrer(8, "Alimentation", "2026-10", "500");

        assertThat(memoire.plafonds).hasSize(4);
    }

    @Test
    @Tag("FR-006-06")
    @Tag("BUG-023")
    void la_categorie_revenus_est_refusee_et_aucun_plafond_n_est_cree() {
        refuse("Revenus", "2026-10", "500", Cause.CATEGORIE_INVALIDE);
    }

    @Test
    @Tag("FR-006-06")
    void une_categorie_absente_ou_inconnue_est_refusee() {
        refuse(null, "2026-10", "500", Cause.CATEGORIE_INVALIDE);
        refuse("", "2026-10", "500", Cause.CATEGORIE_INVALIDE);
        refuse("Voitures", "2026-10", "500", Cause.CATEGORIE_INVALIDE);
    }

    @Test
    @Tag("FR-006-05")
    void un_plafond_invalide_ou_trop_grand_est_refuse_sans_creer_de_plafond() {
        refuse("Logement", "2026-10", null, Cause.PLAFOND_INVALIDE);
        refuse("Logement", "2026-10", "0", Cause.PLAFOND_INVALIDE);
        refuse("Logement", "2026-10", "-10", Cause.PLAFOND_INVALIDE);
        refuse("Logement", "2026-10", "abc", Cause.PLAFOND_INVALIDE);
        refuse("Logement", "2026-10", "10000000000000", Cause.PLAFOND_TROP_GRAND);
    }

    @Test
    @Tag("FR-006-07")
    void un_mois_absent_ou_illisible_est_refuse() {
        refuse("Logement", null, "500", Cause.MOIS_INVALIDE);
        refuse("Logement", "octobre", "500", Cause.MOIS_INVALIDE);
        refuse("Logement", "2026-13", "500", Cause.MOIS_INVALIDE);
    }

    @Test
    @Tag("FR-006-08")
    void la_suppression_retire_le_plafond_du_client() {
        Plafond cree = cas.enregistrer(7, "Alimentation", "2026-10", "500");

        cas.supprimer(7, cree.id());

        assertThat(memoire.plafonds).isEmpty();
    }

    @Test
    @Tag("FR-006-08")
    void le_plafond_d_autrui_et_le_plafond_inexistant_donnent_le_meme_refus_sans_rien_supprimer() {
        Plafond cree = cas.enregistrer(7, "Alimentation", "2026-10", "500");

        assertThatThrownBy(() -> cas.supprimer(8, cree.id())).isInstanceOfSatisfying(ErreurPlafond.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));
        assertThatThrownBy(() -> cas.supprimer(7, 9999)).isInstanceOfSatisfying(ErreurPlafond.class,
                e -> assertThat(e.motif()).isEqualTo(Cause.INTROUVABLE));
        assertThat(memoire.plafonds).hasSize(1);
    }
}
