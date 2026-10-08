package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterVirementsProgrammesCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterVirementsProgrammesCasUsage cas =
            new ConsulterVirementsProgrammesCasUsage(memoire, memoire, memoire);

    private void ajouter(long id, long client, long source, String prochaine) {
        memoire.virementsProgrammes.add(new VirementProgramme(id, client, source, "FR76-0001-0002", "Epargne",
                Montant.lire("35"), FrequenceVirement.MENSUEL, null, Instant.parse(prochaine), true, 0,
                MemoireCoeurBancaire.MAINTENANT));
    }

    @Test
    @Tag("FR-004-03")
    void seuls_les_virements_du_client_sont_rendus_tries_par_prochaine_execution_puis_identifiant() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        ajouter(1, 7, 1, "2027-03-01T23:00:00Z");
        ajouter(2, 8, 3, "2027-01-01T23:00:00Z");
        ajouter(3, 7, 1, "2027-01-01T23:30:00Z");
        ajouter(4, 7, 1, "2027-01-01T23:30:00Z");

        assertThat(cas.duClient(7)).extracting(LigneVirementProgramme::id).containsExactly(3L, 4L, 1L);
    }

    @Test
    @Tag("FR-004-03")
    void la_ligne_porte_le_numero_du_compte_source_et_la_date_du_fuseau_metier() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        ajouter(1, 7, 1, "2026-12-31T23:30:00Z");

        LigneVirementProgramme ligne = cas.duClient(7).getFirst();

        assertThat(ligne.numeroCompteSource()).isEqualTo("FR76-0001-0001");
        assertThat(ligne.prochaineExecution()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(ligne.montant().texte()).isEqualTo("35.00");
        assertThat(ligne.actif()).isTrue();
    }

    @Test
    @Tag("FR-004-03")
    void un_client_sans_virement_programme_a_une_liste_vide_et_un_compte_inconnu_donne_un_numero_vide() {
        assertThat(cas.duClient(7)).isEmpty();

        ajouter(1, 7, 99, "2027-01-01T00:00:00Z");

        assertThat(cas.duClient(7).getFirst().numeroCompteSource()).isEmpty();
    }
}
