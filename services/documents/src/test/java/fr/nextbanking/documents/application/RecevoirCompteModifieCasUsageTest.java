package fr.nextbanking.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.documents.application.port.in.RecevoirCompteModifie.Issue;
import fr.nextbanking.documents.application.port.out.EvenementsTraites;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.application.port.out.Transaction;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.TypeCompte;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RecevoirCompteModifieCasUsageTest {

    private static final UUID ID = UUID.fromString("7b8c3d4e-5f60-4b7c-8d9e-0f1a2b3c4d5e");
    private static final CompteProjete COMPTE = new CompteProjete(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT,
            "FR76-0001-0001", "DEMOFR21XXX", new BigDecimal("2030.00"), 2L);

    private final ProjectionComptes projection = mock(ProjectionComptes.class);
    private final EvenementsTraites traites = mock(EvenementsTraites.class);
    private final Transaction transaction = new Transaction() {
        @Override
        public <T> T executer(Supplier<T> travail) {
            return travail.get();
        }
    };
    private final RecevoirCompteModifieCasUsage casUsage =
            new RecevoirCompteModifieCasUsage(projection, traites, transaction);

    @Test
    void une_version_superieure_est_enregistree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiPlusRecent(COMPTE)).thenReturn(true);

        assertThat(casUsage.recevoir(ID, COMPTE)).isEqualTo(Issue.ENREGISTRE);
    }

    @Test
    void une_version_egale_ou_inferieure_est_ignoree() {
        when(traites.marquerTraite(ID)).thenReturn(true);
        when(projection.enregistrerSiPlusRecent(COMPTE)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, COMPTE)).isEqualTo(Issue.IGNORE_ANCIEN);
    }

    @Test
    void un_evenement_deja_traite_n_a_aucun_effet() {
        when(traites.marquerTraite(ID)).thenReturn(false);

        assertThat(casUsage.recevoir(ID, COMPTE)).isEqualTo(Issue.DEJA_TRAITE);

        verify(projection, never()).enregistrerSiPlusRecent(any());
    }

    @Test
    void refuse_un_evenement_sans_identifiant() {
        assertThatThrownBy(() -> casUsage.recevoir(null, COMPTE)).isInstanceOf(IllegalArgumentException.class);
    }
}
