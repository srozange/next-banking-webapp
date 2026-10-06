package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Prêt à enregistrer, en attente d'approbation et sans identifiant : mensualité, coût total et total des intérêts
 * calculés comme la simulation ; début le 1er du mois courant, fin = début + durée.
 */
public record NouveauPret(long clientId, TypePret type, BigDecimal montant, BigDecimal tauxAnnuel, int dureeMois,
        BigDecimal mensualite, BigDecimal coutTotal, BigDecimal totalInterets, String objet, LocalDate dateDebut,
        LocalDate dateFin) {

    /** Crée le prêt de la demande pour le client, à la date du jour. */
    public static NouveauPret creer(long clientId, DemandePret demande, LocalDate aujourdhui) {
        Simulation simulation = Annuite.simuler(
                new DemandeSimulation(demande.montant(), demande.tauxAnnuel(), demande.dureeMois()));
        LocalDate debut = aujourdhui.withDayOfMonth(1);
        return new NouveauPret(clientId, demande.type(), demande.montant(), demande.tauxAnnuel(),
                demande.dureeMois(), simulation.mensualite(), simulation.coutTotal(), simulation.totalInterets(),
                demande.objet(), debut, debut.plusMonths(demande.dureeMois()));
    }

    /** Le prêt enregistré sous l'identifiant donné, statut EN_ATTENTE ; taux à 4 décimales comme en base. */
    public Pret avecId(long id) {
        return new Pret(id, clientId, type, montant, tauxAnnuel.setScale(4, RoundingMode.HALF_UP), dureeMois, mensualite, coutTotal, totalInterets,
                objet, StatutPret.EN_ATTENTE, dateDebut, dateFin);
    }
}
