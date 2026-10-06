import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import type { Pret } from '../../../domain/pret';
import { listerPrets, type ClientPrets } from '../../../application/prets';
import type { StockageJeton } from '../../../application/seConnecter';
import { TEXTES } from '../../textes';
import { SectionDemande } from './SectionDemande';
import { SectionEmprunts } from './SectionEmprunts';
import { SectionSimulateur } from './SectionSimulateur';
import './EcranPrets.css';

interface Proprietes {
  client: ClientPrets;
  stockage: StockageJeton;
}

/**
 * Écran /prets (FEAT-007), le même pour tout rôle authentifié : simulateur, formulaire de demande et liste des
 * emprunts de l'utilisateur, relue après chaque demande transmise. Session perdue : retour à /login.
 */
export function EcranPrets({ client, stockage }: Proprietes) {
  const [prets, setPrets] = useState<readonly Pret[] | null>(null);
  const [listeIndisponible, setListeIndisponible] = useState(false);
  const [deconnecte, setDeconnecte] = useState(false);

  const charger = useCallback(
    async (estActif: () => boolean) => {
      const resultat = await listerPrets(client, stockage);
      if (!estActif()) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        setDeconnecte(true);
      } else if (resultat.type === 'indisponible') {
        setListeIndisponible(true);
      } else {
        setListeIndisponible(false);
        setPrets(resultat.prets);
      }
    },
    [client, stockage],
  );

  useEffect(() => {
    let actif = true;
    void charger(() => actif);
    return () => {
      actif = false;
    };
  }, [charger]);

  if (deconnecte) {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  return (
    <div className="ecran-prets">
      <title>{TEXTES.titresOnglet.prets}</title>
      <SectionSimulateur client={client} stockage={stockage} surDeconnexion={() => setDeconnecte(true)} />
      <SectionDemande
        client={client}
        stockage={stockage}
        surDeconnexion={() => setDeconnecte(true)}
        surDepot={() => void charger(() => true)}
      />
      <SectionEmprunts prets={prets} indisponible={listeIndisponible} />
    </div>
  );
}
