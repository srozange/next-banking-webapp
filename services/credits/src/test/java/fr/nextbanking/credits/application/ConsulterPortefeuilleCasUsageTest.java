package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.domain.ClientEtSesPrets;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.Role;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Portefeuille du conseiller côté crédits : clients de la projection affectés à l'acteur, triés par identifiant, avec
 * leurs prêts dans l'ordre du dépôt, lus en une seule fois ; codes rendus tels quels, type nul compris.
 */
@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-015-09")
@Tag("FR-030-07")
@Tag("BUG-093")
class ConsulterPortefeuilleCasUsageTest {

    private final ProjectionClients projection = mock(ProjectionClients.class);
    private final PretRepository prets = mock(PretRepository.class);
    private final ConsulterPortefeuilleCasUsage cas = new ConsulterPortefeuilleCasUsage(projection, prets);

    private static Pret pret(long id, long clientId, TypePret type, StatutPret statut, LocalDate debut) {
        return new Pret(id, clientId, type, new BigDecimal("9000.00"), new BigDecimal("3.9000"), 6,
                new BigDecimal("1517.11"), new BigDecimal("9102.66"), new BigDecimal("102.66"), "Voiture", statut,
                debut, debut.plusMonths(6));
    }

    @Test
    void le_conseiller_recoit_ses_clients_tries_avec_leurs_prets_dans_l_ordre_du_depot() {
        Pret recent = pret(2, 1, TypePret.AUTO, StatutPret.EN_COURS, LocalDate.of(2026, 8, 1));
        Pret sansType = pret(5, 1, null, StatutPret.EN_ATTENTE, LocalDate.of(2026, 7, 1));
        Pret ancien = pret(1, 1, TypePret.ETUDIANT, StatutPret.REFUSE, LocalDate.of(2026, 6, 1));
        when(projection.clientsDuConseiller(11L)).thenReturn(List.of(2L, 1L));
        when(prets.desClients(List.of(1L, 2L))).thenReturn(List.of(recent, sansType, ancien));

        List<ClientEtSesPrets> portefeuille = cas.consulter(11L, Set.of(Role.CONSEILLER));

        assertThat(portefeuille).containsExactly(new ClientEtSesPrets(1, List.of(recent, sansType, ancien)),
                new ClientEtSesPrets(2, List.of()));
        assertThat(portefeuille.getFirst().prets()).extracting(Pret::type)
                .containsExactly(TypePret.AUTO, null, TypePret.ETUDIANT);
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide_sans_lire_de_pret() {
        when(projection.clientsDuConseiller(10L)).thenReturn(List.of());

        assertThat(cas.consulter(10L, Set.of(Role.ADMIN))).isEmpty();
        verify(prets, never()).desClients(any());
    }

    @Test
    void un_client_est_refuse_sans_lecture() {
        assertThatThrownBy(() -> cas.consulter(1L, Set.of(Role.CLIENT)))
                .isInstanceOf(AccesAdministrationRefuse.class);
        verify(projection, never()).clientsDuConseiller(anyLong());
    }
}
