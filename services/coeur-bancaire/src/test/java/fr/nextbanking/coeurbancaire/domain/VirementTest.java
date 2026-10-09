package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Règles du virement : contrôles de saisie dans l'ordre de FR-003-07, propriété des comptes, solde, statut
 * d'affichage et opérations au relevé.
 */
class VirementTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-17T09:00:00Z");
    private static final SeuilsFraude SEUILS = new SeuilsFraude(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("10000"));
    private static final Compte COURANT = new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2130.00"));
    private static final Compte EPARGNE = new Compte(2, 7, "FR76-0001-0002", TypeCompte.EPARGNE, Montant.lire("15200.00"));

    private static Soumission soumission(long id, Compte source, Compte destinataire, String montant, String motif) {
        return new Soumission(id, 7, DemandeVirement.lire(source.id(), destinataire.id(), montant, motif), MAINTENANT);
    }

    private static Cause refus(Long source, Long destinataire, String montant, String motif) {
        return org.assertj.core.api.Assertions.catchThrowableOfType(ErreurVirement.class,
                () -> DemandeVirement.lire(source, destinataire, montant, motif)).motif();
    }

    @Test
    @Tag("FR-003-02")
    void les_deux_comptes_sont_obligatoires() {
        assertThat(refus(null, 2L, "100", null)).isEqualTo(Cause.COMPTES_OBLIGATOIRES);
        assertThat(refus(1L, null, "100", null)).isEqualTo(Cause.COMPTES_OBLIGATOIRES);
        assertThat(refus(null, null, null, null)).isEqualTo(Cause.COMPTES_OBLIGATOIRES);
        assertThatThrownBy(() -> DemandeVirement.lire(null, 2L, "100", null))
                .hasMessage("Veuillez sélectionner les deux comptes");
    }

    @Test
    @Tag("FR-003-03")
    void les_comptes_source_et_destinataire_sont_differents() {
        assertThat(refus(1L, 1L, "100", "[retro] test comptes identiques")).isEqualTo(Cause.COMPTES_IDENTIQUES);
        assertThatThrownBy(() -> DemandeVirement.lire(1L, 1L, "100", null))
                .hasMessage("Le compte source et destination doivent être différents");
    }

    @Test
    @Tag("FR-003-04")
    void le_montant_est_strictement_positif_a_deux_decimales_au_plus() {
        for (String montant : new String[] {null, "", "abc", "NaN", "0", "0.00", "-5", "-0.01", "100.001", "1E20",
                "1E999999999", "999999999999999999.99", "12,5"}) {
            assertThat(refus(1L, 2L, montant, null)).as(String.valueOf(montant)).isEqualTo(Cause.MONTANT_INVALIDE);
        }
        assertThatThrownBy(() -> DemandeVirement.lire(1L, 2L, "abc", null))
                .hasMessage("Erreur lors du virement : Le montant doit etre positif");
    }

    @Test
    @Tag("FR-003-04")
    void le_montant_valide_est_rendu_a_deux_decimales() {
        assertThat(DemandeVirement.lire(1L, 2L, "100", null).montant().texte()).isEqualTo("100.00");
        assertThat(DemandeVirement.lire(1L, 2L, " 12.5 ", null).montant().texte()).isEqualTo("12.50");
        assertThat(DemandeVirement.lire(1L, 2L, "0.01", null).montant().texte()).isEqualTo("0.01");
        assertThat(DemandeVirement.lire(1L, 2L, "1E+3", null).montant().texte()).isEqualTo("1000.00");
        assertThat(DemandeVirement.lire(1L, 2L, "99999999999999999.99", null).montant().valeur().scale()).isEqualTo(2);
    }

    @Test
    @Tag("FR-003-01")
    void le_motif_ne_depasse_pas_100_caracteres() {
        assertThat(refus(1L, 2L, "100", "x".repeat(101))).isEqualTo(Cause.MOTIF_TROP_LONG);
        assertThatThrownBy(() -> DemandeVirement.lire(1L, 2L, "100", "x".repeat(101)))
                .hasMessage("Le motif ne doit pas dépasser 100 caractères");
        assertThat(DemandeVirement.lire(1L, 2L, "100", "x".repeat(100)).montant().texte()).isEqualTo("100.00");
        assertThat(DemandeVirement.lire(1L, 2L, "100", null).montant().texte()).isEqualTo("100.00");
    }

    @Test
    @Tag("FR-003-07")
    void les_controles_s_appliquent_dans_l_ordre_comptes_identiques_montant_motif() {
        assertThat(refus(null, null, "abc", "x".repeat(101))).isEqualTo(Cause.COMPTES_OBLIGATOIRES);
        assertThat(refus(1L, 1L, "abc", "x".repeat(101))).isEqualTo(Cause.COMPTES_IDENTIQUES);
        assertThat(refus(1L, 2L, "abc", "x".repeat(101))).isEqualTo(Cause.MONTANT_INVALIDE);
        assertThat(refus(1L, 2L, "100", "x".repeat(101))).isEqualTo(Cause.MOTIF_TROP_LONG);
    }

    @Test
    @Tag("FR-003-09")
    void un_virement_execute_debite_credite_et_inscrit_deux_operations_avec_leur_solde_apres() {
        Virement.Execution execution = Virement.executer(soumission(12, COURANT, EPARGNE, "100.00", "loyer"), COURANT, EPARGNE, SEUILS);

        assertThat(execution.virement()).isEqualTo(new Virement(12, 7, new CompteId(1), new CompteId(2), Montant.lire("100.00"), Motif.lire("loyer"),
                EtatVirement.EXECUTE, StatutAffichage.SUCCESS, MAINTENANT, null, null));
        assertThat(execution.debit()).isEqualTo(new Operation(1, TypeOperation.DEBIT, Montant.lire("100.00"),
                Montant.lire("2030.00"), "Virement vers FR76-0001-0002 - loyer", MAINTENANT, 12));
        assertThat(execution.credit()).isEqualTo(new Operation(2, TypeOperation.CREDIT, Montant.lire("100.00"),
                Montant.lire("15300.00"), "Virement depuis FR76-0001-0001 - loyer", MAINTENANT, 12));
    }

    @Test
    @Tag("FR-003-09")
    void sans_motif_le_libelle_n_a_pas_de_separateur() {
        for (String motif : new String[] {null, "", "  "}) {
            Virement.Execution execution = Virement.executer(soumission(12, COURANT, EPARGNE, "100.00", motif), COURANT, EPARGNE, SEUILS);

            assertThat(execution.virement().motif()).isEqualTo(Motif.AUCUN);
            assertThat(execution.debit().description()).isEqualTo("Virement vers FR76-0001-0002");
            assertThat(execution.credit().description()).isEqualTo("Virement depuis FR76-0001-0001");
        }
    }

    @Test
    @Tag("FR-003-10")
    @Tag("FR-003-29")
    @Tag("BUG-009")
    void a_partir_du_seuil_de_mise_en_attente_le_virement_est_pending_et_les_operations_sont_prefixees() {
        Compte riche = new Compte(1, 7, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("20000.00"));

        Virement.Execution pending = Virement.executer(soumission(12, riche, EPARGNE, "5000.00", "loyer"), riche, EPARGNE, SEUILS);
        Virement.Execution succes = Virement.executer(soumission(13, riche, EPARGNE, "4999.99", "loyer"), riche, EPARGNE, SEUILS);

        assertThat(pending.virement().statut()).isEqualTo(StatutAffichage.PENDING);
        assertThat(pending.debit().description()).isEqualTo("[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer");
        assertThat(pending.credit().description()).isEqualTo("[TRANSACTION IMPORTANTE] Virement depuis FR76-0001-0001 - loyer");
        assertThat(succes.virement().statut()).isEqualTo(StatutAffichage.SUCCESS);
        assertThat(succes.debit().description()).isEqualTo("Virement vers FR76-0001-0002 - loyer");
    }

    @Test
    @Tag("FR-003-06")
    @Tag("BUG-008")
    @Tag("BUG-011")
    void un_solde_insuffisant_refuse_le_virement_sans_nom_d_erreur_interne() {
        assertThatThrownBy(() -> Virement.executer(soumission(12, COURANT, EPARGNE, "9999.00", null), COURANT, EPARGNE, SEUILS))
                .isInstanceOfSatisfying(ErreurVirement.class, e -> assertThat(e.motif()).isEqualTo(Cause.SOLDE_INSUFFISANT))
                .hasMessage("Solde insuffisant sur le compte source");
        // Solde exactement égal au montant : accepté.
        assertThat(Virement.executer(soumission(12, COURANT, EPARGNE, "2130.00", null), COURANT, EPARGNE, SEUILS)
                .debit().soldeApres().texte()).isEqualTo("0.00");
    }

    @Test
    @Tag("FR-003-05")
    void un_compte_d_un_autre_client_est_refuse() {
        Compte autre = new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("500.00"));

        assertThat(org.assertj.core.api.Assertions.catchThrowableOfType(ErreurVirement.class,
                () -> Virement.executer(soumission(12, autre, EPARGNE, "1.00", null), autre, EPARGNE, SEUILS))
                .motif()).isEqualTo(Cause.COMPTE_ETRANGER);
        assertThat(org.assertj.core.api.Assertions.catchThrowableOfType(ErreurVirement.class,
                () -> Virement.executer(soumission(12, COURANT, autre, "1.00", null), COURANT, autre, SEUILS))
                .motif()).isEqualTo(Cause.COMPTE_ETRANGER);
    }

    @Test
    @Tag("FR-021-06")
    @Tag("FR-021-05")
    void une_echeance_programmee_a_des_libelles_programmes_sans_mention_d_attente() {
        Soumission seuil = soumission(12, COURANT, EPARGNE, "2000", "Epargne");
        Virement.Execution avecMotif = Virement.executerEcheance(seuil, COURANT, EPARGNE, "Livret", SEUILS);
        Virement.Execution sansMotif = Virement.executerEcheance(soumission(13, COURANT, EPARGNE, "100", null),
                COURANT, EPARGNE, "Livret", SEUILS);

        assertThat(avecMotif.virement().etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(avecMotif.debit().description()).isEqualTo("Virement programme vers Livret - Epargne");
        assertThat(avecMotif.credit().description()).isEqualTo("Virement programme de FR76-0001-0001 - Epargne");
        assertThat(avecMotif.debit().soldeApres().texte()).isEqualTo(
                COURANT.solde().soustraire(Montant.lire("2000")).texte());
        assertThat(sansMotif.debit().description()).isEqualTo("Virement programme vers Livret");
        assertThat(sansMotif.credit().description()).isEqualTo("Virement programme de FR76-0001-0001");
    }

    @Test
    @Tag("FR-021-06")
    void une_echeance_programmee_accepte_un_destinataire_d_un_autre_client_mais_controle_la_source_et_le_solde() {
        Compte autre = new Compte(3, 8, "FR76-0002-0001", TypeCompte.COURANT, Montant.lire("500.00"));

        assertThat(Virement.executerEcheance(soumission(12, COURANT, autre, "100", null), COURANT, autre, "Ami",
                SEUILS).virement().etat()).isEqualTo(EtatVirement.EXECUTE);
        assertThat(org.assertj.core.api.Assertions.catchThrowableOfType(ErreurVirement.class,
                () -> Virement.executerEcheance(soumission(12, autre, COURANT, "100", null), autre, COURANT, "Moi",
                        SEUILS)).motif()).isEqualTo(Cause.COMPTE_ETRANGER);
        assertThat(org.assertj.core.api.Assertions.catchThrowableOfType(ErreurVirement.class,
                () -> Virement.executerEcheance(soumission(12, COURANT, EPARGNE, "9999", null), COURANT, EPARGNE,
                        "Livret", SEUILS)).motif()).isEqualTo(Cause.SOLDE_INSUFFISANT);
    }

    @Test
    @Tag("FR-021-05")
    void un_motif_blanc_n_est_pas_repris_dans_les_libelles_programmes() {
        assertThat(TextesVirement.libelleDebitProgramme("Livret", " ")).isEqualTo("Virement programme vers Livret");
        assertThat(TextesVirement.libelleCreditProgramme("FR76-0001-0001", null))
                .isEqualTo("Virement programme de FR76-0001-0001");
    }
}
