package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-026-01")
@Tag("FR-001-12")
class EntreeAuditTest {

    private static final UUID ID = UUID.fromString("7b1e4c2a-5d3f-4e6a-8b9c-0d1e2f3a4b5c");
    private static final Instant HORODATAGE = Instant.parse("2026-10-05T08:30:00Z");

    @Test
    void une_connexion_reussie_a_pour_utilisateur_le_client_id_en_texte() {
        EntreeAudit entree = EntreeAudit.connexionReussie(1L, "203.0.113.7", ID, HORODATAGE);

        assertThat(entree.idEvenement()).isEqualTo(ID);
        assertThat(entree.utilisateur()).isEqualTo("1");
        assertThat(entree.action()).isEqualTo(ActionAudit.LOGIN);
        assertThat(entree.detail()).isEqualTo("Connexion réussie");
        assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
        assertThat(entree.horodatage()).isEqualTo(HORODATAGE);
    }

    @Test
    void une_deconnexion_a_pour_utilisateur_le_client_id_en_texte() {
        EntreeAudit entree = EntreeAudit.deconnexion(1L, "203.0.113.7", ID, HORODATAGE);

        assertThat(entree.utilisateur()).isEqualTo("1");
        assertThat(entree.action()).isEqualTo(ActionAudit.LOGOUT);
        assertThat(entree.detail()).isEqualTo("Déconnexion");
        assertThat(entree.adresseIp()).isEqualTo("203.0.113.7");
        assertThat(entree.horodatage()).isEqualTo(HORODATAGE);
    }

    @Test
    void une_tentative_echouee_a_pour_utilisateur_l_identifiant_saisi() {
        EntreeAudit entree = EntreeAudit.tentativeEchouee("retro-compte-inexistant", "10.0.0.1", ID, HORODATAGE);

        assertThat(entree.utilisateur()).isEqualTo("retro-compte-inexistant");
        assertThat(entree.action()).isEqualTo(ActionAudit.LOGIN_FAILED);
        assertThat(entree.detail()).isEqualTo("Tentative échouée");
        assertThat(entree.adresseIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void un_identifiant_saisi_trop_long_est_tronque_a_255_caracteres() {
        String long300 = "a".repeat(255) + "b".repeat(45);
        String long255 = "c".repeat(255);

        assertThat(EntreeAudit.tentativeEchouee(long300, null, ID, HORODATAGE).utilisateur())
                .isEqualTo(long300.substring(0, 255)).hasSize(255);
        assertThat(EntreeAudit.tentativeEchouee(long255, null, ID, HORODATAGE).utilisateur()).isEqualTo(long255);
        assertThat(EntreeAudit.tentativeEchouee(" ".repeat(255) + "x", null, ID, HORODATAGE).utilisateur())
                .isEqualTo("x");
        assertThat(EntreeAudit.tentativeEchouee(" ".repeat(10) + "d".repeat(300), null, ID, HORODATAGE).utilisateur())
                .isEqualTo("d".repeat(255));
        assertThatThrownBy(() -> EntreeAudit.tentativeEchouee(null, null, ID, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void l_adresse_ip_peut_etre_absente() {
        assertThat(new EntreeAudit(ID, "SYSTEM", ActionAudit.LOGIN, null, HORODATAGE).adresseIp()).isNull();
    }

    @Test
    void refuse_une_entree_incomplete() {
        assertThatThrownBy(() -> new EntreeAudit(null, "1", ActionAudit.LOGIN, null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, null, ActionAudit.LOGIN, null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, " ", ActionAudit.LOGIN, null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, "1", null, null, HORODATAGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntreeAudit(ID, "1", ActionAudit.LOGIN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
