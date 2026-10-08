package fr.nextbanking.credits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.credits.application.port.in.RecevoirClientModifie.Issue;
import fr.nextbanking.credits.application.port.out.EvenementsTraites;
import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.ClientProjete;
import fr.nextbanking.credits.domain.Role;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-016-03")
class RecevoirClientModifieCasUsageTest {

    private static final UUID ID = UUID.fromString("3c9d7e1b-2a4f-4b6c-9d8e-1f0a2b3c4d5e");
    private static final ClientProjete CLIENT = new ClientProjete(1L, "user1", "Jean Dupont", Role.CLIENT, false, 11L,
            LocalDate.of(2026, 10, 6), false, 2L);

    private final ProjectionClients projection = mock(ProjectionClients.class);
    private final EvenementsTraites traites = mock(EvenementsTraites.class);
    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            return travail.get();
        }

        @Override
        public <T> T executerIndependamment(Supplier<T> travail) {
            throw new UnsupportedOperationException();
        }
    };
    private final RecevoirClientModifieCasUsage casUsage =
            new RecevoirClientModifieCasUsage(projection, traites, transaction);

    @Test
    void une_version_superieure_est_enregistree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiPlusRecent(CLIENT)).thenReturn(true);

        assertThat(casUsage.recevoir(ID, CLIENT)).isEqualTo(Issue.ENREGISTRE);
    }

    @Test
    void une_version_egale_ou_inferieure_est_ignoree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiPlusRecent(CLIENT)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, CLIENT)).isEqualTo(Issue.IGNORE_ANCIEN);
    }

    @Test
    void un_evenement_deja_traite_n_a_aucun_effet() {
        when(traites.marquerTraite(ID)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, CLIENT)).isEqualTo(Issue.DEJA_TRAITE);

        verify(projection, never()).enregistrerSiPlusRecent(any());
    }

    @Test
    void refuse_un_evenement_sans_identifiant() {
        assertThatThrownBy(() -> casUsage.recevoir(null, CLIENT)).isInstanceOf(IllegalArgumentException.class);
    }
}
