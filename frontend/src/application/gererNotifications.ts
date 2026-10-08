import { compterNonLues, notificationAffichee, type NotificationAffichee, type NotificationBrute } from '../domain/notifications';

/** Réponse de la liste des notifications du client de la session, traduite par l'adaptateur HTTP. */
export type ReponseNotifications = { statut: 'liste'; notifications: readonly NotificationBrute[] } | { statut: 'technique' };

/** Réponse d'une action sur toutes les notifications du client de la session. */
export type ReponseActionNotifications = { statut: 'effectuee' } | { statut: 'technique' };

/** Port sortant : notifications du client de la session (l'identité vient de la session, jamais d'un paramètre). */
export interface ClientNotifications {
  lister(): Promise<ReponseNotifications>;
  toutMarquerLu(): Promise<ReponseActionNotifications>;
  toutEffacer(): Promise<ReponseActionNotifications>;
}

/** Centre de notifications prêt à afficher, de la plus récente à la plus ancienne selon le service. */
export interface CentreNotifications {
  notifications: readonly NotificationAffichee[];
  nonLues: number;
}

async function lire(client: ClientNotifications): Promise<CentreNotifications | null> {
  const reponse = await client.lister();
  if (reponse.statut !== 'liste') {
    return null;
  }
  return { notifications: reponse.notifications.map(notificationAffichee), nonLues: compterNonLues(reponse.notifications) };
}

/** Cas d'usage (FR-002-07, FR-025-05) : notifications du client ; null si le service est indisponible. */
export async function chargerNotifications(client: ClientNotifications): Promise<CentreNotifications | null> {
  try {
    return await lire(client);
  } catch {
    return null;
  }
}

async function agirPuisRelire(
  client: ClientNotifications,
  action: (client: ClientNotifications) => Promise<ReponseActionNotifications>,
): Promise<CentreNotifications | null> {
  try {
    const reponse = await action(client);
    return reponse.statut === 'effectuee' ? await lire(client) : null;
  } catch {
    return null;
  }
}

/** Cas d'usage (FR-002-08, FR-025-06) : tout marquer lu, puis relire ; null si l'action ou la relecture échoue. */
export function toutMarquerLu(client: ClientNotifications): Promise<CentreNotifications | null> {
  return agirPuisRelire(client, (c) => c.toutMarquerLu());
}

/** Cas d'usage (FR-002-08, FR-025-07) : tout effacer, puis relire ; null si l'action ou la relecture échoue. */
export function toutEffacer(client: ClientNotifications): Promise<CentreNotifications | null> {
  return agirPuisRelire(client, (c) => c.toutEffacer());
}
