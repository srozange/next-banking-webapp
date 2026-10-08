/** Règles pures du centre de notifications (FEAT-002, FEAT-025) : types, icônes, comptage des non lues, horodatage. */

/** Notification telle que rendue par le service des notifications ; `creeLe` est un instant ISO-8601 UTC. */
export interface NotificationBrute {
  id: number;
  type: string;
  message: string;
  creeLe: string;
  lue: boolean;
}

/** Variante visuelle d'une notification (CMP-009) ; `neutre` pour un type sans variante prévue. */
export type VarianteNotification = 'confirmation' | 'alerte' | 'fraude' | 'neutre';

/** Notification prête à afficher. */
export interface NotificationAffichee {
  id: number;
  /** Type tel que rendu (« CONFIRMATION », « ALERTE », « FRAUDE ») : c'est aussi son libellé (FR-025-05). */
  type: string;
  /** Icône d'accompagnement (décorative), absente pour un type sans icône prévue. */
  icone: string | null;
  variante: VarianteNotification;
  message: string;
  /** `dd/MM/yyyy HH:mm:ss` en Europe/Paris. */
  horodatage: string;
  lue: boolean;
}

const TYPES: Readonly<Record<string, { icone: string; variante: VarianteNotification }>> = {
  CONFIRMATION: { icone: '✅', variante: 'confirmation' },
  ALERTE: { icone: '⚠️', variante: 'alerte' },
  FRAUDE: { icone: '🚨', variante: 'fraude' },
};

const FORMAT_HORODATAGE = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'Europe/Paris',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
});

/** Instant ISO-8601 affiché `dd/MM/yyyy HH:mm:ss` en Europe/Paris (FR-025-05, AR-45). */
export function formaterHorodatage(instantIso: string): string {
  const parties: Record<string, string> = {};
  for (const partie of FORMAT_HORODATAGE.formatToParts(new Date(instantIso))) {
    parties[partie.type] = partie.value;
  }
  return `${parties.day}/${parties.month}/${parties.year} ${parties.hour}:${parties.minute}:${parties.second}`;
}

/** Nombre de notifications non lues (FR-002-08). */
export function compterNonLues(notifications: readonly NotificationBrute[]): number {
  return notifications.filter((notification) => !notification.lue).length;
}

export function notificationAffichee(notification: NotificationBrute): NotificationAffichee {
  const connu = TYPES[notification.type];
  return {
    id: notification.id,
    type: notification.type,
    icone: connu?.icone ?? null,
    variante: connu?.variante ?? 'neutre',
    message: notification.message,
    horodatage: formaterHorodatage(notification.creeLe),
    lue: notification.lue,
  };
}
