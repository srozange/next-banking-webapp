package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.DemandePretInvalide;
import fr.nextbanking.credits.domain.DemandePretInvalide.Motif;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretDemande;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DeposerDemandePretCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-06T08:30:00Z");

    private final List<String> journal = new ArrayList<>();
    private final List<NouveauPret> prets = new ArrayList<>();
    private final List<PretDemande> evenements = new ArrayList<>();
    private RuntimeException panneDepot;
    private RuntimeException panneConsigner;
    private RuntimeException pannePublier;
    private RuntimeException panneValidation;

    private final PretRepository depot = new PretRepository() {
        @Override
        public List<Pret> parClient(long clientId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Pret> parId(long pretId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long creer(NouveauPret pret) {
            journal.add("creer");
            if (panneDepot != null) {
                throw panneDepot;
            }
            prets.add(pret);
            return 70L + prets.size();
        }
    };

    private final EvenementsSortants sortants = new EvenementsSortants() {
        @Override
        public long consigner(PretDemande evenement) {
            journal.add("consigner");
            if (panneConsigner != null) {
                throw panneConsigner;
            }
            evenements.add(evenement);
            return 100L + evenements.size();
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
            if (panneValidation != null) {
                throw panneValidation;
            }
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

    private final DeposerDemandePretCasUsage casUsage = new DeposerDemandePretCasUsage(depot, sortants, transaction,
            horloge);

    private Pret deposer() {
        return casUsage.deposer(5L, "CONSOMMATION", "10000", "3.5", "300", "Voiture");
    }

    @Test
    @Tag("FR-007-07")
    @Tag("FR-024-01")
    void enregistre_le_pret_et_l_outbox_dans_la_transaction_puis_publie_apres_validation() {
        Pret pret = deposer();

        assertThat(journal).containsExactly("debut", "creer", "consigner", "validation", "publier 101");
        assertThat(pret.id()).isEqualTo(71L);
        assertThat(pret.clientId()).isEqualTo(5L);
        assertThat(pret.statut()).isEqualTo(StatutPret.EN_ATTENTE);
        assertThat(pret.type()).isEqualTo(TypePret.CONSOMMATION);
        assertThat(pret.dateDebut()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(pret.dateFin()).isEqualTo(LocalDate.of(2051, 10, 1));
        assertThat(pret.objet()).isEqualTo("Voiture");
        assertThat(evenements).hasSize(1);
        assertThat(evenements.get(0).pretId()).isEqualTo(71L);
        assertThat(evenements.get(0).horodatage()).isEqualTo(MAINTENANT);
    }

    @Test
    @Tag("FR-007-06")
    @Tag("BUG-025")
    void une_demande_invalide_n_ecrit_rien() {
        assertThatThrownBy(() -> casUsage.deposer(5L, "CONSOMMATION", "10000", "3.5", "301", "x"))
                .isInstanceOf(DemandePretInvalide.class)
                .extracting(e -> ((DemandePretInvalide) e).motif()).isEqualTo(Motif.DUREE);

        assertThat(journal).isEmpty();
    }

    @Test
    @Tag("FR-024-02")
    @Tag("BUG-077")
    void une_panne_de_l_outbox_est_une_transmission_en_echec_sans_publication() {
        IllegalStateException panne = new IllegalStateException("outbox");
        panneConsigner = panne;

        assertThatThrownBy(this::deposer).isInstanceOf(TransmissionEnEchec.class).hasCause(panne);
        assertThat(journal).containsExactly("debut", "creer", "consigner");
    }

    @Test
    @Tag("FR-024-02")
    void une_autre_panne_d_enregistrement_est_une_demande_en_echec() {
        IllegalStateException panne = new IllegalStateException("insert");
        panneDepot = panne;

        assertThatThrownBy(this::deposer).isInstanceOf(DemandePretEnEchec.class).hasCause(panne);
        assertThat(journal).containsExactly("debut", "creer");
    }

    @Test
    @Tag("FR-024-02")
    void un_echec_de_validation_est_une_demande_en_echec_sans_publication() {
        IllegalStateException panne = new IllegalStateException("commit");
        panneValidation = panne;

        assertThatThrownBy(this::deposer).isInstanceOf(DemandePretEnEchec.class).hasCause(panne);
        assertThat(journal).doesNotContain("validation", "publier 101");
    }

    @Test
    @Tag("FR-024-01")
    void une_panne_de_publication_apres_validation_ne_fait_pas_echouer_la_demande() {
        pannePublier = new IllegalStateException("kafka");

        assertThat(deposer().id()).isEqualTo(71L);
        assertThat(journal).containsExactly("debut", "creer", "consigner", "validation", "publier 101");
    }
}
