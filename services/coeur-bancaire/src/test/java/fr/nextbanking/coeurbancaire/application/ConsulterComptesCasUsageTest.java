package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-003-01")
class ConsulterComptesCasUsageTest {

    @Test
    void rend_les_seuls_comptes_du_client_et_une_liste_vide_sans_compte_propre() {
        MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("10.00")));
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        memoire.comptes.put(3L, new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        ConsulterComptesCasUsage cas = new ConsulterComptesCasUsage(memoire);

        assertThat(cas.comptesDuClient(7)).extracting(Compte::id).containsExactly(1L, 2L);
        assertThat(cas.comptesDuClient(10)).isEmpty();
    }
}
