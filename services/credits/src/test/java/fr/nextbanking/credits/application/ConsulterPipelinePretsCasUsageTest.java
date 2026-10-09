package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.domain.IndicateursPrets;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretAvecClient;
import fr.nextbanking.credits.domain.Role;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterPipelinePretsCasUsageTest {

    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);

    private final PretRepository prets = mock(PretRepository.class);
    private final ProjectionClients clients = mock(ProjectionClients.class);
    private final ConsulterPipelinePretsCasUsage casUsage = new ConsulterPipelinePretsCasUsage(prets, clients);

    private static Pret pret(long id, long clientId, StatutPret statut) {
        return new Pret(id, clientId, TypePret.AUTO, new BigDecimal("9000.00"), new BigDecimal("3.9000"), 6,
                new BigDecimal("1517.11"), new BigDecimal("9102.66"), new BigDecimal("102.66"), "Voiture", statut,
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 4, 1));
    }

    @Test
    @Tag("FR-016-03")
    void le_pipeline_porte_le_nom_complet_de_la_projection_et_se_replie_sur_l_identifiant() {
        when(prets.enAttente()).thenReturn(List.of(pret(1L, 5L, StatutPret.EN_ATTENTE),
                pret(2L, 6L, StatutPret.EN_ATTENTE), pret(3L, 5L, StatutPret.EN_ATTENTE)));
        when(clients.nomsComplets(List.of(5L, 6L))).thenReturn(Map.of(5L, "Jean Dupont"));

        List<PretAvecClient> lignes = casUsage.enAttente(ADMIN);

        assertThat(lignes).extracting(PretAvecClient::nomClient).containsExactly("Jean Dupont", "6", "Jean Dupont");
    }

    @Test
    @Tag("FR-016-03")
    void un_pipeline_vide_ne_lit_pas_la_projection() {
        when(prets.enAttente()).thenReturn(List.of());

        assertThat(casUsage.enAttente(ADMIN)).isEmpty();

        verify(clients, never()).nomsComplets(any());
    }

    @Test
    @Tag("FR-016-12")
    void l_historique_rend_tous_les_prets_dans_l_ordre_du_depot() {
        when(prets.tous()).thenReturn(List.of(pret(2L, 5L, StatutPret.REFUSE), pret(1L, 5L, StatutPret.EN_COURS)));
        when(clients.nomsComplets(List.of(5L))).thenReturn(Map.of(5L, "Jean Dupont"));

        assertThat(casUsage.historique(ADMIN)).extracting(ligne -> ligne.pret().id()).containsExactly(2L, 1L);
    }

    @Test
    @Tag("FR-016-12")
    void un_historique_vide_est_une_liste_vide() {
        when(prets.tous()).thenReturn(List.of());

        assertThat(casUsage.historique(ADMIN)).isEmpty();
    }

    @Test
    @Tag("FR-016-02")
    void l_indicateur_compte_les_prets_en_attente_et_cumule_leurs_montants_sans_lire_la_projection() {
        Pret petit = new Pret(1L, 5L, TypePret.AUTO, new BigDecimal("5000.00"), new BigDecimal("3.9000"), 6,
                new BigDecimal("1517.11"), new BigDecimal("9102.66"), new BigDecimal("102.66"), "Voiture",
                StatutPret.EN_ATTENTE, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 4, 1));
        Pret grand = pret(2L, 6L, StatutPret.EN_ATTENTE);
        when(prets.enAttente()).thenReturn(List.of(petit, grand, grand));

        IndicateursPrets indicateurs = casUsage.indicateurs(ADMIN);

        assertThat(indicateurs.nombre()).isEqualTo(3L);
        assertThat(indicateurs.montant().toPlainString()).isEqualTo("23000.00");
        verify(clients, never()).nomsComplets(any());
    }

    @Test
    @Tag("FR-016-02")
    void sans_pret_en_attente_l_indicateur_vaut_zero() {
        when(prets.enAttente()).thenReturn(List.of());

        IndicateursPrets indicateurs = casUsage.indicateurs(ADMIN);

        assertThat(indicateurs.nombre()).isZero();
        assertThat(indicateurs.montant().toPlainString()).isEqualTo("0.00");
    }

    @Test
    @Tag("FR-030-07")
    @Tag("FR-016-01")
    @Tag("FR-016-02")
    void l_indicateur_est_refuse_a_un_role_autre_qu_admin_sans_lecture() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER))) {
            assertThatThrownBy(() -> casUsage.indicateurs(roles)).isInstanceOf(AccesAdministrationRefuse.class);
        }

        verify(prets, never()).enAttente();
    }

    @Test
    @Tag("FR-030-07")
    void un_role_autre_qu_admin_est_refuse() {
        for (Set<Role> roles : List.of(Set.of(Role.CLIENT), Set.of(Role.CONSEILLER))) {
            assertThatThrownBy(() -> casUsage.enAttente(roles)).isInstanceOf(AccesAdministrationRefuse.class);
            assertThatThrownBy(() -> casUsage.historique(roles)).isInstanceOf(AccesAdministrationRefuse.class);
        }

        verify(prets, never()).enAttente();
        verify(prets, never()).tous();
    }
}
