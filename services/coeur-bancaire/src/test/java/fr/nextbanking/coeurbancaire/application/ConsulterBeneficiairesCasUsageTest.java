package fr.nextbanking.coeurbancaire.application;

import static fr.nextbanking.coeurbancaire.application.BeneficiairesTestes.beneficiaire;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterBeneficiairesCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterBeneficiairesCasUsage cas = new ConsulterBeneficiairesCasUsage(memoire);

    private void ajouter(BeneficiaireExterne beneficiaire) {
        memoire.beneficiairesExternes.put(beneficiaire.id(), beneficiaire);
    }

    @Test
    @Tag("FR-013-02")
    void seuls_les_beneficiaires_du_client_sont_rendus_tries_par_libelle() {
        ajouter(beneficiaire(1, 7, "Zoe"));
        ajouter(beneficiaire(2, 7, "Alice"));
        ajouter(beneficiaire(3, 8, "Autre client"));
        ajouter(beneficiaire(4, 7, "Marc"));

        assertThat(cas.beneficiaires(7)).extracting(BeneficiaireExterne::libelle)
                .containsExactly("Alice", "Marc", "Zoe");
    }

    @Test
    @Tag("FR-013-02")
    void a_libelle_egal_l_identifiant_departage() {
        ajouter(beneficiaire(9, 7, "Loyer"));
        ajouter(beneficiaire(3, 7, "Loyer"));

        assertThat(cas.beneficiaires(7)).extracting(BeneficiaireExterne::id).containsExactly(3L, 9L);
    }

    @Test
    @Tag("FR-013-02")
    void un_client_sans_beneficiaire_actif_a_une_liste_vide() {
        ajouter(beneficiaire(3, 8, "Autre client"));

        assertThat(cas.beneficiaires(7)).isEmpty();
    }
}
