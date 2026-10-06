import { useEffect, useState } from 'react';
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
 * Écran /prets (FEAT-007), le même pour tout rôle authentifié : simulateur, formulaire de demande (inerte, la
 * soumission arrive avec la story 5.2) et liste des emprunts de l'utilisateur. Session perdue : retour à /login.
 */
export function EcranPrets({ client, stockage }: Proprietes) {
  const [prets, setPrets] = useState<readonly Pret[] | null>(null);
  const [listeIndisponible, setListeIndisponible] = useState(false);
  const [deconnecte, setDeconnecte] = useState(false);

  useEffect(() => {
    let actif = true;
    void listerPrets(client, stockage).then((resultat) => {
      if (!actif) {
        return;
      }
      if (resultat.type === 'deconnecte') {
        setDeconnecte(true);
      } else if (resultat.type === 'indisponible') {
        setListeIndisponible(true);
      } else {
        setPrets(resultat.prets);
      }
    });
    return () => {
      actif = false;
    };
  }, [client, stockage]);

  if (deconnecte) {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  return (
    <div className="ecran-prets">
      <title>{TEXTES.titresOnglet.prets}</title>
      <SectionSimulateur client={client} stockage={stockage} surDeconnexion={() => setDeconnecte(true)} />
      <SectionDemande />
      <SectionEmprunts prets={prets} indisponible={listeIndisponible} />
    </div>
  );
}
