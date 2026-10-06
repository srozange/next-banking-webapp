package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie.Issue;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Projection locale des clients : consommateur idempotent, {@code versionAgregat} écarte un message ancien. */
@Tag("FR-016-19")
class RecevoirClientModifieCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final RecevoirClientModifieCasUsage cas =
            new RecevoirClientModifieCasUsage(memoire, memoire, memoire, memoire);

    @Test
    void le_nom_complet_est_enregistre_puis_remplace_par_une_version_plus_recente() {
        assertThat(cas.recevoir(UUID.randomUUID(), 7, "Claire Martin", 1)).isEqualTo(Issue.ENREGISTRE);
        assertThat(cas.recevoir(UUID.randomUUID(), 7, "Claire Dupont", 2)).isEqualTo(Issue.ENREGISTRE);

        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Dupont");
    }

    @Test
    void une_version_ancienne_ou_egale_est_ecartee() {
        cas.recevoir(UUID.randomUUID(), 7, "Claire Dupont", 3);

        assertThat(cas.recevoir(UUID.randomUUID(), 7, "Ancien nom", 2)).isEqualTo(Issue.IGNORE_ANCIEN);
        assertThat(cas.recevoir(UUID.randomUUID(), 7, "Même version", 3)).isEqualTo(Issue.IGNORE_ANCIEN);

        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Dupont");
    }

    @Test
    void un_message_livre_deux_fois_n_a_qu_un_effet() {
        UUID id = UUID.randomUUID();
        cas.recevoir(id, 7, "Claire Martin", 1);

        assertThat(cas.recevoir(id, 7, "Autre", 9)).isEqualTo(Issue.DEJA_TRAITE);
        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Martin");
    }

    @Test
    void un_identifiant_d_evenement_absent_est_refuse() {
        assertThatThrownBy(() -> cas.recevoir(null, 7, "x", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
