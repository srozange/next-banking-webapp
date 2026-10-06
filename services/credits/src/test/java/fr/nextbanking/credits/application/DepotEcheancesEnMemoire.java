package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Double de test : prêts et échéances en mémoire, avec journal des appels et pannes provoquables.
 */
class DepotEcheancesEnMemoire implements PretRepository {

    static final Instant MAINTENANT = Instant.parse("2026-10-06T08:30:01Z");

    final List<String> journal = new ArrayList<>();
    final List<Pret> prets = new ArrayList<>();
    final List<Echeance> echeances = new ArrayList<>();
    RuntimeException panne;

    static Pret pret(long id, long clientId, StatutPret statut) {
        return new Pret(id, clientId, TypePret.TRAVAUX, new BigDecimal("5000.00"), new BigDecimal("4.9000"), 24,
                new BigDecimal("218.63"), new BigDecimal("5247.12"), new BigDecimal("247.12"), "Travaux", statut,
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 8, 1));
    }

    static Echeance echeance(long id, long pretId, int numero, boolean paye) {
        return new Echeance(id, pretId, numero, LocalDate.of(2026, 9, 1).plusMonths(numero), new BigDecimal("200.00"),
                new BigDecimal("18.63"), new BigDecimal("218.63"), new BigDecimal("1000.00"), paye,
                paye ? Instant.parse("2026-09-01T08:00:00Z") : null);
    }

    final Transaction transaction = new Transaction() {
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

    final Horloge horloge = new Horloge() {
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

    @Override
    public List<Pret> parClient(long clientId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Pret> parId(long pretId) {
        journal.add("parId " + pretId);
        return prets.stream().filter(pret -> pret.id() == pretId).findFirst();
    }

    @Override
    public long creer(NouveauPret pret) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean transitionner(long pretId, StatutPret de, StatutPret vers) {
        journal.add("transitionner " + de + "->" + vers);
        return true;
    }

    @Override
    public void creerEcheancier(long pretId, List<EcheancePret> lignes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Echeance> echeances(long pretId) {
        journal.add("echeances " + pretId);
        return echeances.stream().filter(echeance -> echeance.pretId() == pretId).toList();
    }

    @Override
    public Optional<Echeance> echeanceParId(long echeanceId) {
        journal.add("echeanceParId " + echeanceId);
        return echeances.stream().filter(echeance -> echeance.id() == echeanceId).findFirst();
    }

    @Override
    public boolean payerEcheance(long echeanceId, Instant datePaiement) {
        journal.add("payer " + echeanceId + " " + datePaiement);
        if (panne != null) {
            throw panne;
        }
        for (int i = 0; i < echeances.size(); i++) {
            Echeance e = echeances.get(i);
            if (e.id() == echeanceId && !e.paye()) {
                echeances.set(i, new Echeance(e.id(), e.pretId(), e.numero(), e.dateEcheance(), e.capital(),
                        e.interets(), e.total(), e.capitalRestant(), true, datePaiement));
                return true;
            }
        }
        return false;
    }

    @Override
    public long nombreEcheancesNonPayees(long pretId) {
        journal.add("nonPayees " + pretId);
        return echeances.stream().filter(e -> e.pretId() == pretId && !e.paye()).count();
    }

    @Override
    public void verrouiller(long pretId) {
        journal.add("verrouiller " + pretId);
    }

    @Override
    public boolean supprimer(long pretId) {
        journal.add("supprimer " + pretId);
        if (panne != null) {
            throw panne;
        }
        return prets.removeIf(pret -> pret.id() == pretId);
    }
}
