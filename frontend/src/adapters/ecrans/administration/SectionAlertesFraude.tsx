import { useRef, useState } from 'react';
import {
  revoirAlerteFraude,
  type ClientAdministration,
  type IssueRevueAlerte,
  type PartieAdministration,
} from '../../../application/consulterAdministration';
import type { SessionLocale } from '../../../application/session';
import {
  alerteOuverte,
  compterAlertesOuvertes,
  dateAlerte,
  relecteurAlerte,
  scoreAlerte,
  type AlerteFraudeAdministree,
} from '../../../domain/administration';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonFantome } from '../../ui/BoutonFantome';
import { Etiquette } from '../../ui/Etiquette';
import { Tableau } from '../../ui/Tableau';

const A = TEXTES.administration.alertes;
const COLONNES = [
  A.colonnes.id,
  A.colonnes.client,
  A.colonnes.regle,
  A.colonnes.score,
  A.colonnes.statut,
  A.colonnes.description,
  A.colonnes.date,
  A.colonnes.actions,
];

type Alertes = PartieAdministration<readonly AlerteFraudeAdministree[]>;

interface Proprietes {
  /** Toutes les alertes de fraude ; null pendant le chargement. */
  partie: Alertes | null;
  client: ClientAdministration;
  session: SessionLocale;
  /** Appelé quand la session est terminée (401 ou session fermée) : retour à la connexion. */
  surDeconnexion: () => void;
}

/**
 * Section « Alertes Fraude — Moteur de détection » de l'écran Administration (FR-016-14 à FR-016-16), après la revue
 * des virements suspendus : toutes les alertes, tous statuts (BUG-056 corrigé), la plus récente d'abord, badge du
 * nombre d'alertes ouvertes s'il est positif. « Confirmer » et « Faux positif » (alertes OPEN seulement) agissent sans
 * confirmation ni message de succès : le bouton est `aria-disabled` pendant la requête, puis les alertes sont relues ;
 * la revue ne touche jamais au virement suspendu (FR-027-12). En échec, le message reçu s'affiche dans la section.
 */
export function SectionAlertesFraude({ partie, client, session, surDeconnexion }: Proprietes) {
  const [source, setSource] = useState(partie);
  const [alertes, setAlertes] = useState<Alertes | null>(partie);
  const [enCours, setEnCours] = useState<ReadonlySet<number>>(new Set());
  const [erreurAction, setErreurAction] = useState<string | null>(null);
  /** Alertes dont la requête est partie : garde synchrone contre un second clic avant le rendu suivant. */
  const demandes = useRef(new Set<number>());

  if (partie !== source) {
    setSource(partie);
    setAlertes(partie);
  }

  async function revoir(alerteId: number, issue: IssueRevueAlerte) {
    if (demandes.current.has(alerteId)) {
      return;
    }
    demandes.current.add(alerteId);
    setEnCours((courantes) => new Set(courantes).add(alerteId));
    setErreurAction(null);
    const resultat = await revoirAlerteFraude(client, session, alerteId, issue);
    demandes.current.delete(alerteId);
    setEnCours((courantes) => {
      const restantes = new Set(courantes);
      restantes.delete(alerteId);
      return restantes;
    });
    if (resultat.type === 'deconnecte') {
      surDeconnexion();
      return;
    }
    if (resultat.type === 'erreur') {
      setErreurAction(resultat.message || TEXTES.erreurTechnique);
      return;
    }
    setAlertes(resultat.alertes);
  }

  const lignes = alertes?.donnees ?? null;
  const ouvertes = lignes === null ? 0 : compterAlertesOuvertes(lignes);
  const message = erreurAction ?? alertes?.message ?? null;
  return (
    <section className="ecran-administration__section" aria-labelledby="admin-alertes-titre" data-testid="admin-alertes-section">
      <div className="ecran-administration__entete-section">
        <h2 id="admin-alertes-titre" className="ecran-administration__titre">
          {A.titre}
        </h2>
        {ouvertes > 0 && <Etiquette data-testid="admin-alertes-badge">{A.ouvertes(ouvertes)}</Etiquette>}
      </div>
      {message !== null && (
        <AlerteErreur testId="admin-message-erreur">
          <p>{message}</p>
        </AlerteErreur>
      )}
      {lignes !== null && (
        <Tableau testId="admin-alertes" colonnes={COLONNES}>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={COLONNES.length}>{A.vide}</td>
            </tr>
          ) : (
            lignes.map((alerte) => (
              <tr key={alerte.alerteId} data-testid={`admin-alerte-${alerte.alerteId}`}>
                <td>{alerte.alerteId}</td>
                <td>{alerte.nomClient ?? A.clientIntrouvable(alerte.clientId)}</td>
                <td>{alerte.regle}</td>
                <td>{scoreAlerte(alerte)}</td>
                <td>
                  <Etiquette data-testid={`admin-alerte-statut-${alerte.alerteId}`}>{alerte.statut}</Etiquette>
                </td>
                <td>{alerte.description ?? ''}</td>
                <td>{dateAlerte(alerte)}</td>
                <td>
                  {alerteOuverte(alerte) ? (
                    <div className="ecran-administration__actions">
                      <BoutonFantome
                        data-testid={`admin-alerte-confirmer-${alerte.alerteId}`}
                        occupe={enCours.has(alerte.alerteId)}
                        onClick={() => void revoir(alerte.alerteId, 'confirmation')}
                      >
                        {A.confirmer}
                      </BoutonFantome>
                      <BoutonFantome
                        data-testid={`admin-alerte-faux-positif-${alerte.alerteId}`}
                        occupe={enCours.has(alerte.alerteId)}
                        onClick={() => void revoir(alerte.alerteId, 'faux-positif')}
                      >
                        {A.fauxPositif}
                      </BoutonFantome>
                    </div>
                  ) : (
                    A.traitePar(relecteurAlerte(alerte))
                  )}
                </td>
              </tr>
            ))
          )}
        </Tableau>
      )}
    </section>
  );
}
