package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Refus pour erreur du contrôle (FR-003-27, BUG-084) : accès à l'historique en échec ou trop long. Le virement est
 * persisté REFUSÉ_ERREUR_CONTRÔLE, sans solde, opération, remise ni notification ; seule l'entrée d'audit
 * « Issue: REFUSE_ERREUR_CONTROLE » est écrite.
 */
@Tag("FR-003-27")
@Tag("FR-027-02")
@Tag("FR-026-02")
@Tag("BUG-084")
class ControleFraudeErreurTest {

    private final MemoireCoeurBancaire memoire = RevueTestes.memoire();
    private ExecuterVirementCasUsage cas;

    @BeforeEach
    void preparer() {
        memoire.maintenant = RevueTestes.SOUMIS;
        memoire.historiqueEnErreur = new IllegalStateException("verrou non obtenu dans le délai");
        cas = ExecuterVirementCasUsageTest.assembler(memoire);
    }

    private ErreurVirement soumettre(String montant, String cle) {
        return catchThrowableOfType(ErreurVirement.class,
                () -> cas.executer(new Commande(7, 1L, 2L, montant, "loyer", cle, RevueTestes.IP)));
    }

    private void riendDebiteNiEmis() {
        assertThat(memoire.comptes.get(1L).solde()).isEqualByComparingTo("20000.00");
        assertThat(memoire.comptes.get(2L).solde()).isEqualByComparingTo("100.00");
        assertThat(memoire.operations).isEmpty();
        assertThat(memoire.outbox).isEmpty();
        assertThat(memoire.decisions).isEmpty();
        assertThat(memoire.decisionsPubliees).isEmpty();
        assertThat(memoire.alertes).isEmpty();
        assertThat(memoire.journal).doesNotContain("debiter", "crediter");
    }

    @Test
    void un_virement_de_50_euros_puis_de_50000_euros_est_refuse_et_persiste_sans_aucun_effet() {
        memoire.comptes.put(1L, new fr.nextbanking.coeurbancaire.domain.Compte(1, 7, "FR76-0001-0001",
                fr.nextbanking.coeurbancaire.domain.TypeCompte.COURANT, new BigDecimal("20000.00")));
        ErreurVirement petit = soumettre("50.00", null);
        ErreurVirement gros = soumettre("20000.00", null);

        assertThat(petit.motif()).isEqualTo(Cause.CONTROLE_EN_ERREUR);
        assertThat(gros.motif()).isEqualTo(Cause.CONTROLE_EN_ERREUR);
        assertThat(memoire.virements.values()).extracting(Virement::etat)
                .containsExactly(EtatVirement.REFUSE_ERREUR_CONTROLE, EtatVirement.REFUSE_ERREUR_CONTROLE);
        riendDebiteNiEmis();
    }

    @Test
    void l_audit_ne_contient_que_l_entree_virement_issue_refuse_erreur_controle() {
        soumettre("50.00", null);

        assertThat(memoire.outboxAudit).singleElement().satisfies(entree -> {
            assertThat(entree.action()).isEqualTo(ActionAudit.VIREMENT);
            assertThat(entree.detail()).isEqualTo("Montant: 50.00 - Motif: loyer - Issue: REFUSE_ERREUR_CONTROLE");
            assertThat(entree.utilisateur()).isEqualTo("7");
        });
    }

    @Test
    void l_erreur_d_origine_est_conservee_pour_le_journal_et_la_trace() {
        ErreurVirement erreur = soumettre("50.00", null);

        assertThat(erreur.getCause()).hasRootCauseMessage("verrou non obtenu dans le délai");
    }

    @Test
    void la_meme_cle_d_idempotence_refuse_de_nouveau_sans_nouveau_virement() {
        soumettre("50.00", "cle-e");
        memoire.historiqueEnErreur = null;

        ErreurVirement rejeu = soumettre("50.00", "cle-e");

        assertThat(rejeu.motif()).isEqualTo(Cause.CONTROLE_EN_ERREUR);
        assertThat(memoire.virements).hasSize(1);
        assertThat(memoire.outboxAudit).hasSize(1);
    }

    @Test
    void un_virement_refuse_non_enregistre_ou_un_audit_non_ecrit_n_empeche_pas_la_reponse_d_erreur() {
        MemoireCoeurBancaire fragile = new MemoireCoeurBancaire() {
            @Override
            public void enregistrer(Virement virement, String cleIdempotence) {
                throw new IllegalStateException("enregistrement impossible");
            }
        };
        fragile.comptes.putAll(memoire.comptes);
        fragile.historiqueEnErreur = new IllegalStateException("historique");
        fragile.auditRefuse = true;
        ExecuterVirementCasUsage fragileCas = ExecuterVirementCasUsageTest.assembler(fragile);

        ErreurVirement erreur = catchThrowableOfType(ErreurVirement.class,
                () -> fragileCas.executer(new Commande(7, 1L, 2L, "50.00", null, null, RevueTestes.IP)));

        assertThat(erreur.motif()).isEqualTo(Cause.CONTROLE_EN_ERREUR);
        assertThat(fragile.virements).isEmpty();
        assertThat(fragile.outboxAudit).isEqualTo(List.of());
    }
}
