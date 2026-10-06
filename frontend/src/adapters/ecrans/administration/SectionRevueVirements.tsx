import { useCallback, useEffect, useRef, useState } from 'react';
import { chargerRevue, type ActionRevue, type ClientRevue, type ContenuRevue, type ResultatDecision } from '../../../application/revue';
import { formaterDate, formaterHeure, type VirementEnRevue } from '../../../domain/virement';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Tableau } from '../../ui/Tableau';
import { ModaleDecision } from './ModaleDecision';
import './SectionRevueVirements.css';

const T = TEXTES.revue;
const COLONNES_FILE = [T.colonnes.client, T.colonnes.montant, T.colonnes.compte, T.colonnes.motif, T.colonnes.butoir, T.colonnes.actions];
const COLONNES_HISTORIQUE = [
  T.colonnes.client,
  T.colonnes.montant,
  T.colonnes.compte,
  T.colonnes.motif,
  T.colonnes.butoir,
  T.historique.colonneIssue,
  T.historique.colonneDate,
];

interface Proprietes {
  client: ClientRevue;
}

/** Message affiché après une décision ; `numero` change à chaque décision pour reprendre le focus. */
interface Message {
  type: 'succes' | 'erreur';
  texte: string;
  numero: number;
}

interface Decision {
  action: ActionRevue;
  virement: VirementEnRevue;
}

const heureButoir = (iso: string) => `${formaterDate(iso)} ${formaterHeure(iso)}`;

function messageDe(resultat: ResultatDecision): Omit<Message, 'numero'> | null {
  switch (resultat.type) {
    case 'enregistre':
      return { type: 'succes', texte: resultat.action === 'liberer' ? T.succesLiberer : T.succesRejeter };
    case 'refuse':
      return { type: 'erreur', texte: resultat.issue === 'REFUSEE_SOLDE_INSUFFISANT' ? T.erreurSoldeInsuffisant : T.erreurHeureButoir };
    case 'erreur':
      return { type: 'erreur', texte: resultat.message ?? TEXTES.erreurTechnique };
    case 'motif':
      return null;
  }
}

/**
 * Section de revue des virements suspendus de l'écran Administration (FR-016-19) : file triée par heure butoir,
 * modale de décision, puis historique des virements traités (seulement s'il en existe). Composant autonome : l'epic 9
 * le placera après « Gestion des clients » et avant « Alertes Fraude — Moteur de détection ». Les boutons ne sont
 * jamais masqués côté client : le serveur décide (rôle, état, heure butoir, solde).
 */
export function SectionRevueVirements({ client }: Proprietes) {
  const [contenu, setContenu] = useState<ContenuRevue | null>(null);
  const [decision, setDecision] = useState<Decision | null>(null);
  const [message, setMessage] = useState<Message | null>(null);
  const declencheur = useRef<HTMLElement | null>(null);
  const alerte = useRef<HTMLDivElement>(null);

  const recharger = useCallback(() => chargerRevue(client).then(setContenu), [client]);

  useEffect(() => {
    let actif = true;
    void chargerRevue(client).then((lu) => {
      if (actif) {
        setContenu(lu);
      }
    });
    return () => {
      actif = false;
    };
  }, [client]);

  useEffect(() => {
    if (message !== null) {
      alerte.current?.focus();
    }
  }, [message]);

  function ouvrir(action: ActionRevue, virement: VirementEnRevue, evenement: { currentTarget: HTMLElement }) {
    declencheur.current = evenement.currentTarget;
    setDecision({ action, virement });
  }

  function fermer() {
    setDecision(null);
    declencheur.current?.focus();
  }

  async function apresDecision(resultat: ResultatDecision) {
    const affiche = messageDe(resultat);
    if (affiche === null) {
      return;
    }
    setDecision(null);
    setMessage((precedent) => ({ ...affiche, numero: (precedent?.numero ?? 0) + 1 }));
    await recharger();
  }

  const file = contenu?.file;
  const historique = contenu?.historique;
  return (
    <section className="section-revue" aria-labelledby="revue-titre" data-testid="revue-section">
      <h2 id="revue-titre" className="section-revue__titre">
        {T.titre}
      </h2>
      {message !== null &&
        (message.type === 'succes' ? (
          <AlerteSucces ref={alerte} testId="revue-message">
            <p>{message.texte}</p>
          </AlerteSucces>
        ) : (
          <AlerteErreur ref={alerte} testId="revue-message-erreur">
            <p>{message.texte}</p>
          </AlerteErreur>
        ))}
      {file === null && (
        <AlerteErreur testId="revue-erreur">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {file !== null && file !== undefined && (
        <Tableau testId="revue-file" colonnes={COLONNES_FILE}>
          {file.length === 0 ? (
            <tr>
              <td colSpan={COLONNES_FILE.length}>{T.vide}</td>
            </tr>
          ) : (
            file.map((virement) => (
              <tr key={virement.virementId}>
                <td>{virement.nomClient ?? virement.clientId}</td>
                <td>{virement.montant}</td>
                <td>{virement.compteDestinataire}</td>
                <td>{virement.motifSuspension}</td>
                <td>{heureButoir(virement.heureButoir)}</td>
                <td className="section-revue__actions">
                  <BoutonTexte data-testid={`revue-liberer-${virement.virementId}`} onClick={(e) => ouvrir('liberer', virement, e)}>
                    {T.liberer}
                  </BoutonTexte>
                  <BoutonTexte data-testid={`revue-rejeter-${virement.virementId}`} onClick={(e) => ouvrir('rejeter', virement, e)}>
                    {T.rejeter}
                  </BoutonTexte>
                </td>
              </tr>
            ))
          )}
        </Tableau>
      )}
      {historique !== null && historique !== undefined && historique.length > 0 && (
        <>
          <h3 className="section-revue__sous-titre">{T.historique.titre}</h3>
          <Tableau testId="revue-historique" colonnes={COLONNES_HISTORIQUE}>
            {historique.map((traite) => (
              <tr key={traite.virement.virementId}>
                <td>{traite.virement.nomClient ?? traite.virement.clientId}</td>
                <td>{traite.virement.montant}</td>
                <td>{traite.virement.compteDestinataire}</td>
                <td>{traite.virement.motifSuspension}</td>
                <td>{heureButoir(traite.virement.heureButoir)}</td>
                <td data-testid={`revue-issue-${traite.virement.virementId}`}>{T.issues[traite.issue]}</td>
                <td>{heureButoir(traite.dateIssue)}</td>
              </tr>
            ))}
          </Tableau>
        </>
      )}
      {decision !== null && (
        <ModaleDecision
          key={`${decision.action}-${decision.virement.virementId}`}
          action={decision.action}
          virement={decision.virement}
          client={client}
          surFermeture={fermer}
          surResultat={apresDecision}
        />
      )}
    </section>
  );
}
