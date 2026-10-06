package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.application.port.out.EvenementsTraites;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.EvenementSortant;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretDecide;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypeNotification;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DecisionPretCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T08:30:01Z");

    private final List<String> journal = new ArrayList<>();
    private final Set<UUID> traites = new HashSet<>();
    private final List<EvenementSortant> evenements = new ArrayList<>();
    private final List<EcheancePret> echeances = new ArrayList<>();
    private Pret pret;
    private boolean transitionPerdue;
    private RuntimeException pannePublier;

    private final PretRepository depot = new PretRepository() {
        @Override
        public List<Pret> parClient(long clientId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Pret> parId(long pretId) {
            journal.add("parId " + pretId);
            return Optional.ofNullable(pret).filter(p -> p.id() == pretId);
        }

        @Override
        public long creer(NouveauPret nouveau) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean transitionner(long pretId, StatutPret de, StatutPret vers) {
            journal.add("transitionner " + de + "->" + vers);
            return !transitionPerdue;
        }

        @Override
        public void creerEcheancier(long pretId, List<EcheancePret> lignes) {
            journal.add("echeancier");
            echeances.addAll(lignes);
        }
    };

    private final EvenementsTraites registre = id -> {
        journal.add("marquer");
        return traites.add(id);
    };

    private final EvenementsSortants sortants = new EvenementsSortants() {
        @Override
        public long consigner(EvenementSortant evenement) {
            journal.add("consigner");
            evenements.add(evenement);
            return 200L;
        }

        @Override
        public void publier(long idLigne) {
            journal.add("publier " + idLigne);
            if (pannePublier != null) {
                throw pannePublier;
            }
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
            return LocalDate.of(2026, 10, 6);
        }

        @Override
        public ZoneId fuseau() {
            return ZoneId.of("Europe/Paris");
        }
    };

    private final DecisionPretCasUsage casUsage = new DecisionPretCasUsage(depot, registre, sortants, transaction,
            horloge);

    private static Pret pret(TypePret type, String montant, int duree, StatutPret statut) {
        return new Pret(42L, 5L, type, new BigDecimal(montant), new BigDecimal("3.5000"), duree,
                new BigDecimal("293.02"), new BigDecimal("10548.72"), new BigDecimal("548.72"), "Voiture", statut,
                LocalDate.of(2026, 10, 1), LocalDate.of(2029, 10, 1));
    }

    @Test
    @Tag("FR-007-10")
    @Tag("FR-024-06")
    void l_approbation_passe_en_cours_cree_l_echeancier_et_consigne_dans_la_transaction_puis_publie() {
        pret = pret(TypePret.CONSOMMATION, "10000.00", 36, StatutPret.EN_ATTENTE);

        assertThat(casUsage.decider(UUID.randomUUID(), 42L)).isTrue();

        assertThat(journal).containsExactly("debut", "marquer", "parId 42", "transitionner EN_ATTENTE->EN_COURS",
                "echeancier", "consigner", "validation", "publier 200");
        assertThat(echeances).hasSize(36);
        PretDecide decide = (PretDecide) evenements.get(0);
        assertThat(decide.statut()).isEqualTo(StatutPret.EN_COURS);
        assertThat(decide.notification().type()).isEqualTo(TypeNotification.CONFIRMATION);
        assertThat(decide.horodatage()).isEqualTo(MAINTENANT);
    }

    @Test
    @Tag("FR-007-09")
    @Tag("FR-024-07")
    void le_refus_passe_refuse_sans_echeancier_avec_une_alerte() {
        pret = pret(TypePret.CONSOMMATION, "100.00", 301, StatutPret.EN_ATTENTE);

        assertThat(casUsage.decider(UUID.randomUUID(), 42L)).isTrue();

        assertThat(journal).containsExactly("debut", "marquer", "parId 42", "transitionner EN_ATTENTE->REFUSE",
                "consigner", "validation", "publier 200");
        assertThat(echeances).isEmpty();
        assertThat(((PretDecide) evenements.get(0)).notification().type()).isEqualTo(TypeNotification.ALERTE);
    }

    @Test
    @Tag("FR-024-08")
    void un_pret_qui_n_est_plus_en_attente_est_ignore_sans_effet() {
        for (StatutPret statut : new StatutPret[] {StatutPret.EN_COURS, StatutPret.REFUSE, StatutPret.REMBOURSE}) {
            journal.clear();
            pret = pret(TypePret.CONSOMMATION, "10000.00", 36, statut);

            assertThat(casUsage.decider(UUID.randomUUID(), 42L)).isTrue();

            assertThat(journal).containsExactly("debut", "marquer", "parId 42", "validation");
        }
        assertThat(evenements).isEmpty();
        assertThat(echeances).isEmpty();
    }

    @Test
    @Tag("FR-024-08")
    void un_pret_inconnu_est_ignore() {
        pret = null;

        assertThat(casUsage.decider(UUID.randomUUID(), 99L)).isTrue();

        assertThat(journal).containsExactly("debut", "marquer", "parId 99", "validation");
        assertThat(evenements).isEmpty();
    }

    @Test
    @Tag("FR-024-08")
    void un_evenement_deja_traite_ne_fait_rien_d_autre() {
        pret = pret(TypePret.CONSOMMATION, "10000.00", 36, StatutPret.EN_ATTENTE);
        UUID id = UUID.randomUUID();
        casUsage.decider(id, 42L);
        journal.clear();

        assertThat(casUsage.decider(id, 42L)).isFalse();

        assertThat(journal).containsExactly("debut", "marquer", "validation");
        assertThat(evenements).hasSize(1);
    }

    @Test
    @Tag("FR-024-08")
    void une_transition_perdue_est_ignoree_sans_echeancier_ni_evenement() {
        pret = pret(TypePret.CONSOMMATION, "10000.00", 36, StatutPret.EN_ATTENTE);
        transitionPerdue = true;

        assertThat(casUsage.decider(UUID.randomUUID(), 42L)).isTrue();

        assertThat(journal).containsExactly("debut", "marquer", "parId 42", "transitionner EN_ATTENTE->EN_COURS",
                "validation");
        assertThat(echeances).isEmpty();
        assertThat(evenements).isEmpty();
    }

    @Test
    @Tag("FR-024-06")
    void une_panne_de_publication_apres_validation_est_laissee_au_relais() {
        pret = pret(TypePret.CONSOMMATION, "10000.00", 36, StatutPret.EN_ATTENTE);
        pannePublier = new IllegalStateException("kafka");

        assertThat(casUsage.decider(UUID.randomUUID(), 42L)).isTrue();

        assertThat(journal).endsWith("validation", "publier 200");
    }
}
