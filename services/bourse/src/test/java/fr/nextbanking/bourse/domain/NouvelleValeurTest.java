package fr.nextbanking.bourse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.bourse.domain.AjoutRefuse.Motif;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class NouvelleValeurTest {

    @Test
    @Tag("FR-009-12")
    @Tag("FR-029-10")
    void normalise_symbole_nom_marche_et_devise() {
        NouvelleValeur valeur = NouvelleValeur.depuis("  sg.pa ", " Société Générale ", " Euronext Paris ", " eur ");

        assertThat(valeur).isEqualTo(new NouvelleValeur("SG.PA", "Société Générale", "Euronext Paris", "EUR"));
    }

    @Test
    @Tag("FR-009-12")
    void la_majuscule_ne_depend_pas_de_la_langue_du_systeme() {
        assertThat(NouvelleValeur.depuis("ibm", "IBM", "NYSE", "usd").symbole()).isEqualTo("IBM");
    }

    @Test
    @Tag("FR-009-11")
    @Tag("FR-029-09")
    void refuse_un_symbole_absent_vide_ou_blanc_avant_tout_autre_controle() {
        for (String symbole : new String[] {null, "", "   "}) {
            refuse(() -> NouvelleValeur.depuis(symbole, null, "Bourse de Tokyo", "JPY"), Motif.SYMBOLE_REQUIS);
        }
    }

    @Test
    @Tag("FR-009-11")
    @Tag("FR-029-09")
    void refuse_un_nom_absent_vide_ou_blanc_avant_les_longueurs_et_les_listes() {
        for (String nom : new String[] {null, "", "  "}) {
            refuse(() -> NouvelleValeur.depuis("X".repeat(21), nom, "Bourse de Tokyo", "JPY"), Motif.NOM_REQUIS);
        }
    }

    @Test
    @Tag("FR-029-13")
    void refuse_un_symbole_de_plus_de_20_caracteres_ou_un_nom_de_plus_de_100_apres_rognage() {
        refuse(() -> NouvelleValeur.depuis("X".repeat(21), "Nom", "NYSE", "USD"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("X", "N".repeat(101), "NYSE", "USD"), Motif.REQUETE_INVALIDE);

        NouvelleValeur limites = NouvelleValeur.depuis(" " + "x".repeat(20) + " ", " " + "N".repeat(100) + " ",
                "NYSE", "USD");
        assertThat(limites.symbole()).hasSize(20);
        assertThat(limites.nom()).hasSize(100);
    }

    @Test
    @Tag("FR-029-14")
    void n_accepte_que_les_marches_et_devises_autorises() {
        for (String marche : NouvelleValeur.MARCHES) {
            for (String devise : NouvelleValeur.DEVISES) {
                NouvelleValeur valeur = NouvelleValeur.depuis("ABC", "Abc", marche, devise.toLowerCase());
                assertThat(valeur.marche()).isEqualTo(marche);
                assertThat(valeur.devise()).isEqualTo(devise);
            }
        }
        assertThat(NouvelleValeur.MARCHES).containsExactlyInAnyOrder("Euronext Paris", "NASDAQ", "NYSE", "LSE",
                "Autre");
        assertThat(NouvelleValeur.DEVISES).containsExactlyInAnyOrder("EUR", "USD", "GBP");
    }

    @Test
    @Tag("FR-029-14")
    void refuse_un_marche_ou_une_devise_hors_liste_ou_absents() {
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "Bourse de Tokyo", "EUR"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "London SE", "GBP"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "M".repeat(51), "EUR"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", null, "EUR"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "NYSE", "JPY"), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "NYSE", "E".repeat(11)), Motif.REQUETE_INVALIDE);
        refuse(() -> NouvelleValeur.depuis("ABC", "Abc", "NYSE", null), Motif.REQUETE_INVALIDE);
    }

    private static void refuse(Executable creation, Motif motif) {
        assertThatThrownBy(creation::execute)
                .isInstanceOfSatisfying(AjoutRefuse.class, refus -> {
                    assertThat(refus.motif()).isEqualTo(motif);
                    assertThat(refus.symbole()).isNull();
                });
    }
}
