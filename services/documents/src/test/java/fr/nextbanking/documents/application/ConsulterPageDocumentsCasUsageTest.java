package fr.nextbanking.documents.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.nextbanking.documents.application.port.in.ConsulterPageDocuments.Page;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.TypeCompte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ConsulterPageDocumentsCasUsageTest {

    private final ProjectionComptes comptes = mock(ProjectionComptes.class);
    private final Horloge horloge = mock(Horloge.class);
    private final ConsulterPageDocumentsCasUsage casUsage = new ConsulterPageDocumentsCasUsage(comptes, horloge);

    @Test
    @Tag("FR-008-03")
    @Tag("FR-008-05")
    void rend_les_comptes_du_client_et_aujourdhui_de_l_horloge() {
        CompteProjete compte = new CompteProjete(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT, "FR76-0001-0001",
                "DEMOFR21XXX", new BigDecimal("2130.00"), 1L);
        when(comptes.parClient(1L)).thenReturn(List.of(compte));
        when(horloge.aujourdhui()).thenReturn(LocalDate.of(2026, 11, 17));

        Page page = casUsage.consulter(1L);

        assertThat(page.aujourdhui()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(page.comptes()).containsExactly(compte);
    }

    @Test
    @Tag("FR-008-06")
    void un_client_sans_compte_obtient_une_liste_vide() {
        when(comptes.parClient(9L)).thenReturn(List.of());
        when(horloge.aujourdhui()).thenReturn(LocalDate.of(2026, 2, 5));

        assertThat(casUsage.consulter(9L).comptes()).isEmpty();
    }
}
