import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import {
  chargerPipelinePrets,
  deciderPret,
  type ActionPret,
  type ClientPipelinePrets,
  type ContenuPipelinePrets,
} from '../../../application/pipelinePrets';
import {
  formaterDateEcheance,
  formaterDuree,
  formaterMontantLigne,
  formaterTaux,
  LIBELLES_STATUT,
  libelleType,
  type PretAdministration,
} from '../../../domain/pret';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { Tableau } from '../../ui/Tableau';
import './SectionsPretsAdministration.css';

const T = TEXTES.administrationPrets;
const C = T.pipeline.colonnes;
const H = T.historique.colonnes;
const COLONNES_PIPELINE = [C.id, C.client, C.type, C.montant, C.duree, C.taux, C.mensualite, C.motif, C.actions];
const COLONNES_HISTORIQUE = [H.id, H.client, H.type, H.montant, H.statut, H.duree, H.mensualite, H.dateDebut];

interface Proprietes {
  client: ClientPipelinePrets;
  /** Contenu rendu entre le pipeline et l'historique (« Gestion des clients », ordre figé de la page). */
  entrePipelineEtHistorique?: ReactNode;
}

/** Message affiché après une décision ; `numero` change à chaque décision pour reprendre le focus. */
interface Message {
  type: 'succes' | 'erreur';
  texte: string;
  numero: number;
}

const typeDe = (pret: PretAdministration) => (pret.type === null ? '' : libelleType(pret.type));

/**
 * Sections « Pipeline Prets — En attente d approbation » et « Historique de tous les prets » de l'écran Administration
 * (FR-016-03, FR-016-04, FR-016-05, FR-016-07, FR-016-12). « Approuver » et « Refuser » agissent sans confirmation ; le
 * pipeline et l'historique sont relus après chaque décision. Les boutons ne sont jamais masqués côté client : le
 * serveur décide (rôle, état du prêt). L'historique est en lecture seule. `entrePipelineEtHistorique` se place entre
 * les deux sections.
 */
export function SectionsPretsAdministration({ client, entrePipelineEtHistorique }: Proprietes) {
  const [contenu, setContenu] = useState<ContenuPipelinePrets | null>(null);
  const [message, setMessage] = useState<Message | null>(null);
  const [enCours, setEnCours] = useState(false);
  const alerte = useRef<HTMLDivElement>(null);

  const recharger = useCallback(() => chargerPipelinePrets(client).then(setContenu), [client]);

  useEffect(() => {
    let actif = true;
    void chargerPipelinePrets(client).then((lu) => {
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

  async function decider(action: ActionPret, pret: PretAdministration) {
    setEnCours(true);
    const issue = await deciderPret(action, pret.id, client);
    const affiche =
      issue === 'effectuee'
        ? { type: 'succes' as const, texte: action === 'approuver' ? T.succesApprouve : T.succesRefuse }
        : { type: 'erreur' as const, texte: T.erreurTraitement };
    setMessage((precedent) => ({ ...affiche, numero: (precedent?.numero ?? 0) + 1 }));
    await recharger();
    setEnCours(false);
  }

  const enAttente = contenu?.enAttente;
  const historique = contenu?.historique;
  return (
    <>
      <section className="section-prets-admin" aria-labelledby="admin-pipeline-titre" data-testid="admin-pipeline-section">
        <h2 id="admin-pipeline-titre" className="section-prets-admin__titre">
          {T.pipeline.titre}
        </h2>
        {message !== null &&
          (message.type === 'succes' ? (
            <AlerteSucces ref={alerte} testId="admin-message-succes">
              <p>{message.texte}</p>
            </AlerteSucces>
          ) : (
            <AlerteErreur ref={alerte} testId="admin-message-erreur">
              <p>{message.texte}</p>
            </AlerteErreur>
          ))}
        {enAttente === null && (
          <AlerteErreur testId="admin-pipeline-erreur">
            <p>{TEXTES.erreurTechnique}</p>
          </AlerteErreur>
        )}
        {enAttente !== null && enAttente !== undefined && (
          <Tableau testId="admin-pipeline" colonnes={COLONNES_PIPELINE}>
            {enAttente.length === 0 ? (
              <tr>
                <td colSpan={COLONNES_PIPELINE.length}>{T.pipeline.vide}</td>
              </tr>
            ) : (
              enAttente.map((pret) => (
                <tr key={pret.id}>
                  <td>{pret.id}</td>
                  <td>{pret.nomClient}</td>
                  <td>{typeDe(pret)}</td>
                  <td>{formaterMontantLigne(pret.montant)}</td>
                  <td>{formaterDuree(pret.dureeMois)}</td>
                  <td>{formaterTaux(pret.tauxAnnuel)}</td>
                  <td>{formaterMontantLigne(pret.mensualite)}</td>
                  <td>{pret.objet}</td>
                  <td className="section-prets-admin__actions">
                    <BoutonTexte data-testid={`admin-approuver-${pret.id}`} desactive={enCours} onClick={() => void decider('approuver', pret)}>
                      {T.pipeline.approuver}
                    </BoutonTexte>
                    <BoutonTexte data-testid={`admin-refuser-${pret.id}`} desactive={enCours} onClick={() => void decider('refuser', pret)}>
                      {T.pipeline.refuser}
                    </BoutonTexte>
                  </td>
                </tr>
              ))
            )}
          </Tableau>
        )}
      </section>
      {entrePipelineEtHistorique}
      <section className="section-prets-admin" aria-labelledby="admin-historique-titre" data-testid="admin-historique-section">
        <h2 id="admin-historique-titre" className="section-prets-admin__titre">
          {T.historique.titre}
        </h2>
        {historique === null && (
          <AlerteErreur testId="admin-historique-erreur">
            <p>{TEXTES.erreurTechnique}</p>
          </AlerteErreur>
        )}
        {historique !== null && historique !== undefined && (
          <Tableau testId="admin-historique" colonnes={COLONNES_HISTORIQUE}>
            {historique.length === 0 ? (
              <tr>
                <td colSpan={COLONNES_HISTORIQUE.length}>{T.historique.vide}</td>
              </tr>
            ) : (
              historique.map((pret) => (
                <tr key={pret.id}>
                  <td>{pret.id}</td>
                  <td>{pret.nomClient}</td>
                  <td>{typeDe(pret)}</td>
                  <td>{formaterMontantLigne(pret.montant)}</td>
                  <td>{LIBELLES_STATUT[pret.statut]}</td>
                  <td>{formaterDuree(pret.dureeMois)}</td>
                  <td>{formaterMontantLigne(pret.mensualite)}</td>
                  <td>{formaterDateEcheance(pret.dateDebut)}</td>
                </tr>
              ))
            )}
          </Tableau>
        )}
      </section>
    </>
  );
}
