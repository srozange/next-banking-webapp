const FORMAT = new Intl.DateTimeFormat('fr-FR', {
  timeZone: 'Europe/Paris',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/** Instant ISO-8601 affiché au format `dd/MM/yyyy HH:mm` en Europe/Paris (FR-005-06). */
export function formaterDate(instantIso: string): string {
  const parties = Object.fromEntries(FORMAT.formatToParts(new Date(instantIso)).map((partie) => [partie.type, partie.value]));
  return `${parties.day}/${parties.month}/${parties.year} ${parties.hour}:${parties.minute}`;
}
