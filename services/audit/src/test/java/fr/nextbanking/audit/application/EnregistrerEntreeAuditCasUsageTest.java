package fr.nextbanking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit.Issue;
import fr.nextbanking.audit.application.port.out.Transaction;
import fr.nextbanking.audit.domain.ActionAudit;
import fr.nextbanking.audit.domain.EntreeAudit;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-01")
class EnregistrerEntreeAuditCasUsageTest {

    private static final UUID ID = UUID.fromString("7b1e4c2a-5d3f-4e6a-8b9c-0d1e2f3a4b5c");
    private static final Instant HORODATAGE = Instant.parse("2026-10-05T08:30:00Z");

    private final List<String> deroule = new ArrayList<>();
    private final List<EntreeAudit> journal = new ArrayList<>();
    private final Set<UUID> traites = new HashSet<>();

    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            deroule.add("debut");
            T resultat = travail.get();
            deroule.add("validation");
            return resultat;
        }
    };

    private final EnregistrerEntreeAuditCasUsage casUsage = new EnregistrerEntreeAuditCasUsage(
            entree -> {
                deroule.add("enregistrer");
                journal.add(entree);
            },
            idEvenement -> {
                deroule.add("marquer");
                return traites.add(idEvenement);
            },
            transaction);

    @Test
    void enregistre_l_entree_et_l_evenement_dans_une_meme_transaction_avec_l_horodatage_de_l_enveloppe() {
        assertThat(casUsage.enregistrer(ID, "1", "LOGIN", "Connexion réussie", "203.0.113.7", HORODATAGE))
                .isEqualTo(Issue.ENREGISTREE);

        assertThat(journal).containsExactly(
                new EntreeAudit(ActionAudit.LOGIN, HORODATAGE, "Connexion réussie", "203.0.113.7", "1"));
        assertThat(traites).containsExactly(ID);
        assertThat(deroule).containsExactly("debut", "marquer", "enregistrer", "validation");
    }

    @Test
    void ignore_un_evenement_deja_traite() {
        casUsage.enregistrer(ID, "1", "LOGIN", "Connexion réussie", "203.0.113.7", HORODATAGE);

        assertThat(casUsage.enregistrer(ID, "1", "LOGIN", "Connexion réussie", "203.0.113.7", HORODATAGE))
                .isEqualTo(Issue.DEJA_TRAITEE);
        assertThat(journal).hasSize(1);
    }

    @Test
    void rejette_un_code_hors_catalogue_sans_rien_ecrire() {
        assertThat(casUsage.enregistrer(ID, "1", "CODE_INCONNU", "x", null, HORODATAGE)).isEqualTo(Issue.CODE_INCONNU);

        assertThat(deroule).isEmpty();
        assertThat(traites).isEmpty();
    }

    @Test
    void n_ecrit_rien_pour_une_entree_incomplete() {
        assertThatThrownBy(() -> casUsage.enregistrer(null, "1", "LOGIN", "d", null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casUsage.enregistrer(ID, " ", "LOGIN", "d", null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> casUsage.enregistrer(ID, "1", "LOGIN", "d", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(deroule).isEmpty();
    }
}
