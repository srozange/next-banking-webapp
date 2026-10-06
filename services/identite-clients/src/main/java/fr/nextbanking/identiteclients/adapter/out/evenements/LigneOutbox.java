package fr.nextbanking.identiteclients.adapter.out.evenements;

/**
 * Ligne de la table {@code outbox} : message prêt à publier.
 */
record LigneOutbox(long id, String topic, String cle, String charge) {
}
