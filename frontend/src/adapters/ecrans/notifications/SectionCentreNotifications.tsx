import { useEffect, useState } from 'react';
import {
  chargerNotifications,
  toutEffacer,
  toutMarquerLu,
  type CentreNotifications,
  type ClientNotifications,
} from '../../../application/gererNotifications';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import './SectionCentreNotifications.css';

const T = TEXTES.notifications;

interface Proprietes {
  client: ClientNotifications;
}

/**
 * Section « Centre de notifications » (FR-002-07, FR-002-08, FR-025-05 à FR-025-07) : les notifications du client de
 * la session, de la plus récente à la plus ancienne, le badge des non lues (zone `aria-live="polite"`), « Tout
 * marquer lu » s'il y a des non lues, « Effacer tout » s'il y a des notifications, sinon le message d'état vide.
 * Placée après le contenu comparé et avant « Suivi de mes virements » (UX-DR153). Erreur du service : texte
 * technique, rien d'autre.
 */
export function SectionCentreNotifications({ client }: Proprietes) {
  // undefined : chargement ; null : service indisponible.
  const [centre, setCentre] = useState<CentreNotifications | null | undefined>(undefined);

  useEffect(() => {
    let actif = true;
    void chargerNotifications(client).then((lu) => {
      if (actif) {
        setCentre(lu);
      }
    });
    return () => {
      actif = false;
    };
  }, [client]);

  const agir = (action: typeof toutMarquerLu) => {
    void action(client).then(setCentre);
  };

  return (
    <section className="centre-notifications" aria-labelledby="notifications-titre" data-testid="notifications-section">
      <div className="centre-notifications__entete">
        <h2 id="notifications-titre" className="centre-notifications__titre" data-testid="notifications-titre">
          {T.titre}
        </h2>
        <span aria-live="polite" data-testid="notifications-badge-zone">
          {centre !== null && centre !== undefined && centre.nonLues > 0 && (
            <Etiquette data-testid="notifications-badge">{T.nouvelles(centre.nonLues)}</Etiquette>
          )}
        </span>
      </div>
      {centre === null && (
        <AlerteErreur testId="notifications-message-erreur">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {centre !== null && centre !== undefined && (
        <>
          {centre.notifications.length > 0 && (
            <div className="centre-notifications__actions">
              {centre.nonLues > 0 && (
                <BoutonSecondaire data-testid="notifications-tout-marquer-lu" onClick={() => agir(toutMarquerLu)}>
                  {T.toutMarquerLu}
                </BoutonSecondaire>
              )}
              <BoutonSecondaire data-testid="notifications-effacer-tout" onClick={() => agir(toutEffacer)}>
                {T.effacerTout}
              </BoutonSecondaire>
            </div>
          )}
          {centre.notifications.length === 0 ? (
            <Carte attenuee>
              <p className="centre-notifications__vide" data-testid="notifications-vide">
                {T.vide}
              </p>
            </Carte>
          ) : (
            <ul className="centre-notifications__liste">
              {centre.notifications.map((notification) => (
                <li
                  key={notification.id}
                  className={`centre-notifications__ligne centre-notifications__ligne--${notification.variante}`}
                  data-testid={`notifications-notification-${notification.id}`}
                >
                  <p className="centre-notifications__type">
                    {notification.icone !== null && (
                      <span className="centre-notifications__icone" aria-hidden="true">
                        {notification.icone}
                      </span>
                    )}
                    <span data-testid={`notifications-type-${notification.id}`}>{notification.type}</span>
                    {!notification.lue && (
                      <span
                        className="centre-notifications__pastille"
                        role="img"
                        aria-label={T.nonLu}
                        title={T.nonLu}
                        data-testid={`notifications-non-lu-${notification.id}`}
                      />
                    )}
                  </p>
                  <p className="centre-notifications__message" data-testid={`notifications-message-${notification.id}`}>
                    {notification.message}
                  </p>
                  <p className="centre-notifications__horodatage" data-testid={`notifications-horodatage-${notification.id}`}>
                    {notification.horodatage}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  );
}
