package fr.nextbanking.identiteclients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-015-01")
@Tag("FR-015-02")
@Tag("FR-030-07")
@Tag("BUG-093")
class ConsulterPortefeuilleCasUsageTest {

    private static final ClientDuPortefeuille MARIE =
            new ClientDuPortefeuille(2L, "Marie Lebrun", "user2", "marie.lebrun@example.com", null, false);
    private static final ClientDuPortefeuille JEAN =
            new ClientDuPortefeuille(1L, "Jean Dupont", "user1", "jean.dupont@example.com", "+33601020304", true);
    private static final ClientDuPortefeuille JEAN_BIS =
            new ClientDuPortefeuille(5L, "Jean Dupont", "user5", "jean.bis@example.com", null, true);

    private final List<Long> lus = new ArrayList<>();
    private final ConsulterPortefeuilleCasUsage casUsage = new ConsulterPortefeuilleCasUsage(conseillerId -> {
        lus.add(conseillerId);
        return conseillerId == 11L ? List.of(JEAN_BIS, MARIE, JEAN) : List.of();
    });

    @Test
    void le_conseiller_recoit_ses_clients_actifs_et_inactifs_tries_par_nom_puis_identifiant() {
        assertThat(casUsage.consulter(11L, Set.of(Role.CONSEILLER))).containsExactly(JEAN, JEAN_BIS, MARIE);
        assertThat(lus).containsExactly(11L);
    }

    @Test
    void un_admin_sans_client_affecte_recoit_une_liste_vide() {
        assertThat(casUsage.consulter(10L, Set.of(Role.ADMIN))).isEmpty();
        assertThat(lus).containsExactly(10L);
    }

    @Test
    void un_client_est_refuse_sans_lecture() {
        assertThatThrownBy(() -> casUsage.consulter(1L, Set.of(Role.CLIENT))).isInstanceOf(AccesRefuse.class);
        assertThatThrownBy(() -> casUsage.consulter(1L, Set.of())).isInstanceOf(AccesRefuse.class);
        assertThat(lus).isEmpty();
    }
}
