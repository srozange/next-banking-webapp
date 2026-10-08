import type { ClientNotifications, ReponseActionNotifications, ReponseNotifications } from '../../application/gererNotifications';
import type { NotificationBrute } from '../../domain/notifications';

export const CHEMIN_NOTIFICATIONS = '/api/notifications/notifications';
export const CHEMIN_TOUT_MARQUER_LU = `${CHEMIN_NOTIFICATIONS}/lecture`;

const estNotification = (valeur: unknown): valeur is NotificationBrute => {
  const notification = valeur as Partial<Record<keyof NotificationBrute, unknown>> | null;
  return (
    typeof notification?.id === 'number' &&
    typeof notification.type === 'string' &&
    (notification.titre === null || typeof notification.titre === 'string') &&
    typeof notification.message === 'string' &&
    typeof notification.creeLe === 'string' &&
    !Number.isNaN(Date.parse(notification.creeLe)) &&
    typeof notification.lue === 'boolean'
  );
};

/**
 * Adaptateur HTTP des notifications. `appeler` porte le jeton et la règle du 401 (fetchAuthentifie) ; aucun
 * paramètre n'est envoyé : l'identité vient du jeton. Toute réponse inattendue, tout corps illisible et toute coupure
 * sont rendus « technique ».
 */
export function creerClientNotificationsHttp(appeler: typeof fetch): ClientNotifications {
  async function agir(chemin: string, methode: 'POST' | 'DELETE'): Promise<ReponseActionNotifications> {
    try {
      const reponse = await appeler(chemin, { method: methode, headers: { Accept: 'application/json' } });
      return reponse.status === 204 ? { statut: 'effectuee' } : { statut: 'technique' };
    } catch {
      return { statut: 'technique' };
    }
  }

  return {
    async lister(): Promise<ReponseNotifications> {
      try {
        const reponse = await appeler(CHEMIN_NOTIFICATIONS, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estNotification) ? { statut: 'liste', notifications: corps } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    toutMarquerLu: () => agir(CHEMIN_TOUT_MARQUER_LU, 'POST'),
    toutEffacer: () => agir(CHEMIN_NOTIFICATIONS, 'DELETE'),
  };
}
