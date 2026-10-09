package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.OperationEnregistree;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import fr.nextbanking.coeurbancaire.domain.Virement;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Événement {@code operation-enregistree} : un par opération écrite (deux par virement : débit puis crédit), consigné
 * dans la transaction de l'écriture, avec la version du compte relue après l'écriture ; rien si l'écriture est annulée.
 */
@Tag("FR-017-04")
class OperationEnregistreeCasUsageTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();

    @BeforeEach
    void preparer() {
        memoire.comptes.put(1L, new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00")));
        memoire.comptes.put(2L, new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("100.00")));
        memoire.maintenant = MAINTENANT;
        memoire.beneficiairesActifs.put(7L, Set.of("FR76-0001-0002"));
    }

    private static Commande commande(String montant) {
        return new Commande(7, 1L, 2L, montant, "loyer", null, "203.0.113.7");
    }

    @Test
    void un_virement_execute_ecrit_deux_evenements_debit_puis_credit_avec_la_version_du_compte() {
        ExecuterVirementCasUsageTest.assembler(memoire).executer(commande("100"));

        assertThat(memoire.operationsEnregistrees).extracting(e -> e.operationId(),
                e -> e.operation().compteId(), e -> e.operation().type(), e -> e.operation().montant().texte(),
                e -> e.operation().soldeApres().texte(), OperationEnregistree::versionAgregat,
                OperationEnregistree::horodatage)
                .containsExactly(Tuple.tuple(1L, 1L, TypeOperation.DEBIT, "100.00", "19900.00", 2L, MAINTENANT),
                        Tuple.tuple(2L, 2L, TypeOperation.CREDIT, "100.00", "200.00", 2L, MAINTENANT));
        assertThat(memoire.operationsEnregistrees).extracting(OperationEnregistree::idEvenement)
                .doesNotHaveDuplicates();
        assertThat(memoire.operationsPubliees).containsExactly(601L, 602L);
    }

    @Test
    void un_virement_suspendu_ou_annule_n_ecrit_aucun_evenement() {
        ExecuterVirementCasUsageTest.assembler(memoire).executer(commande("10000.00"));
        assertThat(memoire.operationsEnregistrees).isEmpty();
        assertThat(memoire.operationsPubliees).isEmpty();

        memoire.debitRefuse = true;
        assertThatThrownBy(() -> ExecuterVirementCasUsageTest.assembler(memoire).executer(commande("100")))
                .isInstanceOf(ErreurVirement.class);
        assertThat(memoire.operationsEnregistrees).isEmpty();
        assertThat(memoire.operationsPubliees).isEmpty();
    }

    @Test
    void une_remise_impossible_annule_les_evenements_d_operation_deja_consignes() {
        memoire.outboxRefusee = true;

        assertThatThrownBy(() -> ExecuterVirementCasUsageTest.assembler(memoire).executer(commande("100")))
                .isInstanceOf(ErreurVirement.class);

        assertThat(memoire.operationsEnregistrees).isEmpty();
        assertThat(memoire.operationsPubliees).isEmpty();
    }

    @Test
    void le_debit_asynchrone_d_un_virement_libere_ecrit_deux_evenements() {
        memoire.virements.put(12L, new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("12000.00"),
                Motif.lire("loyer"), EtatVirement.LIBERE, StatutAffichage.PENDING, RevueTestes.SOUMIS,
                RevueTestes.BUTOIR, "motif"));
        TraiterVirementATraiterCasUsage cas = new TraiterVirementATraiterCasUsage(memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire);

        cas.traiter(UUID.randomUUID(), 12);

        assertThat(memoire.operationsEnregistrees).extracting(e -> e.operation().compteId(),
                e -> e.operation().type(), OperationEnregistree::versionAgregat)
                .containsExactly(Tuple.tuple(1L, TypeOperation.DEBIT, 2L), Tuple.tuple(2L, TypeOperation.CREDIT, 2L));
        assertThat(memoire.operationsPubliees).containsExactly(601L, 602L);
    }

    @Test
    void une_echeance_de_virement_programme_ecrit_deux_evenements() {
        memoire.virementsProgrammes.add(new VirementProgramme(1, 7, 1, "FR76-0001-0002", "Epargne",
                Montant.lire("100.00"), FrequenceVirement.MENSUEL, "Epargne mensuelle",
                Instant.parse("2026-11-17T07:00:00Z"), true, 0, Instant.parse("2026-01-01T00:00:00Z")));
        ExecuterEcheancesCasUsage cas = new ExecuterEcheancesCasUsage(memoire, memoire, memoire, memoire, memoire,
                memoire, memoire, memoire, memoire,
                new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000")),
                new ControleFraudeCasUsage(memoire, memoire, memoire, memoire, ControleFraudeTestes.PARAMETRES));

        assertThat(cas.executerLesDues()).isEqualTo(1);

        assertThat(memoire.operationsEnregistrees).extracting(e -> e.operation().compteId(),
                e -> e.operation().type(), OperationEnregistree::versionAgregat)
                .containsExactly(Tuple.tuple(1L, TypeOperation.DEBIT, 2L), Tuple.tuple(2L, TypeOperation.CREDIT, 2L));
        assertThat(memoire.operationsPubliees).containsExactly(601L, 602L);
    }

    @Test
    void un_evenement_exige_ses_champs_et_des_identifiants_positifs() {
        Operation operation = new Operation(1L, TypeOperation.DEBIT, Montant.lire("1.00"), Montant.lire("2.00"),
                "x", MAINTENANT, 1L);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new OperationEnregistree(null, 1, operation, 1, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationEnregistree(id, 1, null, 1, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationEnregistree(id, 1, operation, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationEnregistree(id, 0, operation, 1, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationEnregistree(id, 1, operation, 0, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new OperationEnregistree(id, 1, operation, 1, MAINTENANT).operationId()).isEqualTo(1L);
    }
}
