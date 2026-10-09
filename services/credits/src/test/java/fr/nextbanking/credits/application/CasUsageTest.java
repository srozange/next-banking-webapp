package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.Annuite;
import fr.nextbanking.credits.domain.DemandeSimulation;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.SimulationInvalide;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CasUsageTest {

    private static final Pret DE_USER1 = pret(7L, 1L);
    private static final Pret D_AUTRUI = pret(8L, 2L);

    private final PretRepository prets = new PretRepository() {
        @Override
        public List<Pret> desClients(java.util.Collection<Long> clientIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Pret> parClient(long clientId) {
            return List.of(DE_USER1, D_AUTRUI).stream().filter(pret -> pret.clientId() == clientId).toList();
        }

        @Override
        public Optional<Pret> parId(long pretId) {
            return List.of(DE_USER1, D_AUTRUI).stream().filter(pret -> pret.id() == pretId).findFirst();
        }

        @Override
        public List<Pret> enAttente() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Pret> tous() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long creer(fr.nextbanking.credits.domain.NouveauPret pret) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean transitionner(long pretId, StatutPret de, StatutPret vers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void creerEcheancier(long pretId, List<fr.nextbanking.credits.domain.EcheancePret> echeances) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<fr.nextbanking.credits.domain.Echeance> echeances(long pretId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<fr.nextbanking.credits.domain.Echeance> echeanceParId(long echeanceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean payerEcheance(long echeanceId, Instant datePaiement) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long nombreEcheancesNonPayees(long pretId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void verrouiller(long pretId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean supprimer(long pretId) {
            throw new UnsupportedOperationException();
        }
    };

    private static Pret pret(long id, long clientId) {
        BigDecimal montant = new BigDecimal("5000.00");
        return new Pret(id, clientId, TypePret.TRAVAUX, montant, new BigDecimal("4.9000"), 24,
                new BigDecimal("218.63"), new BigDecimal("5247.12"), new BigDecimal("247.12"), "Travaux",
                StatutPret.EN_COURS, LocalDate.of(2026, 8, 1), LocalDate.of(2028, 8, 1));
    }

    @Test
    @Tag("FR-007-03")
    void la_simulation_valide_passe_par_le_port_de_calcul() {
        SimulerEmpruntCasUsage casUsage = new SimulerEmpruntCasUsage(Annuite::simuler);

        assertThat(casUsage.simuler("10000", "3.5", "36").mensualite()).isEqualByComparingTo("293.02");
    }

    @Test
    @Tag("FR-007-02")
    @Tag("FR-004-05")
    void les_champs_invalides_sont_refuses_sans_appeler_le_calcul() {
        CalculSimulation interdit = demande -> {
            throw new AssertionError("calcul appelé");
        };
        SimulerEmpruntCasUsage casUsage = new SimulerEmpruntCasUsage(interdit);

        assertThatThrownBy(() -> casUsage.simuler("", "3.5", "36")).isInstanceOf(SimulationInvalide.class);
        assertThatThrownBy(() -> casUsage.simuler("100", "31", "36")).isInstanceOf(SimulationInvalide.class);
        assertThatThrownBy(() -> casUsage.simuler("100", "3", "361")).isInstanceOf(SimulationInvalide.class);
    }

    @Test
    @Tag("FR-007-03")
    void une_panne_du_calcul_devient_une_simulation_en_echec() {
        IllegalStateException panne = new IllegalStateException("détail interne");
        SimulerEmpruntCasUsage casUsage = new SimulerEmpruntCasUsage((DemandeSimulation demande) -> {
            throw panne;
        });

        assertThatThrownBy(() -> casUsage.simuler("10000", "3.5", "36"))
                .isInstanceOf(SimulationEnEchec.class).hasCause(panne);
    }

    @Test
    @Tag("FR-007-08")
    void la_liste_ne_contient_que_les_prets_du_client() {
        ListerPretsCasUsage casUsage = new ListerPretsCasUsage(prets);

        assertThat(casUsage.lister(1L)).containsExactly(DE_USER1);
        assertThat(casUsage.lister(3L)).isEmpty();
    }

    @Test
    @Tag("FR-007-01")
    void un_pret_n_est_consultable_que_par_son_proprietaire() {
        ConsulterPretCasUsage casUsage = new ConsulterPretCasUsage(prets);

        assertThat(casUsage.consulter(1L, 7L)).isEqualTo(DE_USER1);
        assertThatThrownBy(() -> casUsage.consulter(1L, 8L)).isInstanceOf(AccesPretRefuse.class);
        assertThatThrownBy(() -> casUsage.consulter(1L, 99L)).isInstanceOf(PretIntrouvable.class);
    }
}
