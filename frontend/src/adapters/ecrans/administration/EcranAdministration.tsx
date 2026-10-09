import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import { chargerAdministration, type ClientAdministration, type ResultatAdministration } from '../../../application/consulterAdministration';
import type { ClientPipelinePrets } from '../../../application/pipelinePrets';
import type { ClientRevue } from '../../../application/revue';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { SectionGestionClients } from './SectionGestionClients';
import { SectionIndicateursAdministration } from './SectionIndicateursAdministration';
import { SectionRevueVirements } from './SectionRevueVirements';
import { SectionsPretsAdministration } from './SectionsPretsAdministration';
import { SectionUtilisateurs } from './SectionUtilisateurs';
import './EcranAdministration.css';

interface Proprietes {
  client: ClientAdministration;
  clientPipelinePrets: ClientPipelinePrets;
  clientRevue: ClientRevue;
  session: SessionLocale;
}

type EtatAdministration = Extract<ResultatAdministration, { type: 'etat' }>;

/**
 * Écran /administration (FEAT-016), pour l'ADMIN (les autres rôles sont redirigés par la garde) : sections dans
 * l'ordre figé — indicateurs, « Pipeline Prets », « Gestion des clients », « Historique de tous les prets », « Tous
 * les utilisateurs du systeme », revue des virements suspendus. Les quatre lectures de l'administration partent
 * ensemble ; une panne n'affiche que son message dans sa section ; un 401 ramène à la connexion.
 */
export function EcranAdministration({ client, clientPipelinePrets, clientRevue, session }: Proprietes) {
  const naviguer = useNavigate();
  const [etat, setEtat] = useState<EtatAdministration | null>(null);

  useEffect(() => {
    let actif = true;
    void chargerAdministration(client, session).then((resultat) => {
      if (!actif) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        void naviguer(ROUTE_CONNEXION, { replace: true });
        return;
      }
      setEtat(resultat);
    });
    return () => {
      actif = false;
    };
  }, [client, session, naviguer]);

  return (
    <div className="ecran-administration" aria-busy={etat === null ? 'true' : undefined} data-testid="admin-ecran">
      <title>{TEXTES.titresOnglet.administration}</title>
      <SectionIndicateursAdministration identite={etat?.indicateursIdentite ?? null} prets={etat?.indicateursPrets ?? null} />
      <SectionsPretsAdministration
        client={clientPipelinePrets}
        entrePipelineEtHistorique={<SectionGestionClients partie={etat?.clients ?? null} />}
      />
      <SectionUtilisateurs partie={etat?.utilisateurs ?? null} />
      <SectionRevueVirements client={clientRevue} />
    </div>
  );
}
