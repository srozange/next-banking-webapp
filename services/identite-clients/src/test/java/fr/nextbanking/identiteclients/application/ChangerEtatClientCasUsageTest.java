package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.ActionAudit;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.ClientIntrouvable;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-030-07")
@Tag("FR-016-01")
@Tag("FR-016-11")
@Tag("FR-026-02")
@Tag("FR-026-05")
@Tag("BUG-093")
class ChangerEtatClientCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T09:00:00Z");

    private final List<String> deroule = new ArrayList<>();
    private final List<EntreeAudit> consignees = new ArrayList<>();
    private boolean echecEcriture;

    private final DepotClients depot = new DepotClients() {
        @Override
        public Optional<Client> parIdentifiant(String identifiant) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Client> parId(long clientId) {
            deroule.add("lire " + clientId);
            return clientId == 1L
                    ? Optional.of(new Client(1L, "user1", "Jean Dupont", Role.CLIENT, true, "{bcrypt}x"))
                    : Optional.empty();
        }

        @Override
        public void changerActif(long clientId, boolean actif) {
            deroule.add("actif " + clientId + "=" + actif);
            if (echecEcriture) {
                throw new IllegalStateException("écriture impossible");
            }
        }
    };

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
            deroule.add("independante");
            return travail.get();
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

    private final ChangerEtatClientCasUsage casUsage =
            new ChangerEtatClientCasUsage(depot, journal, transaction, horloge);

    @Test
    void l_administrateur_desactive_apres_avoir_consigne_l_audit_dans_une_transaction_independante() {
        casUsage.desactiver(10L, Set.of(Role.ADMIN), 1L, "203.0.113.7");

        assertThat(consignees).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_DEACTIVATE);
            assertThat(entree.utilisateur()).isEqualTo("10");
            assertThat(entree.detail()).isEqualTo("Désactivation client id=1");
            assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
            assertThat(entree.horodatage()).isEqualTo(MAINTENANT);
        });
        assertThat(deroule).containsExactly("debut", "lire 1", "independante", "consigner", "actif 1=false",
                "validation", "publier 7");
    }

    @Test
    void l_administrateur_active_et_consigne_admin_activate() {
        casUsage.activer(10L, Set.of(Role.ADMIN), 1L, null);

        assertThat(consignees).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.ADMIN_ACTIVATE);
            assertThat(entree.detail()).isEqualTo("Activation client id=1");
            assertThat(entree.adresseIp()).isNull();
        });
        assertThat(deroule).contains("actif 1=true").endsWith("publier 7");
    }

    @Test
    void un_role_insuffisant_est_refuse_sans_rien_lire_ecrire_ni_consigner() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER), Set.<Role>of())) {
            assertThatThrownBy(() -> casUsage.desactiver(1L, roles, 1L, "203.0.113.7"))
                    .isInstanceOf(AccesRefuse.class);
            assertThatThrownBy(() -> casUsage.activer(1L, roles, 1L, "203.0.113.7"))
                    .isInstanceOf(AccesRefuse.class);
        }
        assertThatThrownBy(() -> casUsage.desactiver(1L, null, 1L, null)).isInstanceOf(AccesRefuse.class);
        assertThat(deroule).isEmpty();
        assertThat(consignees).isEmpty();
    }

    @Test
    void un_client_inexistant_est_refuse_sans_audit_ni_ecriture() {
        assertThatThrownBy(() -> casUsage.desactiver(10L, Set.of(Role.ADMIN), 999L, "203.0.113.7"))
                .isInstanceOf(ClientIntrouvable.class);

        assertThat(consignees).isEmpty();
        assertThat(deroule).containsExactly("debut", "lire 999");
    }

    @Test
    void un_echec_d_ecriture_remonte_apres_la_consignation_de_l_audit_et_sans_publication() {
        echecEcriture = true;

        assertThatThrownBy(() -> casUsage.desactiver(10L, Set.of(Role.ADMIN), 1L, "203.0.113.7"))
                .hasMessage("écriture impossible");

        assertThat(consignees).hasSize(1);
        assertThat(deroule).doesNotContain("validation").noneMatch(e -> e.startsWith("publier"));
    }
}
