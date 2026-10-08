package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * File de revue triée par heure butoir, historique des 50 derniers virements traités (ADMIN seul), suivi des quatre
 * premiers virements passés en revue du client.
 */
@Tag("FR-016-19")
@Tag("FR-027-16")
class ConsulterRevueCasUsageTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private final ConsulterRevueCasUsage revue = new ConsulterRevueCasUsage(memoire);
    private final ConsulterSuiviVirementsCasUsage suivi = new ConsulterSuiviVirementsCasUsage(memoire);

    @BeforeEach
    void preparer() {
        RevueTestes.suspendu(memoire);
    }

    private void virement(long id, EtatVirement etat, long minutesApresSoumission, boolean enRevue) {
        memoire.virements.put(id, new Virement(id, 7, new CompteId(1), new CompteId(2), Montant.lire("100.00"), Motif.AUCUN, etat,
                StatutAffichage.SUCCESS, RevueTestes.SOUMIS.plusSeconds(60 * minutesApresSoumission),
                enRevue ? RevueTestes.BUTOIR.plusSeconds(60 * minutesApresSoumission) : null,
                enRevue ? "motif" : null));
    }

    @Test
    void la_file_liste_les_suspendus_par_heure_butoir_croissante_avec_le_nom_du_client() {
        virement(13, EtatVirement.SUSPENDU, -10, true);
        virement(14, EtatVirement.EXECUTE, 0, false);
        virement(15, EtatVirement.LIBERE, 0, true);

        assertThat(revue.fileDeRevue(RevueTestes.ADMIN)).extracting(l -> l.virementId(), l -> l.nomClient(),
                l -> l.numeroDestinataire()).containsExactly(
                        org.assertj.core.groups.Tuple.tuple(13L, "Claire Martin", "FR76-0001-0002"),
                        org.assertj.core.groups.Tuple.tuple(12L, "Claire Martin", "FR76-0001-0002"));
    }

    @Test
    void l_historique_liste_les_virements_traites_du_plus_recent_au_plus_ancien() {
        virement(13, EtatVirement.REJETE, 0, true);
        memoire.decisionsRevue.put(12L, new fr.nextbanking.coeurbancaire.domain.DecisionRevue(IssueRevue.LIBERE, "99",
                "ok", RevueTestes.SOUMIS.plusSeconds(10)));
        memoire.decisionsRevue.put(13L, new fr.nextbanking.coeurbancaire.domain.DecisionRevue(IssueRevue.REJETE, "99",
                "non", RevueTestes.SOUMIS.plusSeconds(20)));

        assertThat(revue.historique(RevueTestes.ADMIN)).extracting(l -> l.ligne().virementId(), l -> l.issue(),
                l -> l.dateIssue()).containsExactly(
                        org.assertj.core.groups.Tuple.tuple(13L, IssueRevue.REJETE, RevueTestes.SOUMIS.plusSeconds(20)),
                        org.assertj.core.groups.Tuple.tuple(12L, IssueRevue.LIBERE, RevueTestes.SOUMIS.plusSeconds(10)));
    }

    @Test
    void un_client_ou_un_conseiller_n_a_acces_ni_a_la_file_ni_a_l_historique() {
        for (Set<Role> roles : new Set[] {Set.of(Role.CLIENT), Set.of(Role.CONSEILLER), Set.<Role>of()}) {
            assertThatThrownBy(() -> revue.fileDeRevue(roles)).isInstanceOf(ErreurVirement.class)
                    .hasMessage("Accès refusé");
            assertThatThrownBy(() -> revue.historique(roles)).isInstanceOf(ErreurVirement.class);
        }
        assertThat(new CommandeDecision(1, Set.of(Role.ADMIN), 1, null, null).roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void le_suivi_ne_garde_que_les_quatre_virements_passes_en_revue_les_plus_recents() {
        virement(13, EtatVirement.LIBERE, 1, true);
        virement(14, EtatVirement.REJETE, 2, true);
        virement(15, EtatVirement.EXECUTE, 3, true);
        virement(16, EtatVirement.SUSPENDU, 4, true);
        virement(17, EtatVirement.EXECUTE, 5, false);
        virement(18, EtatVirement.REFUSE_ERREUR_CONTROLE, 6, false);

        assertThat(suivi.suivi(7)).extracting(LigneSuivi::virementId).containsExactly(16L, 15L, 14L, 13L);
        assertThat(suivi.suivi(7).getFirst().etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(suivi.suivi(8)).isEmpty();
    }
}
