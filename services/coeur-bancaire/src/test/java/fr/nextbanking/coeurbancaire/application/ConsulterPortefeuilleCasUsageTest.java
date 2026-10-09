package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ClientEtSesComptes;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Portefeuille du conseiller côté cœur bancaire : clients de la projection affectés à l'acteur, non supprimés, triés
 * par identifiant, avec leurs comptes triés par identifiant et lus en une seule fois.
 */
@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-015-09")
@Tag("FR-030-07")
@Tag("BUG-093")
class ConsulterPortefeuilleCasUsageTest {

    private static final Compte COURANT =
            new Compte(1, 1, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2130.00"));
    private static final Compte EPARGNE =
            new Compte(2, 1, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("15200.00"));
    private static final Compte AUTRE = new Compte(3, 3, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("500.00"));

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final ConsulterPortefeuilleCasUsage cas = new ConsulterPortefeuilleCasUsage(memoire, memoire);

    @BeforeEach
    void preparer() {
        memoire.enregistrerSiPlusRecent(new ClientProjete(2, "Marie Lebrun", 11L, false, 1));
        memoire.enregistrerSiPlusRecent(new ClientProjete(1, "Jean Dupont", 11L, false, 1));
        memoire.enregistrerSiPlusRecent(new ClientProjete(3, "Pierre Bernard", 12L, false, 1));
        memoire.enregistrerSiPlusRecent(new ClientProjete(5, "Supprimé", 11L, true, 1));
        memoire.comptes.put(1L, COURANT);
        memoire.comptes.put(2L, EPARGNE);
        memoire.comptes.put(3L, AUTRE);
    }

    @Test
    void le_conseiller_recoit_ses_clients_tries_avec_leurs_comptes_et_un_client_sans_compte_a_une_liste_vide() {
        List<ClientEtSesComptes> portefeuille = cas.consulter(11L, Set.of(Role.CONSEILLER));

        assertThat(portefeuille).containsExactly(new ClientEtSesComptes(1, List.of(COURANT, EPARGNE)),
                new ClientEtSesComptes(2, List.of()));
        assertThat(memoire.lecturesParLot).containsExactly(List.of(1L, 2L));
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide_sans_lire_de_compte() {
        assertThat(cas.consulter(10L, Set.of(Role.ADMIN))).isEmpty();
        assertThat(memoire.lecturesParLot).isEmpty();
    }

    @Test
    void un_client_est_refuse() {
        assertThatThrownBy(() -> cas.consulter(1L, Set.of(Role.CLIENT))).isInstanceOf(ErreurVirement.class);
        assertThat(memoire.lecturesParLot).isEmpty();
    }

    @Test
    void les_codes_de_type_et_les_soldes_sont_rendus_tels_quels() {
        ClientEtSesComptes jean = cas.consulter(11L, Set.of(Role.CONSEILLER)).getFirst();

        assertThat(jean.comptes()).extracting(compte -> compte.type().name() + " " + compte.solde().texte())
                .containsExactly("COURANT 2130.00", "EPARGNE 15200.00");
    }
}
