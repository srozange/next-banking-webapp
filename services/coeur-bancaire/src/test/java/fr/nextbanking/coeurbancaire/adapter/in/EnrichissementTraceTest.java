package fr.nextbanking.coeurbancaire.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.nextbanking.coeurbancaire.domain.Role;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Enrichissement de l'observation courante (AR-105) : l'attribut {@code virement.controle.issue} et l'erreur sont posés
 * sur l'observation en cours, sans en créer ; la référence est l'identifiant de la trace courante.
 */
@Tag("FR-003-27")
@Tag("NFR-OBS-02")
class EnrichissementTraceTest {

    private static StaticListableBeanFactory avecTracer(Tracer tracer) {
        StaticListableBeanFactory fabrique = new StaticListableBeanFactory();
        if (tracer != null) {
            fabrique.addBean("tracer", tracer);
        }
        return fabrique;
    }

    private static ObservationRegistry registre() {
        ObservationRegistry registre = ObservationRegistry.create();
        registre.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public boolean supportsContext(Observation.Context contexte) {
                return true;
            }
        });
        return registre;
    }

    @Test
    void l_issue_et_l_erreur_sont_posees_sur_l_observation_courante() {
        ObservationRegistry registre = registre();
        EnrichissementTrace trace = new EnrichissementTrace(registre, avecTracer(null).getBeanProvider(Tracer.class));
        Observation observation = Observation.start("http.server.requests", registre);
        IllegalStateException erreur = new IllegalStateException("contrôle en erreur");

        try (Observation.Scope scope = observation.openScope()) {
            trace.issue("REFUSE_ERREUR_CONTROLE");
            trace.erreur(erreur);
        }
        observation.stop();

        assertThat(observation.getContext().getLowCardinalityKeyValue("virement.controle.issue").getValue())
                .isEqualTo("REFUSE_ERREUR_CONTROLE");
        assertThat(observation.getContext().getError()).isSameAs(erreur);
    }

    @Test
    void sans_observation_courante_rien_n_est_pose_et_rien_n_est_cree() {
        ObservationRegistry registre = registre();
        EnrichissementTrace trace = new EnrichissementTrace(registre, avecTracer(null).getBeanProvider(Tracer.class));

        trace.issue("EXECUTE");
        trace.erreur(new IllegalStateException());

        assertThat(registre.getCurrentObservation()).isNull();
    }

    @Test
    void la_reference_est_l_identifiant_de_la_trace_courante_ou_indisponible() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext contexte = mock(TraceContext.class);
        when(contexte.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        when(span.context()).thenReturn(contexte);

        when(tracer.currentSpan()).thenReturn(span);
        assertThat(new EnrichissementTrace(registre(), avecTracer(tracer).getBeanProvider(Tracer.class)).reference())
                .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");

        when(tracer.currentSpan()).thenReturn(null);
        assertThat(new EnrichissementTrace(registre(), avecTracer(tracer).getBeanProvider(Tracer.class)).reference())
                .isEqualTo("indisponible");
        assertThat(new EnrichissementTrace(registre(), avecTracer(null).getBeanProvider(Tracer.class)).reference())
                .isEqualTo("indisponible");
    }

    @Test
    void les_roles_connus_du_jeton_sont_lus_les_autres_valeurs_ignorees() {
        Jwt avec = Jwt.withTokenValue("t").header("alg", "none").subject("1")
                .claim("roles", java.util.List.of("ADMIN", "inconnu")).build();
        Jwt sans = Jwt.withTokenValue("t").header("alg", "none").subject("1").build();

        assertThat(JetonRoles.depuis(avec)).containsExactly(Role.ADMIN);
        assertThat(JetonRoles.depuis(sans)).isEmpty();
    }
}
