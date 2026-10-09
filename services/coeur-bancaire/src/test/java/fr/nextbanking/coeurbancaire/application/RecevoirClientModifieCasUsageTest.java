package fr.nextbanking.coeurbancaire.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie.Issue;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Projection locale des clients : consommateur idempotent, {@code versionAgregat} écarte un message ancien ; une
 * version supérieure écrase l'affectation au conseiller, y compris vers aucun conseiller.
 */
@Tag("FR-016-19")
class RecevoirClientModifieCasUsageTest {

    private final MemoireCoeurBancaire memoire = new MemoireCoeurBancaire();
    private final RecevoirClientModifieCasUsage cas =
            new RecevoirClientModifieCasUsage(memoire, memoire, memoire, memoire);

    @Test
    void le_nom_complet_est_enregistre_puis_remplace_par_une_version_plus_recente() {
        assertThat(cas.recevoir(UUID.randomUUID(), client("Claire Martin", 11L, 1))).isEqualTo(Issue.ENREGISTRE);
        assertThat(cas.recevoir(UUID.randomUUID(), client("Claire Dupont", 11L, 2))).isEqualTo(Issue.ENREGISTRE);

        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Dupont");
    }

    @Test
    void une_version_ancienne_ou_egale_est_ecartee() {
        cas.recevoir(UUID.randomUUID(), client("Claire Dupont", 11L, 3));

        assertThat(cas.recevoir(UUID.randomUUID(), client("Ancien nom", 12L, 2))).isEqualTo(Issue.IGNORE_ANCIEN);
        assertThat(cas.recevoir(UUID.randomUUID(), client("Même version", 12L, 3))).isEqualTo(Issue.IGNORE_ANCIEN);

        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Dupont");
        assertThat(memoire.clientsDuConseiller(11L)).containsExactly(7L);
        assertThat(memoire.clientsDuConseiller(12L)).isEmpty();
    }

    @Test
    @Tag("FR-015-02")
    void une_version_superieure_reaffecte_le_client_y_compris_vers_aucun_conseiller() {
        cas.recevoir(UUID.randomUUID(), client("Claire Dupont", 11L, 1));

        cas.recevoir(UUID.randomUUID(), client("Claire Dupont", 12L, 2));
        assertThat(memoire.clientsDuConseiller(11L)).isEmpty();
        assertThat(memoire.clientsDuConseiller(12L)).containsExactly(7L);

        cas.recevoir(UUID.randomUUID(), client("Claire Dupont", null, 3));
        assertThat(memoire.clientsDuConseiller(12L)).isEmpty();
        assertThat(memoire.conseillers).containsEntry(7L, null);
    }

    @Test
    void un_message_livre_deux_fois_n_a_qu_un_effet() {
        UUID id = UUID.randomUUID();
        cas.recevoir(id, client("Claire Martin", 11L, 1));

        assertThat(cas.recevoir(id, client("Autre", 12L, 9))).isEqualTo(Issue.DEJA_TRAITE);
        assertThat(memoire.noms.get(7L)).isEqualTo("Claire Martin");
    }

    @Test
    void un_identifiant_d_evenement_absent_est_refuse() {
        assertThatThrownBy(() -> cas.recevoir(null, client("x", 11L, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    private static ClientProjete client(String nom, Long conseillerId, long version) {
        return new ClientProjete(7L, nom, conseillerId, false, version);
    }
}
