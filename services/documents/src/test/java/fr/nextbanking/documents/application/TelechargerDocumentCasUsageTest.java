package fr.nextbanking.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.nextbanking.documents.application.DemandeDocumentInvalide.Motif;
import fr.nextbanking.documents.application.port.in.TelechargerDocument.Commande;
import fr.nextbanking.documents.application.port.out.Documents;
import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionClients;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.application.port.out.ProjectionOperations;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.Ifu;
import fr.nextbanking.documents.domain.LigneReleve;
import fr.nextbanking.documents.domain.OperationProjetee;
import fr.nextbanking.documents.domain.Releve;
import fr.nextbanking.documents.domain.TypeCompte;
import fr.nextbanking.documents.domain.TypeDocument;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cas d'usage du téléchargement : ordre des refus (type, compte, existence, appartenance), génération avec les données
 * des projections et de l'Horloge, compteur incrémenté après une génération réussie seulement.
 */
class TelechargerDocumentCasUsageTest {

    private static final Instant INSTANT = Instant.parse("2026-10-09T22:30:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 10);
    private static final CompteProjete COMPTE_1 = new CompteProjete(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT,
            "FR76-0001-0001", "DEMOFR21XXX", new BigDecimal("2130.00"), 1L);
    private static final CompteProjete COMPTE_D_AUTRUI = new CompteProjete(7L, 2L, "FR76-0002-0001",
            TypeCompte.COURANT, "FR76-0002-0001", "DEMOFR21XXX", new BigDecimal("500.00"), 1L);
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final DocumentPdf PDF = new DocumentPdf("rib_1.pdf", new byte[] {1, 2, 3});

    private final ProjectionComptes comptes = mock(ProjectionComptes.class);
    private final ProjectionClients clients = mock(ProjectionClients.class);
    private final ProjectionOperations operations = mock(ProjectionOperations.class);
    private final GenerateurPdf generateur = mock(GenerateurPdf.class);
    private final Documents documents = mock(Documents.class);
    private final Horloge horloge = mock(Horloge.class);
    private final TelechargerDocumentCasUsage casUsage =
            new TelechargerDocumentCasUsage(comptes, clients, operations, generateur, documents, horloge);

    @BeforeEach
    void preparer() {
        when(comptes.trouver(1L)).thenReturn(Optional.of(COMPTE_1));
        when(comptes.trouver(7L)).thenReturn(Optional.of(COMPTE_D_AUTRUI));
        when(clients.nomComplet(1L)).thenReturn(Optional.of("Jean Dupont"));
        when(horloge.instant()).thenReturn(INSTANT);
        when(horloge.aujourdhui()).thenReturn(AUJOURDHUI);
        when(horloge.fuseau()).thenReturn(PARIS);
        when(generateur.generer(any(), any())).thenReturn(PDF);
        when(operations.totalInteretsCredites(org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    private DemandeDocumentInvalide refus(String type, String compteId) {
        return (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(
                () -> casUsage.telecharger(new Commande(1L, type, compteId, null, null)));
    }

    private void rienGenereNiCompte() {
        verifyNoInteractions(generateur, documents);
    }

    @Test
    @Tag("FR-017-02")
    void un_type_absent_est_refuse_avant_tout_autre_controle() {
        assertThat(refus(null, null).motif()).isEqualTo(Motif.TYPE_MANQUANT);
        assertThat(refus(null, "abc").motif()).isEqualTo(Motif.TYPE_MANQUANT);

        rienGenereNiCompte();
        verifyNoInteractions(comptes);
    }

    @Test
    @Tag("FR-017-02")
    @Tag("FR-008-11")
    void un_type_inconnu_est_refuse_et_rendu_tel_que_recu_avant_le_controle_du_compte() {
        DemandeDocumentInvalide refus = refus("FACTURE", "abc");

        assertThat(refus.motif()).isEqualTo(Motif.TYPE_INCONNU);
        assertThat(refus.valeur()).isEqualTo("FACTURE");
        assertThat(refus("fActure", "1").valeur()).isEqualTo("fActure");
        assertThat(refus("", "1").valeur()).isEmpty();
        rienGenereNiCompte();
        verifyNoInteractions(comptes);
    }

    @Test
    @Tag("FR-017-03")
    void un_compte_absent_vide_ou_non_numerique_est_refuse() {
        for (String compteId : new String[] {null, "", "   ", "abc", "1.5", "99999999999999999999"}) {
            DemandeDocumentInvalide refus = refus("RIB", compteId);

            assertThat(refus.motif()).as(String.valueOf(compteId)).isEqualTo(Motif.COMPTE_ID_MANQUANT);
            assertThat(refus.valeur()).isNull();
        }
        rienGenereNiCompte();
        verifyNoInteractions(comptes);
    }

    @Test
    @Tag("FR-017-09")
    void un_compte_absent_de_la_projection_est_introuvable() {
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "999999", null, null)))
                .isInstanceOf(CompteIntrouvable.class);

        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-08")
    @Tag("FR-008-04")
    @Tag("BUG-058")
    @Tag("BUG-027")
    void le_compte_d_un_autre_client_est_refuse_sans_rien_generer_ni_compter_quel_que_soit_le_type() {
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "7", null, null)))
                .isInstanceOf(AccesCompteRefuse.class);
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "attestation", "7", null, null)))
                .isInstanceOf(AccesCompteRefuse.class);

        rienGenereNiCompte();
        verify(clients, never()).nomComplet(2L);
    }

    @Test
    @Tag("FR-017-08")
    void le_controle_d_appartenance_ne_regarde_jamais_le_role() {
        // Le cas d'usage ne reçoit que le client du jeton : un conseiller ou un administrateur est un client comme un autre.
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(11L, "RIB", "1", null, null)))
                .isInstanceOf(AccesCompteRefuse.class);
        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-09")
    void un_titulaire_absent_de_la_projection_des_clients_est_introuvable() {
        when(clients.nomComplet(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1", null, null)))
                .isInstanceOf(ClientIntrouvable.class);

        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-05")
    @Tag("FR-008-08")
    @Tag("FR-008-12")
    void un_rib_est_genere_avec_les_donnees_des_projections_et_de_l_horloge_puis_compte() {
        DocumentPdf rendu = casUsage.telecharger(new Commande(1L, "rib", " 1 ", null, null));

        assertThat(rendu).isSameAs(PDF);
        verify(generateur).generer(TypeDocument.RIB, new DonneesDocument(1L, "Jean Dupont", 1L, "FR76-0001-0001",
                "FR76-0001-0001", new BigDecimal("2130.00"), AUJOURDHUI));
        verify(documents).compterTelechargement(1L, TypeDocument.RIB, "compteId=1", PDF, INSTANT);
    }

    @Test
    @Tag("FR-017-06")
    @Tag("FR-008-09")
    @Tag("FR-008-12")
    void une_attestation_est_generee_puis_comptee_sous_ses_propres_parametres() {
        casUsage.telecharger(new Commande(1L, "ATTESTATION", "1", null, null));

        verify(generateur).generer(TypeDocument.ATTESTATION, new DonneesDocument(1L, "Jean Dupont", 1L,
                "FR76-0001-0001", "FR76-0001-0001", new BigDecimal("2130.00"), AUJOURDHUI));
        verify(documents).compterTelechargement(1L, TypeDocument.ATTESTATION, "compteId=1", PDF, INSTANT);
    }

    private static OperationProjetee operation(long id, String instant, String libelle, TypeOperation type,
            String montant, String soldeApres) {
        return new OperationProjetee(id, 1L, Instant.parse(instant), libelle, type, new BigDecimal(montant),
                new BigDecimal(soldeApres), 1L);
    }

    private DemandeDocumentInvalide refusPeriode(String type, String compteId, String annee, String mois) {
        return (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(
                () -> casUsage.telecharger(new Commande(1L, type, compteId, annee, mois)));
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    @Tag("FR-008-12")
    void un_releve_borne_le_mois_en_europe_paris_convertit_les_jours_et_compte_ses_parametres() {
        Instant debut = Instant.parse("2026-02-28T23:00:00Z");
        Instant fin = Instant.parse("2026-03-31T22:00:00Z");
        when(operations.duCompte(1L, debut, fin)).thenReturn(List.of(
                operation(5L, "2026-03-31T21:59:59Z", "Loyer", TypeOperation.DEBIT, "100.00", "2030.00"),
                operation(6L, "2026-03-01T09:00:00Z", "Salaire", TypeOperation.CREDIT, "50.00", "2180.00")));

        DocumentPdf rendu = casUsage.telecharger(new Commande(1L, "releve", "1", "2026", "03"));

        assertThat(rendu).isSameAs(PDF);
        verify(generateur).generer(TypeDocument.RELEVE, new DonneesDocument(1L, "Jean Dupont", 1L, "FR76-0001-0001",
                "FR76-0001-0001", new BigDecimal("2130.00"), AUJOURDHUI,
                new Releve(TypeCompte.COURANT, 2026, 3, List.of(
                        new LigneReleve(LocalDate.of(2026, 3, 31), "Loyer", TypeOperation.DEBIT,
                                new BigDecimal("100.00"), new BigDecimal("2030.00")),
                        new LigneReleve(LocalDate.of(2026, 3, 1), "Salaire", TypeOperation.CREDIT,
                                new BigDecimal("50.00"), new BigDecimal("2180.00")))),
                null));
        verify(documents).compterTelechargement(1L, TypeDocument.RELEVE, "compteId=1;annee=2026;mois=3", PDF,
                INSTANT);
    }

    @Test
    @Tag("FR-017-07")
    void le_releve_de_decembre_s_arrete_au_premier_janvier_a_minuit_heure_de_paris() {
        casUsage.telecharger(new Commande(1L, "RELEVE", "1", "2026", "12"));

        verify(operations).duCompte(1L, Instant.parse("2026-11-30T23:00:00Z"), Instant.parse("2026-12-31T23:00:00Z"));
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    void un_mois_sans_operation_donne_un_releve_sans_ligne() {
        casUsage.telecharger(new Commande(1L, "RELEVE", "1", "2026", "5"));

        verify(generateur).generer(TypeDocument.RELEVE, new DonneesDocument(1L, "Jean Dupont", 1L, "FR76-0001-0001",
                "FR76-0001-0001", new BigDecimal("2130.00"), AUJOURDHUI,
                new Releve(TypeCompte.COURANT, 2026, 5, List.of()), null));
    }

    @Test
    @Tag("FR-017-07")
    void le_releve_exige_un_compte_du_titulaire() {
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RELEVE", "7", "2026", "3")))
                .isInstanceOf(AccesCompteRefuse.class);
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RELEVE", "999999", "2026", "3")))
                .isInstanceOf(CompteIntrouvable.class);
        rienGenereNiCompte();
        verifyNoInteractions(operations);
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-017-11")
    void annee_et_mois_invalides_sont_refuses_avant_le_404_et_le_403_et_toute_generation() {
        for (String annee : new String[] {null, "", "  ", "abc", "1999", "2101", "20x6"}) {
            assertThat(refusPeriode("RELEVE", "999999", annee, "3").motif()).as("annee " + annee)
                    .isEqualTo(Motif.PERIODE_INVALIDE);
            assertThat(refusPeriode("IFU", null, annee, null).motif()).as("ifu " + annee)
                    .isEqualTo(Motif.PERIODE_INVALIDE);
        }
        for (String mois : new String[] {null, "", "abc", "0", "13", "-1"}) {
            assertThat(refusPeriode("RELEVE", "7", "2026", mois).motif()).as("mois " + mois)
                    .isEqualTo(Motif.PERIODE_INVALIDE);
        }
        rienGenereNiCompte();
        verifyNoInteractions(comptes, operations);
    }

    @Test
    @Tag("FR-017-07")
    void les_bornes_des_plages_de_l_annee_et_du_mois_sont_acceptees() {
        casUsage.telecharger(new Commande(1L, "RELEVE", "1", "2000", "1"));
        casUsage.telecharger(new Commande(1L, "RELEVE", "1", "2100", "12"));
        casUsage.telecharger(new Commande(1L, "IFU", null, "2100", null));

        verify(generateur, org.mockito.Mockito.times(3)).generer(any(), any());
    }

    @Test
    @Tag("FR-017-07")
    void le_compte_id_est_controle_avant_l_annee_et_le_mois() {
        assertThat(refusPeriode("RELEVE", "abc", null, null).motif()).isEqualTo(Motif.COMPTE_ID_MANQUANT);
        assertThat(refusPeriode("RELEVE", "1", "2026", "13").motif()).isEqualTo(Motif.PERIODE_INVALIDE);
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-008-10")
    @Tag("FR-008-12")
    @Tag("NFR-SEC-03")
    void l_ifu_est_celui_du_client_du_jeton_sur_l_annee_civile_en_europe_paris_avec_un_pfu_arrondi_au_demi_superieur() {
        when(operations.totalInteretsCredites(1L, Instant.parse("2024-12-31T23:00:00Z"),
                Instant.parse("2025-12-31T23:00:00Z"))).thenReturn(new BigDecimal("12.35"));

        casUsage.telecharger(new Commande(1L, "ifu", null, "2025", null));

        verifyNoInteractions(comptes);
        verify(generateur).generer(TypeDocument.IFU, new DonneesDocument(1L, "Jean Dupont", null, null, null, null,
                AUJOURDHUI, null, Ifu.de(2025, new BigDecimal("12.35"))));
        verify(documents).compterTelechargement(1L, TypeDocument.IFU, "annee=2025", PDF, INSTANT);
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-017-09")
    void un_client_du_jeton_absent_de_la_projection_n_a_pas_d_ifu() {
        when(clients.nomComplet(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "IFU", null, "2025", null)))
                .isInstanceOf(ClientIntrouvable.class);

        rienGenereNiCompte();
        verifyNoInteractions(operations);
    }

    @Test
    @Tag("FR-017-10")
    @Tag("BUG-059")
    void une_panne_de_generation_se_propage_sans_rien_compter() {
        when(generateur.generer(any(), any())).thenThrow(new IllegalStateException("panne"));

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1", null, null)))
                .isInstanceOf(IllegalStateException.class);

        verify(documents, never()).compterTelechargement(anyLong(), any(), anyString(), any(), any());
    }

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    @Test
    @Tag("FR-017-10")
    void une_panne_du_compteur_se_propage_et_le_document_n_est_pas_rendu() {
        org.mockito.Mockito.doThrow(new IllegalStateException("base")).when(documents)
                .compterTelechargement(anyLong(), any(), anyString(), any(), any());

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1", null, null)))
                .isInstanceOf(IllegalStateException.class);
    }
}
