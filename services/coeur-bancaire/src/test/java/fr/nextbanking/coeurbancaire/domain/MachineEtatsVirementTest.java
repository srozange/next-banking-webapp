package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Machine à cinq états du virement (AD-12) : EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE. Toute
 * transition hors de la table est refusée.
 */
@Tag("FR-027-16")
class MachineEtatsVirementTest {

    private static final Map<EtatVirement, Set<EtatVirement>> AUTORISEES = Map.of(
            EtatVirement.SUSPENDU, EnumSet.of(EtatVirement.LIBERE, EtatVirement.REJETE),
            EtatVirement.LIBERE, EnumSet.of(EtatVirement.EXECUTE, EtatVirement.REJETE),
            EtatVirement.EXECUTE, EnumSet.noneOf(EtatVirement.class),
            EtatVirement.REJETE, EnumSet.noneOf(EtatVirement.class),
            EtatVirement.REFUSE_ERREUR_CONTROLE, EnumSet.noneOf(EtatVirement.class));

    @Test
    void les_cinq_etats_existent() {
        assertThat(EtatVirement.values()).containsExactly(EtatVirement.EXECUTE, EtatVirement.SUSPENDU,
                EtatVirement.LIBERE, EtatVirement.REJETE, EtatVirement.REFUSE_ERREUR_CONTROLE);
    }

    @Test
    void la_soumission_mene_a_execute_suspendu_ou_refuse_pour_erreur_de_controle() {
        assertThat(EnumSet.allOf(EtatVirement.class).stream().filter(EtatVirement::estIssueDeSoumission))
                .containsExactlyInAnyOrder(EtatVirement.EXECUTE, EtatVirement.SUSPENDU,
                        EtatVirement.REFUSE_ERREUR_CONTROLE);
    }

    @Test
    void seules_les_transitions_de_la_table_sont_autorisees_les_autres_sont_refusees() {
        for (EtatVirement de : EtatVirement.values()) {
            for (EtatVirement vers : EtatVirement.values()) {
                assertThat(de.peutTransiterVers(vers)).as(de + " -> " + vers)
                        .isEqualTo(AUTORISEES.get(de).contains(vers));
            }
        }
        assertThat(EtatVirement.REJETE.peutTransiterVers(EtatVirement.LIBERE)).isFalse();
        assertThat(EtatVirement.EXECUTE.peutTransiterVers(EtatVirement.SUSPENDU)).isFalse();
    }
}
