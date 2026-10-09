package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import fr.nextbanking.coeurbancaire.domain.EtatCompte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import fr.nextbanking.coeurbancaire.domain.LigneTraitee;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.OperationEnregistree;
import fr.nextbanking.coeurbancaire.domain.EcheanceNonExecutee;
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
        EvenementsTraites, Transaction, Horloge, HistoriqueVirements, Beneficiaires, AlertesFraude, JournalBatch,
        ProjectionClients, VirementsProgrammes, Plafonds {

    static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");

    final Map<Long, Compte> comptes = new HashMap<>();
    final List<Operation> operations = new ArrayList<>();
    final Map<Long, Virement> virements = new HashMap<>();
    final Map<String, Long> cles = new HashMap<>();
    final List<VirementATraiter> outbox = new ArrayList<>();
    final List<VirementDecide> decisions = new ArrayList<>();
    final List<Long> decisionsPubliees = new ArrayList<>();
    final List<Long> echeancesPubliees = new ArrayList<>();
    /** Événements compte-modifie consignés (lignes 500 et suivantes), et lignes publiées. */
    final List<CompteModifie> comptesModifies = new ArrayList<>();
    final List<OperationEnregistree> operationsEnregistrees = new ArrayList<>();
    final List<Long> operationsPubliees = new ArrayList<>();
    final List<Long> comptesPublies = new ArrayList<>();
    /** Version d'agrégat des comptes écrits (1 tant qu'aucun débit ni crédit). */
    final Map<Long, Long> versionsComptes = new HashMap<>();
    final List<Long> publies = new ArrayList<>();
    final List<EntreeAudit> outboxAudit = new ArrayList<>();
    final List<Long> auditPublie = new ArrayList<>();
    final Set<UUID> traites = new HashSet<>();
    final List<String> journal = new ArrayList<>();
    final List<AlerteFraude> alertes = new ArrayList<>();
    /** Numéros des bénéficiaires actifs, par client. */
    final Map<Long, Set<String>> beneficiairesActifs = new HashMap<>();
    /** Bénéficiaires externes actifs, par identifiant (la désactivation les retire). */
    final Map<Long, BeneficiaireExterne> beneficiairesExternes = new HashMap<>();
    /** Désactivations perdues d'avance (un autre traitement a gagné la course). */
    final Set<Long> desactivationsPerdues = new HashSet<>();
    final List<Ajout> ajouts = new ArrayList<>();
    boolean ajoutEnErreur;
    /** Signaux d'historique forcés ; absents : calculés sur les opérations et virements écrits. */
    Long operationsDuJour;
    Optional<Instant> dernierVirement;
    Long virementsImportants;
    RuntimeException historiqueEnErreur;

    /** Décisions de revue enregistrées par transition, par virement. */
    final Map<Long, DecisionRevue> decisionsRevue = new HashMap<>();
    /** Nom complet des clients, projection locale. */
    final Map<Long, String> noms = new HashMap<>();
    final Map<Long, Long> versionsClients = new HashMap<>();
    /** Conseiller affecté (nul possible) et clients supprimés, projection locale. */
    final Map<Long, Long> conseillers = new HashMap<>();
    final Set<Long> supprimes = new HashSet<>();
    /** Lectures par lot des comptes, pour vérifier qu'elles sont uniques. */
    final List<java.util.Collection<Long>> lecturesParLot = new ArrayList<>();
    final List<String> lignesBatch = new ArrayList<>();
    /** Transitions refusées d'avance (un autre traitement a gagné la course). */
    final Set<Long> transitionsPerdues = new HashSet<>();
    boolean batchRefuse;
    boolean lectureEcheancesEnErreur;
    boolean transitionEnErreur;

    /** Instant de l'horloge de test ; réglable. */
    Instant maintenant = MAINTENANT;

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
    public List<Compte> desClients(java.util.Collection<Long> clientIds) {
        lecturesParLot.add(List.copyOf(clientIds));
        return comptes.values().stream().filter(c -> clientIds.contains(c.clientId()))
                .sorted(java.util.Comparator.comparingLong(Compte::id).reversed()).toList();
    }

    @Override
    public Optional<Compte> trouver(long compteId) {
        return Optional.ofNullable(comptes.get(compteId));
    }

    @Override
    public Optional<Compte> parNumero(String numero) {
        return comptes.values().stream().filter(c -> c.numero().equals(numero)).findFirst();
    }

    /** Virements programmés enregistrés, dans l'ordre d'écriture. */
    final List<VirementProgramme> virementsProgrammes = new ArrayList<>();
    boolean enregistrementProgrammeEnErreur;
    /** Dernière exécution enregistrée par la prise d'une échéance, par identifiant. */
    final Map<Long, Instant> derniereExecution = new HashMap<>();
    /** Prises d'échéance perdues d'avance (un autre passage a gagné la course). */
    final Set<Long> prisesPerdues = new HashSet<>();
    boolean lectureDuesEnErreur;
    /** Échéances notifiées : « identifiant|échéance ». */
    final Set<String> echeancesNotifiees = new HashSet<>();
    final List<EcheanceNonExecutee> echeancesNonExecutees = new ArrayList<>();
    /** Désactivations d'échéance perdues d'avance (un autre traitement a gagné la course). */
    final Set<Long> desactivationsEcheancePerdues = new HashSet<>();
    boolean marqueEnErreur;

    @Override
    public boolean desactiver(long id, Instant echeancePrevue) {
        journal.add("desactiver-echeance");
        for (int i = 0; i < virementsProgrammes.size(); i++) {
            VirementProgramme v = virementsProgrammes.get(i);
            if (v.id() == id && v.actif() && v.prochaineExecution().equals(echeancePrevue)
                    && !desactivationsEcheancePerdues.contains(id)) {
                virementsProgrammes.set(i, new VirementProgramme(v.id(), v.clientId(), v.compteSourceId(),
                        v.numeroBeneficiaire(), v.libelleBeneficiaire(), v.montant(), v.frequence(), v.motif(),
                        v.prochaineExecution(), false, v.nbExecutions(), v.creeLe()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean marquerNotifiee(long id, Instant echeance, Instant le) {
        journal.add("marquer");
        if (marqueEnErreur) {
            throw new IllegalStateException("marque en erreur");
        }
        return echeancesNotifiees.add(id + "|" + echeance);
    }

    @Override
    public long consigner(EcheanceNonExecutee evenement) {
        if (outboxRefusee) {
            throw new IllegalStateException("outbox refusée");
        }
        echeancesNonExecutees.add(evenement);
        return 400L + echeancesNonExecutees.size();
    }

    @Override
    public VirementProgramme enregistrer(long clientId, DemandeVirementProgramme demande,
            Instant prochaineExecution, Instant creeLe) {
        if (enregistrementProgrammeEnErreur) {
            throw new IllegalStateException("base indisponible");
        }
        VirementProgramme cree = new VirementProgramme(virementsProgrammes.size() + 1L, clientId,
                demande.compteSourceId(), demande.numeroBeneficiaire(), demande.libelleBeneficiaire(),
                demande.montant(), demande.frequence(), demande.motif(), prochaineExecution, true, 0, creeLe);
        virementsProgrammes.add(cree);
        return cree;
    }

    @Override
    public boolean changerEtat(long id, long clientId, boolean actifAttendu) {
        for (int i = 0; i < virementsProgrammes.size(); i++) {
            VirementProgramme v = virementsProgrammes.get(i);
            if (v.id() == id && v.clientId() == clientId && v.actif() == actifAttendu) {
                virementsProgrammes.set(i, new VirementProgramme(v.id(), v.clientId(), v.compteSourceId(),
                        v.numeroBeneficiaire(), v.libelleBeneficiaire(), v.montant(), v.frequence(), v.motif(),
                        v.prochaineExecution(), !actifAttendu, v.nbExecutions(), v.creeLe()));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<VirementProgramme> dues(Instant maintenant) {
        if (lectureDuesEnErreur) {
            throw new IllegalStateException("lecture des échéances en erreur");
        }
        return virementsProgrammes.stream().filter(v -> v.actif() && !v.prochaineExecution().isAfter(maintenant))
                .sorted(java.util.Comparator.comparing(VirementProgramme::prochaineExecution)
                        .thenComparingLong(VirementProgramme::id)).toList();
    }

    @Override
    public boolean prendreEcheance(long id, Instant echeancePrevue, Instant prochaine, Instant executeLe) {
        journal.add("prendre");
        for (int i = 0; i < virementsProgrammes.size(); i++) {
            VirementProgramme v = virementsProgrammes.get(i);
            if (v.id() == id && v.actif() && v.prochaineExecution().equals(echeancePrevue)
                    && !prisesPerdues.contains(id)) {
                virementsProgrammes.set(i, new VirementProgramme(v.id(), v.clientId(), v.compteSourceId(),
                        v.numeroBeneficiaire(), v.libelleBeneficiaire(), v.montant(), v.frequence(), v.motif(),
                        prochaine, v.actif(), v.nbExecutions() + 1, v.creeLe()));
                derniereExecution.put(id, executeLe);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supprimer(long id, long clientId) {
        return virementsProgrammes.removeIf(v -> v.id() == id && v.clientId() == clientId);
    }

    @Override
    public List<VirementProgramme> listerDuClient(long clientId) {
        return virementsProgrammes.stream().filter(v -> v.clientId() == clientId)
                .sorted(java.util.Comparator.comparing(VirementProgramme::prochaineExecution)
                        .thenComparingLong(VirementProgramme::id)).toList();
    }

    @Override
    public List<Compte> verrouiller(long premierId, long secondId) {
        journal.add("verrouiller");
        return java.util.stream.Stream.of(premierId, secondId).sorted().map(comptes::get)
                .filter(java.util.Objects::nonNull).toList();
    }

    @Override
    public boolean debiter(CompteId compteId, Montant montant) {
        journal.add("debiter");
        Compte compte = comptes.get(compteId.valeur());
        if (debitRefuse || compte.solde().compareTo(montant) < 0) {
            return false;
        }
        comptes.put(compteId.valeur(), new Compte(compteId.valeur(), compte.clientId(), compte.numero(), compte.type(),
                compte.solde().soustraire(montant)));
        versionsComptes.merge(compteId.valeur(), 2L, (ancienne, ignoree) -> ancienne + 1);
        return true;
    }

    @Override
    public void crediter(CompteId compteId, Montant montant) {
        journal.add("crediter");
        Compte compte = comptes.get(compteId.valeur());
        comptes.put(compteId.valeur(), new Compte(compteId.valeur(), compte.clientId(), compte.numero(), compte.type(),
                compte.solde().ajouter(montant)));
        versionsComptes.merge(compteId.valeur(), 2L, (ancienne, ignoree) -> ancienne + 1);
    }

    @Override
    public EtatCompte etat(CompteId compteId) {
        return new EtatCompte(comptes.get(compteId.valeur()), versionsComptes.getOrDefault(compteId.valeur(), 1L));
    }

    @Override
    public long enregistrer(Operation operation) {
        operations.add(operation);
        return operations.size();
    }

    /** Opérations du compte dans l'ordre où elles ont été inscrites : le cas d'usage décide du tri. */
    @Override
    public List<Operation> duCompte(long compteId) {
        return operations.stream().filter(o -> o.compteId() == compteId).toList();
    }

    /** Mois (fuseau donné) où le client a des opérations, du plus récent au plus ancien. */
    @Override
    public List<java.time.YearMonth> moisAvecOperations(long clientId, ZoneId fuseau, int limite) {
        return operations.stream().filter(o -> comptes.get(o.compteId()).clientId() == clientId)
                .map(o -> java.time.YearMonth.from(o.date().atZone(fuseau))).distinct()
                .sorted(java.util.Comparator.reverseOrder()).limit(limite).toList();
    }

    @Override
    public List<Operation> duClientSurPeriode(long clientId, Instant debut, Instant fin) {
        return operations.stream().filter(o -> comptes.get(o.compteId()).clientId() == clientId)
                .filter(o -> !o.date().isBefore(debut) && o.date().isBefore(fin)).toList();
    }

    /** Plafonds définis, par client. */
    record PlafondMemoire(long clientId, Plafond plafond) {
    }

    final List<PlafondMemoire> plafonds = new ArrayList<>();

    @Override
    public long compter(long clientId, String mois) {
        return duMois(clientId, mois).size();
    }

    @Override
    public List<Plafond> duMois(long clientId, String mois) {
        return plafonds.stream().filter(p -> p.clientId() == clientId && p.plafond().mois().equals(mois))
                .map(PlafondMemoire::plafond).toList();
    }

    @Override
    public Plafond enregistrer(long clientId, fr.nextbanking.coeurbancaire.domain.CategorieBudget categorie,
            String mois, Montant plafond) {
        long id = plafonds.stream().filter(p -> p.clientId() == clientId && p.plafond().mois().equals(mois)
                && p.plafond().categorie() == categorie).map(p -> p.plafond().id()).findFirst().orElse(prochainId++);
        plafonds.removeIf(p -> p.plafond().id() == id);
        Plafond enregistre = new Plafond(id, categorie, mois, plafond);
        plafonds.add(new PlafondMemoire(clientId, enregistre));
        return enregistre;
    }

    @Override
    public boolean retirer(long clientId, long plafondId) {
        return plafonds.removeIf(p -> p.clientId() == clientId && p.plafond().id() == plafondId);
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
    public boolean transiter(long virementId, EtatVirement de, EtatVirement vers, DecisionRevue decision) {
        journal.add("transiter " + de + "->" + vers);
        if (transitionEnErreur) {
            throw new IllegalStateException("transition en erreur");
        }
        Virement virement = virements.get(virementId);
        if (transitionsPerdues.contains(virementId) || virement == null || virement.etat() != de
                || !de.peutTransiterVers(vers)) {
            return false;
        }
        virements.put(virementId, new Virement(virement.id(), virement.clientId(), virement.compteSourceId(),
                virement.compteDestinataireId(), virement.montant(), virement.motif(), vers, virement.statut(),
                virement.soumisLe(), virement.heureButoir(), virement.motifSuspension()));
        if (decision != null) {
            decisionsRevue.put(virementId, decision);
        }
        return true;
    }

    @Override
    public List<Virement> suspendusEchus(Instant maintenant) {
        if (lectureEcheancesEnErreur) {
            throw new IllegalStateException("lecture en erreur");
        }
        return virements.values().stream().filter(v -> v.etat() == EtatVirement.SUSPENDU)
                .filter(v -> v.heureButoirAtteinte(maintenant))
                .sorted(java.util.Comparator.comparing(Virement::heureButoir).thenComparingLong(Virement::id))
                .toList();
    }

    @Override
    public List<LigneRevue> fileDeRevue() {
        return virements.values().stream().filter(v -> v.etat() == EtatVirement.SUSPENDU)
                .sorted(java.util.Comparator.comparing(Virement::heureButoir).thenComparingLong(Virement::id))
                .map(this::ligneRevue).toList();
    }

    private LigneRevue ligneRevue(Virement v) {
        return new LigneRevue(v.id(), v.clientId(), noms.get(v.clientId()), v.montant().valeur(),
                comptes.get(v.compteDestinataireId().valeur()).numero(), v.motifSuspension(), v.heureButoir());
    }

    @Override
    public List<LigneTraitee> historiqueDesTraites(int limite) {
        return decisionsRevue.entrySet().stream()
                .sorted(java.util.Comparator.<Map.Entry<Long, DecisionRevue>, Instant>comparing(e -> e.getValue().date())
                        .reversed().thenComparing(java.util.Map.Entry::getKey, java.util.Comparator.reverseOrder()))
                .limit(limite)
                .map(e -> new LigneTraitee(ligneRevue(virements.get(e.getKey())), e.getValue().issue(),
                        e.getValue().date()))
                .toList();
    }

    @Override
    public List<LigneSuivi> suiviDuClient(long clientId, int limite) {
        return virements.values().stream().filter(v -> v.clientId() == clientId && v.heureButoir() != null)
                .sorted(java.util.Comparator.comparing(Virement::soumisLe).reversed()
                        .thenComparing(java.util.Comparator.comparingLong(Virement::id).reversed()))
                .limit(limite)
                .map(v -> new LigneSuivi(v.id(), v.soumisLe(), comptes.get(v.compteDestinataireId().valeur()).numero(),
                        v.montant().valeur(), v.etat(), v.heureButoir()))
                .toList();
    }

    @Override
    public int passerEnRevue(long virementId, String revuPar, Instant revueLe) {
        journal.add("revue");
        int passees = 0;
        for (int i = 0; i < alertes.size(); i++) {
            AlerteFraude alerte = alertes.get(i);
            if (alerte.virementId() == virementId && alerte.statut() == StatutAlerte.OPEN) {
                alertes.set(i, new AlerteFraude(alerte.clientId(), alerte.virementId(), alerte.regle(),
                        alerte.score(), alerte.description(), StatutAlerte.REVIEWED, alerte.creeLe()));
                passees++;
            }
        }
        return passees;
    }

    @Override
    public void enregistrer(String job, Instant debut, Instant fin, int enregistrements, Statut statut,
            String details) {
        if (batchRefuse) {
            throw new IllegalStateException("journal d'exécution refusé");
        }
        lignesBatch.add(job + "|" + enregistrements + "|" + statut + "|" + details);
    }

    @Override
    public boolean enregistrerSiPlusRecent(ClientProjete client) {
        Long connue = versionsClients.get(client.clientId());
        if (connue != null && connue >= client.versionAgregat()) {
            return false;
        }
        versionsClients.put(client.clientId(), client.versionAgregat());
        noms.put(client.clientId(), client.nomComplet());
        conseillers.put(client.clientId(), client.conseillerId());
        if (client.supprime()) {
            supprimes.add(client.clientId());
        } else {
            supprimes.remove(client.clientId());
        }
        return true;
    }

    @Override
    public List<Long> clientsDuConseiller(long conseillerId) {
        return conseillers.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() == conseillerId && !supprimes.contains(e.getKey()))
                .map(Map.Entry::getKey).sorted().toList();
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
                .filter(v -> v.etat() == EtatVirement.EXECUTE).filter(v -> v.montant().valeur().compareTo(montantMin) > 0)
                .filter(v -> !v.soumisLe().isBefore(depuis)).count();
    }

    @Override
    public boolean estActif(long clientId, String numero) {
        return beneficiairesActifs.getOrDefault(clientId, Set.of()).contains(numero);
    }

    @Override
    public List<BeneficiaireExterne> actifsDuClient(long clientId) {
        return beneficiairesExternes.values().stream().filter(b -> b.clientId() == clientId)
                .sorted(java.util.Comparator.comparingLong(BeneficiaireExterne::id).reversed()).toList();
    }

    @Override
    public Optional<BeneficiaireExterne> trouverActif(long beneficiaireId) {
        return Optional.ofNullable(beneficiairesExternes.get(beneficiaireId));
    }

    @Override
    public BeneficiaireExterne ajouter(long clientId, fr.nextbanking.coeurbancaire.domain.DemandeBeneficiaire demande,
            Instant creeLe) {
        journal.add("ajouter " + clientId);
        if (ajoutEnErreur) {
            throw new IllegalStateException("base indisponible");
        }
        long id = beneficiairesExternes.keySet().stream().mapToLong(Long::longValue).max().orElse(0) + 1;
        BeneficiaireExterne cree = new BeneficiaireExterne(id, clientId, demande.libelle(), demande.titulaire(),
                demande.iban(), demande.bic(), demande.banque());
        beneficiairesExternes.put(id, cree);
        ajouts.add(new Ajout(cree, demande.zoneSepa(), creeLe));
        return cree;
    }

    /** Ajout enregistré : bénéficiaire, zone SEPA calculée et date de création. */
    record Ajout(BeneficiaireExterne beneficiaire, boolean zoneSepa, Instant creeLe) {
    }

    @Override
    public boolean desactiver(long beneficiaireId, long clientId) {
        journal.add("desactiver " + beneficiaireId);
        BeneficiaireExterne actif = beneficiairesExternes.get(beneficiaireId);
        if (desactivationsPerdues.contains(beneficiaireId) || actif == null || !actif.appartientA(clientId)) {
            return false;
        }
        beneficiairesExternes.remove(beneficiaireId);
        return true;
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
    public long consigner(CompteModifie evenement) {
        if (outboxRefusee) {
            throw new IllegalStateException("outbox refusée");
        }
        comptesModifies.add(evenement);
        return 500L + comptesModifies.size();
    }

    @Override
    public long consigner(OperationEnregistree evenement) {
        if (outboxRefusee) {
            throw new IllegalStateException("outbox refusée");
        }
        operationsEnregistrees.add(evenement);
        return 600L + operationsEnregistrees.size();
    }

    @Override
    public void publier(long idLigne) {
        if (idLigne >= 600) {
            operationsPubliees.add(idLigne);
        } else if (idLigne >= 500) {
            comptesPublies.add(idLigne);
        } else if (idLigne >= 400) {
            echeancesPubliees.add(idLigne);
        } else if (idLigne >= 300) {
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
        int comptesModifiesAvant = comptesModifies.size();
        int operationsEnregistreesAvant = operationsEnregistrees.size();
        Map<Long, Long> versionsAvant = new HashMap<>(versionsComptes);
        List<AlerteFraude> alertesAvant = new ArrayList<>(alertes);
        Map<Long, DecisionRevue> decisionsRevueAvant = new HashMap<>(decisionsRevue);
        Set<UUID> traitesAvant = new HashSet<>(traites);
        Set<String> notifieesAvant = new HashSet<>(echeancesNotifiees);
        int echeancesAvant = echeancesNonExecutees.size();
        List<VirementProgramme> programmesAvant = new ArrayList<>(virementsProgrammes);
        Map<Long, Instant> derniereAvant = new HashMap<>(derniereExecution);
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
            while (comptesModifies.size() > comptesModifiesAvant) {
                comptesModifies.removeLast();
            }
            while (operationsEnregistrees.size() > operationsEnregistreesAvant) {
                operationsEnregistrees.removeLast();
            }
            versionsComptes.clear();
            versionsComptes.putAll(versionsAvant);
            alertes.clear();
            alertes.addAll(alertesAvant);
            decisionsRevue.clear();
            decisionsRevue.putAll(decisionsRevueAvant);
            traites.clear();
            traites.addAll(traitesAvant);
            echeancesNotifiees.clear();
            echeancesNotifiees.addAll(notifieesAvant);
            while (echeancesNonExecutees.size() > echeancesAvant) {
                echeancesNonExecutees.removeLast();
            }
            virementsProgrammes.clear();
            virementsProgrammes.addAll(programmesAvant);
            derniereExecution.clear();
            derniereExecution.putAll(derniereAvant);
            throw e;
        }
    }

    @Override
    public <T> T executerIndependamment(Supplier<T> travail) {
        return travail.get();
    }

    @Override
    public Instant instant() {
        return maintenant;
    }

    @Override
    public LocalDate aujourdhui() {
        return maintenant.atZone(fuseau()).toLocalDate();
    }

    @Override
    public ZoneId fuseau() {
        return ZoneId.of("Europe/Paris");
    }
}
