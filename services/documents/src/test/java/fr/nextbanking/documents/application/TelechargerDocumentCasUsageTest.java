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
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.TypeCompte;
import fr.nextbanking.documents.domain.TypeDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    private static final DocumentPdf PDF = new DocumentPdf("rib_1.pdf", new byte[] {1, 2, 3});

    private final ProjectionComptes comptes = mock(ProjectionComptes.class);
    private final ProjectionClients clients = mock(ProjectionClients.class);
    private final GenerateurPdf generateur = mock(GenerateurPdf.class);
    private final Documents documents = mock(Documents.class);
    private final Horloge horloge = mock(Horloge.class);
    private final TelechargerDocumentCasUsage casUsage =
            new TelechargerDocumentCasUsage(comptes, clients, generateur, documents, horloge);

    @BeforeEach
    void preparer() {
        when(comptes.trouver(1L)).thenReturn(Optional.of(COMPTE_1));
        when(comptes.trouver(7L)).thenReturn(Optional.of(COMPTE_D_AUTRUI));
        when(clients.nomComplet(1L)).thenReturn(Optional.of("Jean Dupont"));
        when(horloge.instant()).thenReturn(INSTANT);
        when(horloge.aujourdhui()).thenReturn(AUJOURDHUI);
        when(generateur.generer(any(), any())).thenReturn(PDF);
    }

    private DemandeDocumentInvalide refus(String type, String compteId) {
        return (DemandeDocumentInvalide) org.assertj.core.api.Assertions.catchThrowable(
                () -> casUsage.telecharger(new Commande(1L, type, compteId)));
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
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "999999")))
                .isInstanceOf(CompteIntrouvable.class);

        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-08")
    @Tag("FR-008-04")
    @Tag("BUG-058")
    @Tag("BUG-027")
    void le_compte_d_un_autre_client_est_refuse_sans_rien_generer_ni_compter_quel_que_soit_le_type() {
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "7")))
                .isInstanceOf(AccesCompteRefuse.class);
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "attestation", "7")))
                .isInstanceOf(AccesCompteRefuse.class);

        rienGenereNiCompte();
        verify(clients, never()).nomComplet(2L);
    }

    @Test
    @Tag("FR-017-08")
    void le_controle_d_appartenance_ne_regarde_jamais_le_role() {
        // Le cas d'usage ne reçoit que le client du jeton : un conseiller ou un administrateur est un client comme un autre.
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(11L, "RIB", "1")))
                .isInstanceOf(AccesCompteRefuse.class);
        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-09")
    void un_titulaire_absent_de_la_projection_des_clients_est_introuvable() {
        when(clients.nomComplet(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1")))
                .isInstanceOf(ClientIntrouvable.class);

        rienGenereNiCompte();
    }

    @Test
    @Tag("FR-017-05")
    @Tag("FR-008-08")
    @Tag("FR-008-12")
    void un_rib_est_genere_avec_les_donnees_des_projections_et_de_l_horloge_puis_compte() {
        DocumentPdf rendu = casUsage.telecharger(new Commande(1L, "rib", " 1 "));

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
        casUsage.telecharger(new Commande(1L, "ATTESTATION", "1"));

        verify(generateur).generer(TypeDocument.ATTESTATION, new DonneesDocument(1L, "Jean Dupont", 1L,
                "FR76-0001-0001", "FR76-0001-0001", new BigDecimal("2130.00"), AUJOURDHUI));
        verify(documents).compterTelechargement(1L, TypeDocument.ATTESTATION, "compteId=1", PDF, INSTANT);
    }

    @Test
    void l_ifu_est_un_document_du_client_et_n_exige_aucun_compte() {
        casUsage.telecharger(new Commande(1L, "IFU", null));

        verifyNoInteractions(comptes);
        verify(generateur).generer(TypeDocument.IFU,
                new DonneesDocument(1L, "Jean Dupont", null, null, null, null, AUJOURDHUI));
        verify(documents).compterTelechargement(1L, TypeDocument.IFU, "", PDF, INSTANT);
    }

    @Test
    void le_releve_passe_la_validation_du_type_et_exige_un_compte_du_titulaire() {
        casUsage.telecharger(new Commande(1L, "RELEVE", "1"));

        verify(generateur).generer(any(), any());
        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RELEVE", "7")))
                .isInstanceOf(AccesCompteRefuse.class);
    }

    @Test
    @Tag("FR-017-10")
    @Tag("BUG-059")
    void une_panne_de_generation_se_propage_sans_rien_compter() {
        when(generateur.generer(any(), any())).thenThrow(new IllegalStateException("panne"));

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1")))
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

        assertThatThrownBy(() -> casUsage.telecharger(new Commande(1L, "RIB", "1")))
                .isInstanceOf(IllegalStateException.class);
    }
}
