package fr.nextbanking.credits.adapter.in.evenements;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.nextbanking.credits.application.port.in.DecisionPret;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture de l'enveloppe brute (chaîne JSON sans en-tête de type) et confiance au cas d'usage.
 */
@Tag("FR-024-06")
class RecepteurPretDemandeTest {

    private static final UUID ID = UUID.fromString("3f2c1a9e-8b7d-4c6e-9f10-2a3b4c5d6e7f");

    private final DecisionPret decisionPret = mock(DecisionPret.class);
    private final RecepteurPretDemande recepteur = new RecepteurPretDemande(decisionPret,
            JsonMapper.builder().build());

    private static String message(String type, int version) {
        return """
                {"idEvenement":"%s","type":"%s","version":%d,"horodatage":"2026-10-06T08:30:00Z",\
                "producteur":"credits","donnees":{"pretId":42}}""".formatted(ID, type, version);
    }

    @Test
    void une_demande_est_confiee_au_cas_d_usage() {
        when(decisionPret.decider(ID, 42L)).thenReturn(true);

        recepteur.recevoir(message("pret-demande", 1));

        verify(decisionPret).decider(ID, 42L);
    }

    @Test
    void une_redistribution_est_journalisee_sans_erreur() {
        when(decisionPret.decider(ID, 42L)).thenReturn(false);

        recepteur.recevoir(message("pret-demande", 1));

        verify(decisionPret).decider(ID, 42L);
    }

    @Test
    void un_type_ou_une_version_inattendus_sont_ignores() {
        recepteur.recevoir(message("autre", 1));
        recepteur.recevoir(message("pret-demande", 2));

        verify(decisionPret, never()).decider(any(), anyLong());
    }
}
