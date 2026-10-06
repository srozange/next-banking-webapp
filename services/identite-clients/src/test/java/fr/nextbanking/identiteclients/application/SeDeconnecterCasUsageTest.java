package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.ActionAudit;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-001-10")
@Tag("FR-001-12")
@Tag("FR-026-01")
@Tag("FR-026-05")
class SeDeconnecterCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T09:00:00Z");

    private final List<String> deroule = new ArrayList<>();
    private final List<EntreeAudit> consignees = new ArrayList<>();

    private final JournalAudit journal = new JournalAudit() {
        @Override
        public long consigner(EntreeAudit entree) {
            deroule.add("consigner");
            consignees.add(entree);
            return 7L;
        }

        @Override
        public void publier(long idLigne) {
            deroule.add("publier " + idLigne);
        }
    };

    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            deroule.add("debut");
            T resultat = travail.get();
            deroule.add("validation");
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
            throw new UnsupportedOperationException();
        }

        @Override
        public ZoneId fuseau() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void consigne_logout_dans_une_transaction_puis_publie_apres_validation() {
        new SeDeconnecterCasUsage(journal, transaction, horloge).seDeconnecter(1L, "203.0.113.7");

        assertThat(consignees).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.LOGOUT);
            assertThat(entree.utilisateur()).isEqualTo("1");
            assertThat(entree.detail()).isEqualTo("Déconnexion");
            assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
            assertThat(entree.horodatage()).isEqualTo(MAINTENANT);
        });
        assertThat(deroule).containsExactly("debut", "consigner", "validation", "publier 7");
    }
}
