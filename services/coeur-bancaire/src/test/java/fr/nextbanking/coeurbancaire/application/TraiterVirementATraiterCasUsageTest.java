package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter.Issue;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-023-01")
class TraiterVirementATraiterCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final TraiterVirementATraiterCasUsage cas =
            new TraiterVirementATraiterCasUsage(memoire, memoire, memoire, memoire);

    @Test
    @Tag("FR-023-03")
    void un_virement_deja_execute_n_est_pas_debite_a_nouveau_et_n_emet_rien() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, new BigDecimal("2030.00")));
        memoire.virements.put(12L, new Virement(12, 7, 1, 2, new BigDecimal("100.00"), "loyer",
                EtatVirement.EXECUTE, StatutAffichage.SUCCESS, MemoireCoeurBancaire.MAINTENANT));

        Issue issue = cas.traiter(UUID.randomUUID(), 12);

        assertThat(issue).isEqualTo(Issue.DEJA_EXECUTE);
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("2030.00");
        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
    }

    @Test
    void un_message_livre_deux_fois_n_a_qu_un_effet() {
        UUID id = UUID.randomUUID();

        assertThat(cas.traiter(id, 12)).isEqualTo(Issue.VIREMENT_INCONNU);
        assertThat(cas.traiter(id, 12)).isEqualTo(Issue.DEJA_TRAITE);
    }

    @Test
    void refuse_un_identifiant_d_evenement_absent() {
        assertThatThrownBy(() -> cas.traiter(null, 12)).isInstanceOf(IllegalArgumentException.class);
    }
}
