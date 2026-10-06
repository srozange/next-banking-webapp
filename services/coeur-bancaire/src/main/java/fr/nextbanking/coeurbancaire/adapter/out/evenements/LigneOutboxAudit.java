package fr.nextbanking.coeurbancaire.adapter.out.evenements;

/**
 * Ligne de la table {@code outbox_audit} : message prêt à publier.
 */
record LigneOutboxAudit(long id, String topic, String cle, String charge) {
}
