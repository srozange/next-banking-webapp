package fr.nextbanking.coeurbancaire.domain;

import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.DIX_HEURES;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.FRANCE;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.VIDE;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.evaluer;
import static fr.nextbanking.coeurbancaire.domain.ControleFraudeOutils.signal;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Signaux fondés sur le destinataire et l'heure : NEW_BENEFICIARY, UNUSUAL_HOURS, FOREIGN_IBAN. */
class ControleFraudeContexteTest {

    private static Instant parisA(String heure) {
        return LocalDateTime.parse("2026-11-17T" + heure).atZone(ZoneId.of("Europe/Paris")).toInstant();
    }

    @Test
    @Tag("FR-003-19")
    @Tag("FR-027-07")
    void new_beneficiary_vaut_35_pour_un_destinataire_non_beneficiaire_et_pas_pour_un_beneficiaire_actif() {
        assertThat(signal(evaluer("100.00", new Iban("FR76-0001-0002"), false, VIDE, DIX_HEURES),
                TypeSignal.NEW_BENEFICIARY)).contains(new Signal(TypeSignal.NEW_BENEFICIARY, 35,
                        "Target IBAN FR76-0001-0002 is not registered as a trusted beneficiary"));
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, DIX_HEURES), TypeSignal.NEW_BENEFICIARY)).isEmpty();
    }

    @Test
    @Tag("FR-003-20")
    @Tag("FR-027-08")
    void unusual_hours_vaut_45_a_05_59_et_23_30_heure_de_paris_et_pas_a_10_00() {
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("05:59:00")), TypeSignal.UNUSUAL_HOURS))
                .contains(new Signal(TypeSignal.UNUSUAL_HOURS, 45,
                        "Transaction initiated at hour 5 which is outside normal operating hours (06:00-23:00)"));
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("23:30:00")), TypeSignal.UNUSUAL_HOURS))
                .contains(new Signal(TypeSignal.UNUSUAL_HOURS, 45,
                        "Transaction initiated at hour 23 which is outside normal operating hours (06:00-23:00)"));
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("10:00:00")), TypeSignal.UNUSUAL_HOURS))
                .isEmpty();
    }

    @Test
    @Tag("FR-003-20")
    @Tag("FR-027-08")
    void l_heure_est_celle_du_fuseau_de_l_application_et_les_bornes_sont_6h_et_23h() {
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("06:00:00")), TypeSignal.UNUSUAL_HOURS))
                .isEmpty();
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("22:59:59")), TypeSignal.UNUSUAL_HOURS))
                .isEmpty();
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, parisA("23:00:00")), TypeSignal.UNUSUAL_HOURS))
                .isPresent();
        // 21:30 UTC en hiver = 22:30 à Paris : pas inhabituel ; 22:30 UTC = 23:30 à Paris : inhabituel.
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, Instant.parse("2026-11-17T21:30:00Z")),
                TypeSignal.UNUSUAL_HOURS)).isEmpty();
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, Instant.parse("2026-11-17T22:30:00Z")),
                TypeSignal.UNUSUAL_HOURS)).isPresent();
    }

    @Test
    @Tag("FR-003-22")
    @Tag("FR-027-09")
    void foreign_iban_vaut_30_pour_de89_seulement_sans_tenir_compte_de_la_casse() {
        assertThat(signal(evaluer("100.00", new Iban("DE89370400440532013000"), true, VIDE, DIX_HEURES),
                TypeSignal.FOREIGN_IBAN)).contains(new Signal(TypeSignal.FOREIGN_IBAN, 30,
                        "Target IBAN DE89370400440532013000 does not start with FR (possible foreign transfer)"));
        assertThat(signal(evaluer("100.00", new Iban("fr7630006000011234567890189"), true, VIDE, DIX_HEURES),
                TypeSignal.FOREIGN_IBAN)).isEmpty();
        assertThat(signal(evaluer("100.00", FRANCE, true, VIDE, DIX_HEURES), TypeSignal.FOREIGN_IBAN)).isEmpty();
    }
}
