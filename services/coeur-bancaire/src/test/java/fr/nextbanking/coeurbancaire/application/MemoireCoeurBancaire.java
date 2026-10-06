package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import fr.nextbanking.coeurbancaire.domain.VirementDecide;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cœur bancaire en mémoire pour les tests des cas d'usage : tous les ports sortants, avec une transaction qui annule
 * les écritures (comptes, opérations, virements, outbox) quand le travail échoue, et des défaillances programmables.
 */
class MemoireCoeurBancaire implements Comptes, Operations, DepotVirements, EvenementsSortants, JournalAudit,
        EvenementsTraites, Transaction, Horloge, HistoriqueVirements, Beneficiaires, AlertesFraude {

    static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");

    final Map<Long, Compte> comptes = new HashMap<>();
    final List<Operation> operations = new ArrayList<>();
    final Map<Long, Virement> virements = new HashMap<>();
    final Map<String, Long> cles = new HashMap<>();
    final List<VirementATraiter> outbox = new ArrayList<>();
    final List<VirementDecide> decisions = new ArrayList<>();
    final List<Long> decisionsPubliees = new ArrayList<>();
    final List<Long> publies = new ArrayList<>();
    final List<EntreeAudit> outboxAudit = new ArrayList<>();
    final List<Long> auditPublie = new ArrayList<>();
    final Set<UUID> traites = new HashSet<>();
    final List<String> journal = new ArrayList<>();
    final List<AlerteFraude> alertes = new ArrayList<>();
    /** Numéros des bénéficiaires actifs, par client. */
    final Map<Long, Set<String>> beneficiairesActifs = new HashMap<>();
    /** Signaux d'historique forcés ; absents : calculés sur les opérations et virements écrits. */
    Long operationsDuJour;
    Optional<Instant> dernierVirement;
    Long virementsImportants;
    RuntimeException historiqueEnErreur;

    boolean outboxRefusee;
    boolean auditRefuse;
    boolean debitRefuse;
    long prochainId = 12;

    @Override
    public List<Compte> duClient(long clientId) {
        return comptes.values().stream().filter(c -> c.clientId() == clientId)
                .sorted(java.util.Comparator.comparingLong(Compte::id)).toList();
    }

    @Override
    public List<Compte> verrouiller(long premierId, long secondId) {
        journal.add("verrouiller");
        return java.util.stream.Stream.of(premierId, secondId).sorted().map(comptes::get)
                .filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public boolean debiter(long compteId, BigDecimal montant) {
        journal.add("debiter");
        Compte compte = comptes.get(compteId);
        if (debitRefuse || compte.solde().compareTo(montant) < 0) {
            return false;
        }
        comptes.put(compteId, new Compte(compteId, compte.clientId(), compte.numero(), compte.type(),
                compte.solde().subtract(montant)));
        return true;
    }

    @Override
    public void crediter(long compteId, BigDecimal montant) {
        journal.add("crediter");
        Compte compte = comptes.get(compteId);
        comptes.put(compteId, new Compte(compteId, compte.clientId(), compte.numero(), compte.type(),
                compte.solde().add(montant)));
    }

    @Override
    public void enregistrer(Operation operation) {
        operations.add(operation);
    }

    @Override
    public long attribuerIdentifiant() {
        return prochainId++;
    }

    @Override
    public void enregistrer(Virement virement, String cleIdempotence) {
        if (cleIdempotence != null) {
            String cle = virement.clientId() + "/" + cleIdempotence;
            if (cles.containsKey(cle)) {
                throw new ErreurVirement(Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE);
            }
            cles.put(cle, virement.id());
        }
        virements.put(virement.id(), virement);
    }

    @Override
    public Optional<Virement> parCle(long clientId, String cleIdempotence) {
        return Optional.ofNullable(cles.get(clientId + "/" + cleIdempotence)).map(virements::get);
    }

    @Override
    public Optional<Virement> parId(long virementId) {
        return Optional.ofNullable(virements.get(virementId));
    }

    @Override
    public void verrouillerClient(long clientId) {
        journal.add("verrou-client");
        if (historiqueEnErreur != null) {
            throw historiqueEnErreur;
        }
    }

    @Override
    public long operationsDepuis(long clientId, Instant debut) {
        journal.add("historique");
        if (operationsDuJour != null) {
            return operationsDuJour;
        }
        return operations.stream().filter(o -> comptes.get(o.compteId()).clientId() == clientId)
                .filter(o -> !o.date().isBefore(debut)).count();
    }

    @Override
    public Optional<Instant> dernierVirement(long clientId) {
        if (dernierVirement != null) {
            return dernierVirement;
        }
        return virements.values().stream().filter(v -> v.clientId() == clientId).map(Virement::soumisLe)
                .max(java.util.Comparator.naturalOrder());
    }

    @Override
    public long virementsExecutesAuDessus(long clientId, BigDecimal montantMin, Instant depuis) {
        if (virementsImportants != null) {
            return virementsImportants;
        }
        return virements.values().stream().filter(v -> v.clientId() == clientId)
                .filter(v -> v.etat() == EtatVirement.EXECUTE).filter(v -> v.montant().compareTo(montantMin) > 0)
                .filter(v -> !v.soumisLe().isBefore(depuis)).count();
    }

    @Override
    public boolean estActif(long clientId, String numero) {
        return beneficiairesActifs.getOrDefault(clientId, Set.of()).contains(numero);
    }

    @Override
    public void enregistrer(AlerteFraude alerte) {
        journal.add("alerte");
        alertes.add(alerte);
    }

    @Override
    public long consigner(TemoinEcrit evenement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long consigner(VirementATraiter evenement) {
        if (outboxRefusee) {
            throw new IllegalStateException("outbox refusée");
        }
        outbox.add(evenement);
        return 100L + outbox.size();
    }

    @Override
    public long consigner(VirementDecide evenement) {
        if (outboxRefusee) {
            throw new IllegalStateException("outbox refusée");
        }
        decisions.add(evenement);
        return 300L + decisions.size();
    }

    @Override
    public void publier(long idLigne) {
        if (idLigne >= 300) {
            decisionsPubliees.add(idLigne);
        } else if (idLigne >= 200) {
            auditPublie.add(idLigne);
        } else {
            publies.add(idLigne);
        }
    }

    @Override
    public long consigner(EntreeAudit entree) {
        if (auditRefuse) {
            throw new IllegalStateException("audit refusé");
        }
        outboxAudit.add(entree);
        return 200L + outboxAudit.size();
    }

    @Override
    public boolean marquerTraite(UUID idEvenement, Instant traiteLe) {
        return traites.add(idEvenement);
    }

    @Override
    public <T> T executer(Supplier<T> travail) {
        Map<Long, Compte> comptesAvant = new HashMap<>(comptes);
        int operationsAvant = operations.size();
        Map<Long, Virement> virementsAvant = new HashMap<>(virements);
        Map<String, Long> clesAvant = new HashMap<>(cles);
        int outboxAvant = outbox.size();
        int decisionsAvant = decisions.size();
        int alertesAvant = alertes.size();
        try {
            return travail.get();
        } catch (RuntimeException e) {
            comptes.clear();
            comptes.putAll(comptesAvant);
            while (operations.size() > operationsAvant) {
                operations.removeLast();
            }
            virements.clear();
            virements.putAll(virementsAvant);
            cles.clear();
            cles.putAll(clesAvant);
            while (outbox.size() > outboxAvant) {
                outbox.removeLast();
            }
            while (decisions.size() > decisionsAvant) {
                decisions.removeLast();
            }
            while (alertes.size() > alertesAvant) {
                alertes.removeLast();
            }
            throw e;
        }
    }

    @Override
    public <T> T executerIndependamment(Supplier<T> travail) {
        return travail.get();
    }

    @Override
    public Instant instant() {
        return MAINTENANT;
    }

    @Override
    public LocalDate aujourdhui() {
        return LocalDate.of(2026, 11, 17);
    }

    @Override
    public ZoneId fuseau() {
        return ZoneId.of("Europe/Paris");
    }
}
