package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.MemoireCoeurBancaire.RevueAlerte;
import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Liste et revue des alertes de fraude par l'administrateur : rôle vérifié avant tout, toutes les alertes rendues
 * telles que le port les trie, auteur par identifiant et heure de l'horloge, identifiant nul ignoré, seules les issues
 * CONFIRMED et FALSE_POSITIVE acceptées, aucune écriture hors de l'alerte.
 */
@Tag("FR-016-14")
@Tag("FR-016-15")
@Tag("FR-016-16")
@Tag("FR-027-12")
class RevoirAlertesFraudeCasUsageTest {

    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);
    private static final Instant T = Instant.parse("2026-11-17T09:30:00Z");

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private final RevoirAlertesFraudeCasUsage revue = new RevoirAlertesFraudeCasUsage(memoire, memoire);

    @BeforeEach
    void preparer() {
        RevueTestes.suspendu(memoire);
        memoire.noms.put(10L, "Alice Martin");
        memoire.maintenant = T;
        memoire.alertesAdministrees.add(alerte(3, StatutAlerte.OPEN, null, null));
        memoire.alertesAdministrees.add(alerte(2, StatutAlerte.CONFIRMED, "10", "Alice Martin"));
        memoire.alertesAdministrees.add(alerte(1, StatutAlerte.FALSE_POSITIVE, "10", "Alice Martin"));
        memoire.journal.clear();
    }

    private static AlerteAdministree alerte(long id, StatutAlerte statut, String revuPar, String nomRelecteur) {
        return new AlerteAdministree(id, 7, "Claire Martin", "LARGE_AMOUNT", 45, statut, "description",
                RevueTestes.SOUMIS.plusSeconds(id), revuPar, nomRelecteur);
    }

    @Test
    @Tag("BUG-056")
    void l_administrateur_recoit_toutes_les_alertes_tous_statuts_dans_l_ordre_du_port() {
        assertThat(revue.toutes(ADMIN)).extracting(AlerteAdministree::alerteId, AlerteAdministree::statut)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, StatutAlerte.OPEN),
                        org.assertj.core.groups.Tuple.tuple(2L, StatutAlerte.CONFIRMED),
                        org.assertj.core.groups.Tuple.tuple(1L, StatutAlerte.FALSE_POSITIVE));
    }

    @Test
    void un_role_autre_qu_admin_est_refuse_sans_aucun_appel_au_port() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER), Set.<Role>of())) {
            assertThatThrownBy(() -> revue.toutes(roles)).isInstanceOf(ErreurVirement.class)
                    .hasMessage("Accès refusé");
            assertThatThrownBy(() -> revue.revoir(3L, StatutAlerte.CONFIRMED, 10, roles))
                    .isInstanceOf(ErreurVirement.class).hasMessage("Accès refusé");
            assertThatThrownBy(() -> revue.revoir(null, StatutAlerte.REVIEWED, 10, roles))
                    .isInstanceOf(ErreurVirement.class);
        }
        assertThat(memoire.journal).isEmpty();
        assertThat(memoire.revuesAlertes).isEmpty();
    }

    @Test
    void la_confirmation_enregistre_l_auteur_par_identifiant_et_l_heure_de_l_horloge() {
        assertThat(revue.revoir(3L, StatutAlerte.CONFIRMED, 10, ADMIN)).isTrue();

        assertThat(memoire.revuesAlertes).containsExactly(new RevueAlerte(3, StatutAlerte.CONFIRMED, "10", T));
        assertThat(memoire.alertesAdministrees.getFirst().statut()).isEqualTo(StatutAlerte.CONFIRMED);
        assertThat(memoire.alertesAdministrees.getFirst().nomRelecteur()).isEqualTo("Alice Martin");
    }

    @Test
    @Tag("FR-027-13")
    void le_faux_positif_n_ecrit_que_l_alerte_sans_toucher_au_virement_suspendu() {
        assertThat(revue.revoir(3L, StatutAlerte.FALSE_POSITIVE, 10, ADMIN)).isTrue();

        assertThat(memoire.revuesAlertes).containsExactly(new RevueAlerte(3, StatutAlerte.FALSE_POSITIVE, "10", T));
        assertThat(memoire.virements.get(12L).etat()).isEqualTo(fr.nextbanking.coeurbancaire.domain.EtatVirement.SUSPENDU);
        assertThat(memoire.alertes).extracting(a -> a.statut()).containsExactly(StatutAlerte.OPEN, StatutAlerte.OPEN,
                StatutAlerte.CONFIRMED);
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.outboxAudit).isEmpty();
        assertThat(memoire.journal).isEmpty();
    }

    @Test
    void une_alerte_deja_traitee_ne_change_pas() {
        assertThat(revue.revoir(2L, StatutAlerte.FALSE_POSITIVE, 11, ADMIN)).isFalse();

        assertThat(memoire.alertesAdministrees.get(1)).isEqualTo(alerte(2, StatutAlerte.CONFIRMED, "10",
                "Alice Martin"));
    }

    @Test
    void un_identifiant_nul_est_ignore_sans_erreur_ni_appel_au_port() {
        assertThat(revue.revoir(null, StatutAlerte.CONFIRMED, 10, ADMIN)).isFalse();

        assertThat(memoire.revuesAlertes).isEmpty();
    }

    @Test
    void seules_les_issues_confirmee_et_faux_positif_sont_acceptees() {
        for (StatutAlerte issue : new StatutAlerte[] {StatutAlerte.OPEN, StatutAlerte.REVIEWED, null}) {
            assertThatThrownBy(() -> revue.revoir(3L, issue, 10, ADMIN)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(memoire.revuesAlertes).isEmpty();
    }
}
