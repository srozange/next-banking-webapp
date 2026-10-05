package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.out.DepotTemoins;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.Temoin;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class EcrireTemoinCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-05T08:30:00Z");

    private final List<String> journal = new ArrayList<>();
    private final List<Temoin> temoins = new ArrayList<>();
    private final List<TemoinEcrit> evenements = new ArrayList<>();

    private final DepotTemoins depot = temoin -> {
        journal.add("enregistrer");
        temoins.add(temoin);
        return 41L + temoins.size();
    };

    private final EvenementsSortants sortants = new EvenementsSortants() {
        @Override
        public long consigner(TemoinEcrit evenement) {
            journal.add("consigner");
            evenements.add(evenement);
            return 100L + evenements.size();
        }

        @Override
        public void publier(long idLigne) {
            journal.add("publier " + idLigne);
        }
    };

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

    private final Horloge horloge = new Horloge() {
        @Override
        public Instant instant() {
            return MAINTENANT;
        }

        @Override
        public LocalDate aujourdhui() {
            return LocalDate.of(2026, 10, 5);
        }

        @Override
        public ZoneId fuseau() {
            return ZoneId.of("Europe/Paris");
        }
    };

    private final EcrireTemoinCasUsage casUsage = new EcrireTemoinCasUsage(depot, sortants, transaction, horloge);

    @Test
    void enregistre_le_temoin_et_l_outbox_dans_la_transaction_puis_publie_apres_validation() {
        long temoinId = casUsage.ecrire("premier témoin");

        assertThat(temoinId).isEqualTo(42L);
        assertThat(temoins).containsExactly(new Temoin("premier témoin"));
        assertThat(journal).containsExactly("debut", "enregistrer", "consigner", "validation", "publier 101");
    }

    @Test
    void l_evenement_porte_le_temoin_et_l_horodatage_de_l_horloge() {
        casUsage.ecrire("premier témoin");
        casUsage.ecrire("second témoin");

        assertThat(evenements).hasSize(2);
        assertThat(evenements.get(0).temoinId()).isEqualTo(42L);
        assertThat(evenements.get(0).horodatage()).isEqualTo(MAINTENANT);
        assertThat(evenements.get(1).temoinId()).isEqualTo(43L);
        assertThat(evenements.get(0).idEvenement()).isNotEqualTo(evenements.get(1).idEvenement());
    }

    @Test
    void n_ecrit_rien_si_le_libelle_est_refuse() {
        assertThatThrownBy(() -> casUsage.ecrire(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(journal).isEmpty();
    }
}
