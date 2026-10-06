package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class NouveauPretTest {

    @Test
    @Tag("FR-007-07")
    void calcule_comme_la_simulation_et_place_les_dates_au_premier_du_mois() {
        DemandePret demande = DemandePret.lire("CONSOMMATION", "10000", "3.5", "36", "Voiture");

        NouveauPret nouveau = NouveauPret.creer(7L, demande, LocalDate.of(2026, 10, 6));

        assertThat(nouveau.clientId()).isEqualTo(7L);
        assertThat(nouveau.mensualite()).isEqualByComparingTo("293.02");
        assertThat(nouveau.coutTotal()).isEqualByComparingTo("10548.72");
        assertThat(nouveau.totalInterets()).isEqualByComparingTo("548.72");
        assertThat(nouveau.dateDebut()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(nouveau.dateFin()).isEqualTo(LocalDate.of(2029, 10, 1));
        assertThat(nouveau.objet()).isEqualTo("Voiture");
    }

    @Test
    @Tag("FR-007-07")
    void la_fin_est_le_debut_plus_la_duree_en_mois_et_le_pret_est_en_attente() {
        DemandePret demande = DemandePret.lire("AUTO", "5000", "0", "300", "x");

        NouveauPret nouveau = NouveauPret.creer(1L, demande, LocalDate.of(2026, 12, 31));
        Pret pret = nouveau.avecId(9L);

        assertThat(nouveau.dateDebut()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(nouveau.dateFin()).isEqualTo(LocalDate.of(2051, 12, 1));
        assertThat(pret.id()).isEqualTo(9L);
        assertThat(pret.statut()).isEqualTo(StatutPret.EN_ATTENTE);
        assertThat(pret.type()).isEqualTo(TypePret.AUTO);
        assertThat(pret.dureeMois()).isEqualTo(300);
        assertThat(pret.mensualite()).isEqualByComparingTo("16.67");
    }

    @Test
    @Tag("FR-024-01")
    void l_evenement_de_demande_exige_ses_trois_champs() {
        UUID id = UUID.randomUUID();
        Instant maintenant = Instant.parse("2026-10-06T08:30:00Z");

        assertThat(new PretDemande(id, 1L, maintenant).pretId()).isEqualTo(1L);
        assertThat(new PretDemande(id, 1L, maintenant).idEvenement()).isEqualTo(id);
        assertThat(new PretDemande(id, 1L, maintenant).horodatage()).isEqualTo(maintenant);
        assertThatThrownBy(() -> new PretDemande(null, 1L, maintenant)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PretDemande(id, 0L, maintenant)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PretDemande(id, 1L, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
