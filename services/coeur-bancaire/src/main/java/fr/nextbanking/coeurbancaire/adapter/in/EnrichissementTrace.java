package fr.nextbanking.coeurbancaire.adapter.in;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Enrichit l'observation courante de la requête (AR-105) : l'attribut {@code virement.controle.issue} (EXECUTE,
 * SUSPENDU, REFUSE_ERREUR_CONTROLE) et, pour un refus, l'erreur. Aucune observation ni aucun span n'est créé ici :
 * celle de la requête HTTP suffit. Rend aussi l'identifiant de la trace courante, référence d'un refus.
 */
@Component
public class EnrichissementTrace {

    static final String ATTRIBUT_ISSUE = "virement.controle.issue";
    static final String REFERENCE_INDISPONIBLE = "indisponible";

    private final ObservationRegistry observations;
    private final ObjectProvider<Tracer> tracer;

    public EnrichissementTrace(ObservationRegistry observations, ObjectProvider<Tracer> tracer) {
        this.observations = observations;
        this.tracer = tracer;
    }

    /** Pose l'issue du contrôle sur l'observation courante, s'il y en a une. */
    void issue(String issue) {
        Observation courante = observations.getCurrentObservation();
        if (courante != null) {
            courante.lowCardinalityKeyValue(ATTRIBUT_ISSUE, issue);
        }
    }

    /** Signale l'erreur à l'observation courante, s'il y en a une. */
    void erreur(Throwable erreur) {
        Observation courante = observations.getCurrentObservation();
        if (courante != null) {
            courante.error(erreur);
        }
    }

    /** Identifiant de la trace courante, ou « indisponible » hors de toute trace. */
    String reference() {
        Tracer actif = tracer.getIfAvailable();
        Span span = actif == null ? null : actif.currentSpan();
        return span == null ? REFERENCE_INDISPONIBLE : span.context().traceId();
    }
}
