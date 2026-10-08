package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-006-03")
class CategorieBudgetTest {

    @Test
    void les_categories_sont_dans_l_ordre_de_recherche_avec_leur_libelle() {
        assertThat(CategorieBudget.values()).extracting(CategorieBudget::libelle).containsExactly("Logement",
                "Alimentation", "Transport", "Sante", "Loisirs", "Abonnements", "Epargne", "Revenus", "Assurances",
                "Autres");
        assertThat(CategorieBudget.AUTRES.motsCles()).isEmpty();
        assertThat(CategorieBudget.REVENUS.motsCles()).containsExactly("salaire", "remboursement");
    }

    @Test
    void un_libelle_va_a_la_categorie_dont_un_mot_cle_figure_sans_tenir_compte_de_la_casse() {
        assertThat(CategorieBudget.deLibelle("LOYER mars")).isEqualTo(CategorieBudget.LOGEMENT);
        assertThat(CategorieBudget.deLibelle("Carrefour Market")).isEqualTo(CategorieBudget.ALIMENTATION);
        assertThat(CategorieBudget.deLibelle("sncf billet")).isEqualTo(CategorieBudget.TRANSPORT);
        assertThat(CategorieBudget.deLibelle("Salaire octobre")).isEqualTo(CategorieBudget.REVENUS);
        assertThat(CategorieBudget.deLibelle("AXA auto")).isEqualTo(CategorieBudget.ASSURANCES);
    }

    @Test
    void les_accents_sont_ignores() {
        assertThat(CategorieBudget.deLibelle("Intermarché")).isEqualTo(CategorieBudget.ALIMENTATION);
        assertThat(CategorieBudget.deLibelle("Hôpital")).isEqualTo(CategorieBudget.SANTE);
        assertThat(CategorieBudget.deLibelle("Épargne mensuelle")).isEqualTo(CategorieBudget.EPARGNE);
    }

    @Test
    void la_premiere_categorie_dans_l_ordre_l_emporte_et_sinon_autres() {
        assertThat(CategorieBudget.deLibelle("loyer assurance")).isEqualTo(CategorieBudget.LOGEMENT);
        assertThat(CategorieBudget.deLibelle("xyz")).isEqualTo(CategorieBudget.AUTRES);
        assertThat(CategorieBudget.deLibelle("")).isEqualTo(CategorieBudget.AUTRES);
    }

    @Test
    @Tag("FR-006-06")
    @Tag("BUG-023")
    void toutes_les_categories_sont_plafonnables_sauf_revenus() {
        for (CategorieBudget categorie : CategorieBudget.values()) {
            assertThat(categorie.estPlafonnable()).isEqualTo(categorie != CategorieBudget.REVENUS);
        }
    }

    @Test
    @Tag("FR-006-06")
    void une_categorie_se_retrouve_par_son_libelle_affiche_sans_tenir_compte_de_la_casse() {
        assertThat(CategorieBudget.deLibelleAffiche("Alimentation")).isEqualTo(CategorieBudget.ALIMENTATION);
        assertThat(CategorieBudget.deLibelleAffiche(" sante ")).isEqualTo(CategorieBudget.SANTE);
        assertThat(CategorieBudget.deLibelleAffiche("Inconnue")).isNull();
        assertThat(CategorieBudget.deLibelleAffiche(null)).isNull();
    }
}
