package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter.Issue;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import fr.nextbanking.coeurbancaire.domain.EtatCompte;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Événement {@code compte-modifie} : un par compte dont le solde change, écrit dans la transaction de l'écriture
 * (virement exécuté, débit asynchrone d'un virement libéré, échéance d'un virement programmé), avec une version
 * d'agrégat croissante ; rien quand l'écriture est annulée ou qu'aucun solde ne change.
 */
class CompteModifieCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("100.00")));
        memoire.maintenant = MAINTENANT;
        memoire.beneficiairesActifs.put(7L, Set.of("FR76-0001-0002"));
    }

    private ExecuterVirementCasUsage virements() {
        return ExecuterVirementCasUsageTest.assembler(memoire);
    }

    private static Commande commande(String montant) {
        return new Commande(7, 1L, 2L, montant, "loyer", null, "203.0.113.7");
    }

    @Test
    void un_virement_execute_ecrit_un_evenement_par_compte_source_puis_destinataire_et_les_publie_apres_validation() {
        virements().executer(commande("100"));

        assertThat(memoire.comptesModifies).extracting(e -> e.etat().compte().id(), e -> e.etat().compte().clientId(),
                e -> e.etat().compte().solde().texte(), e -> e.etat().versionAgregat(), CompteModifie::horodatage)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 7L, "19900.00", 2L, MAINTENANT),
                        org.assertj.core.groups.Tuple.tuple(2L, 7L, "200.00", 2L, MAINTENANT));
        assertThat(memoire.comptesModifies).extracting(CompteModifie::idEvenement).doesNotHaveDuplicates();
        assertThat(memoire.comptesPublies).containsExactly(501L, 502L);
    }

    @Test
    void la_version_d_un_compte_croit_strictement_a_chaque_virement() {
        ExecuterVirementCasUsage cas = virements();

        cas.executer(commande("100"));
        cas.executer(commande("50"));

        assertThat(memoire.comptesModifies).extracting(e -> e.etat().compte().id(), e -> e.etat().versionAgregat())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 2L), org.assertj.core.groups.Tuple.tuple(2L, 2L),
                        org.assertj.core.groups.Tuple.tuple(1L, 3L), org.assertj.core.groups.Tuple.tuple(2L, 3L));
    }

    @Test
    void un_virement_suspendu_ne_modifie_aucun_compte_et_n_ecrit_aucun_evenement() {
        virements().executer(commande("10000.00"));

        assertThat(memoire.virements.get(12L).etat()).isEqualTo(EtatVirement.SUSPENDU);
        assertThat(memoire.comptesModifies).isEmpty();
        assertThat(memoire.comptesPublies).isEmpty();
    }

    @Test
    void une_ecriture_annulee_n_ecrit_ni_ne_publie_aucun_evenement() {
        memoire.debitRefuse = true;

        assertThatThrownBy(() -> virements().executer(commande("100")))
                .isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);

        assertThat(memoire.comptesModifies).isEmpty();
        assertThat(memoire.comptesPublies).isEmpty();
        assertThat(memoire.versionsComptes).isEmpty();
    }

    @Test
    void une_remise_impossible_annule_les_evenements_deja_consignes_et_les_versions() {
        memoire.outboxRefusee = true;

        assertThatThrownBy(() -> virements().executer(commande("100")))
                .isInstanceOf(fr.nextbanking.coeurbancaire.domain.ErreurVirement.class);

        assertThat(memoire.comptesModifies).isEmpty();
        assertThat(memoire.comptesPublies).isEmpty();
        assertThat(memoire.etat(new CompteId(1)).versionAgregat()).isEqualTo(1L);
    }

    @Test
    void le_debit_asynchrone_d_un_virement_libere_ecrit_un_evenement_par_compte() {
        memoire.virements.put(12L, new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("12000.00"),
                Motif.lire("loyer"), EtatVirement.LIBERE, StatutAffichage.PENDING, RevueTestes.SOUMIS,
                RevueTestes.BUTOIR, "motif"));
        TraiterVirementATraiterCasUsage cas = new TraiterVirementATraiterCasUsage(memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire);

        assertThat(cas.traiter(UUID.randomUUID(), 12)).isEqualTo(Issue.EXECUTE_APRES_LIBERATION);

        assertThat(memoire.comptesModifies).extracting(e -> e.etat().compte().id(),
                e -> e.etat().compte().solde().texte(), e -> e.etat().versionAgregat())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, "8000.00", 2L),
                        org.assertj.core.groups.Tuple.tuple(2L, "12100.00", 2L));
        assertThat(memoire.comptesPublies).containsExactly(501L, 502L);
    }

    @Test
    void un_virement_libere_rejete_pour_solde_insuffisant_ou_deja_execute_n_ecrit_aucun_evenement() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("11999.99")));
        memoire.virements.put(12L, new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("12000.00"),
                Motif.lire("loyer"), EtatVirement.LIBERE, StatutAffichage.PENDING, RevueTestes.SOUMIS,
                RevueTestes.BUTOIR, "motif"));
        TraiterVirementATraiterCasUsage cas = new TraiterVirementATraiterCasUsage(memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire);

        assertThat(cas.traiter(UUID.randomUUID(), 12)).isEqualTo(Issue.REJETE_SOLDE_INSUFFISANT);
        assertThat(cas.traiter(UUID.randomUUID(), 99)).isEqualTo(Issue.VIREMENT_INCONNU);

        assertThat(memoire.comptesModifies).isEmpty();
        assertThat(memoire.comptesPublies).isEmpty();
    }

    @Test
    void une_echeance_de_virement_programme_ecrit_un_evenement_par_compte_modifie() {
        memoire.virementsProgrammes.add(new VirementProgramme(1, 7, 1, "FR76-0001-0002", "Epargne",
                Montant.lire("100.00"), FrequenceVirement.MENSUEL, "Epargne mensuelle",
                Instant.parse("2026-11-17T07:00:00Z"), true, 0, Instant.parse("2026-01-01T00:00:00Z")));
        ExecuterEcheancesCasUsage cas = new ExecuterEcheancesCasUsage(memoire, memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000")),
                new ControleFraudeCasUsage(memoire, memoire, memoire, memoire, ControleFraudeTestes.PARAMETRES));

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.comptesModifies).extracting(e -> e.etat().compte().id(),
                e -> e.etat().compte().solde().texte(), e -> e.etat().versionAgregat())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, "19900.00", 2L),
                        org.assertj.core.groups.Tuple.tuple(2L, "200.00", 2L));
        assertThat(memoire.comptesPublies).containsExactly(501L, 502L);
    }

    @Test
    void une_echeance_dont_le_destinataire_est_introuvable_ou_au_solde_insuffisant_n_ecrit_aucun_evenement() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("10.00")));
        memoire.virementsProgrammes.add(new VirementProgramme(1, 7, 1, "FR76-0001-0002", "Epargne",
                Montant.lire("100.00"), FrequenceVirement.MENSUEL, null, Instant.parse("2026-11-17T07:00:00Z"), true,
                0, Instant.parse("2026-01-01T00:00:00Z")));
        memoire.virementsProgrammes.add(new VirementProgramme(2, 7, 1, "FR76-9999-9999", "Inconnu",
                Montant.lire("5.00"), FrequenceVirement.MENSUEL, null, Instant.parse("2026-11-17T07:00:00Z"), true,
                0, Instant.parse("2026-01-01T00:00:00Z")));
        ExecuterEcheancesCasUsage cas = new ExecuterEcheancesCasUsage(memoire, memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000")),
                new ControleFraudeCasUsage(memoire, memoire, memoire, memoire, ControleFraudeTestes.PARAMETRES));

        assertThat(cas.executerLesDues()).isZero();

        assertThat(memoire.comptesModifies).isEmpty();
        assertThat(memoire.comptesPublies).isEmpty();
    }

    @Test
    void un_evenement_et_un_etat_exigent_leurs_champs_et_une_version_positive() {
        EtatCompte etat = new EtatCompte(memoire.comptes.get(1L), 1);

        assertThatThrownBy(() -> new EtatCompte(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EtatCompte(memoire.comptes.get(1L), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompteModifie(null, etat, MAINTENANT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompteModifie(UUID.randomUUID(), null, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CompteModifie(UUID.randomUUID(), etat, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(List.of(etat)).hasSize(1);
    }
}
