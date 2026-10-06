package fr.nextbanking.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.TemoinRecu;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-002-09")
@Tag("BUG-007")
class RecevoirTemoinCasUsageTest {

    private static final UUID ID = UUID.fromString("3f2c1a9e-8b7d-4c6e-9f10-2a3b4c5d6e7f");

    private final List<String> journal = new ArrayList<>();
    private final List<TemoinRecu> enregistres = new ArrayList<>();
    private final Set<UUID> traites = new HashSet<>();

    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            journal.add("debut");
            T resultat = travail.get();
            journal.add("validation");
            return resultat;
        }

        @Override
        public <T> T executerIndependamment(Supplier<T> travail) {
            throw new UnsupportedOperationException();
        }
    };

    private final RecevoirTemoinCasUsage casUsage = new RecevoirTemoinCasUsage(
            temoin -> {
                journal.add("enregistrer");
                enregistres.add(temoin);
            },
            idEvenement -> {
                journal.add("marquer");
                return traites.add(idEvenement);
            },
            transaction);

    @Test
    void enregistre_le_temoin_et_l_evenement_dans_une_meme_transaction() {
        assertThat(casUsage.recevoir(ID, 42L)).isTrue();

        assertThat(enregistres).containsExactly(new TemoinRecu(42L));
        assertThat(traites).containsExactly(ID);
        assertThat(journal).containsExactly("debut", "marquer", "enregistrer", "validation");
    }

    @Test
    void ignore_un_evenement_deja_traite() {
        casUsage.recevoir(ID, 42L);

        assertThat(casUsage.recevoir(ID, 42L)).isFalse();
        assertThat(enregistres).containsExactly(new TemoinRecu(42L));
    }

    @Test
    void n_enregistre_rien_si_l_identifiant_du_temoin_est_refuse() {
        assertThatThrownBy(() -> casUsage.recevoir(ID, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(journal).isEmpty();
    }

    @Test
    void n_enregistre_rien_sans_identifiant_d_evenement() {
        assertThatThrownBy(() -> casUsage.recevoir(null, 42L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(journal).isEmpty();
    }
}
