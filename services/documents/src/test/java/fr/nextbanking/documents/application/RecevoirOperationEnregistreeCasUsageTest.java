package fr.nextbanking.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.documents.application.port.in.RecevoirOperationEnregistree.Issue;
import fr.nextbanking.documents.application.port.out.EvenementsTraites;
import fr.nextbanking.documents.application.port.out.ProjectionOperations;
import fr.nextbanking.documents.application.port.out.Transaction;
import fr.nextbanking.documents.domain.OperationProjetee;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-017-04")
class RecevoirOperationEnregistreeCasUsageTest {

    private static final UUID ID = UUID.fromString("9c1d2e3f-4a5b-4c6d-8e7f-0a1b2c3d4e5f");
    private static final OperationProjetee OPERATION = new OperationProjetee(42L, 1L,
            Instant.parse("2026-11-17T09:00:00Z"), "Virement vers FR76-0001-0002", TypeOperation.DEBIT,
            new BigDecimal("100.00"), new BigDecimal("2030.00"), 2L);

    private final ProjectionOperations projection = mock(ProjectionOperations.class);
    private final EvenementsTraites traites = mock(EvenementsTraites.class);
    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            return travail.get();
        }
    };
    private final RecevoirOperationEnregistreeCasUsage casUsage =
            new RecevoirOperationEnregistreeCasUsage(projection, traites, transaction);

    @Test
    void une_nouvelle_operation_est_enregistree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiAbsente(OPERATION)).thenReturn(true);

        assertThat(casUsage.recevoir(ID, OPERATION)).isEqualTo(Issue.ENREGISTRE);
    }

    @Test
    void une_operation_deja_presente_sous_un_autre_evenement_est_ignoree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiAbsente(OPERATION)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, OPERATION)).isEqualTo(Issue.DEJA_PRESENTE);
    }

    @Test
    void un_evenement_deja_traite_n_a_aucun_effet() {
        when(traites.marquerTraite(ID)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, OPERATION)).isEqualTo(Issue.DEJA_TRAITE);

        verify(projection, never()).enregistrerSiAbsente(any());
    }

    @Test
    void refuse_un_evenement_sans_identifiant() {
        assertThatThrownBy(() -> casUsage.recevoir(null, OPERATION)).isInstanceOf(IllegalArgumentException.class);
    }
}
