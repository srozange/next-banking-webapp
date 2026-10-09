import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import {
  ABSENT,
  DUREE_ILLIMITEE,
  formaterBooleen,
  formaterDepotMin,
  formaterDuree,
  formaterInstant,
  formaterNombre,
  formaterPourcentage,
  formaterRisque,
  valeurOuAbsent,
  type Catalogue,
  type Traitement,
} from '../../../domain/produits';
import { chargerProduits, type ClientProduits, type EtatProduits } from '../../../application/consulterProduits';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import './EcranProduits.css';

const T = TEXTES.produits;

interface Proprietes {
  client: ClientProduits;
  session: SessionLocale;
}

/** Statuts connus du journal : une classe de couleur ; tout autre statut s'affiche sans badge coloré. */
const CLASSES_STATUT: Readonly<Record<string, string>> = {
  SUCCESS: 'ecran-produits__badge--oui',
  FAILED: 'ecran-produits__badge--non',
  PARTIAL: 'ecran-produits__badge--partiel',
};

/**
 * Écran /produits (FEAT-011), identique pour CLIENT, CONSEILLER et ADMIN, en lecture seule : catalogue des produits
 * (service open-banking-conseiller) et journal des traitements nocturnes (cœur bancaire), chargés en parallèle ;
 * « Rafraichir » refait les deux appels. La panne d'un service n'empêche pas d'afficher l'autre.
 */
export function EcranProduits({ client, session }: Proprietes) {
  const naviguer = useNavigate();
  const [etat, setEtat] = useState<EtatProduits | null>(null);
  const [occupe, setOccupe] = useState(true);
  /** Numéro de la dernière demande : une réponse plus ancienne est ignorée. */
  const demande = useRef(0);
  const actif = useRef(true);

  async function charger() {
    const numero = ++demande.current;
    setOccupe(true);
    const resultat = await chargerProduits(client, session);
    if (!actif.current || numero !== demande.current) {
      return;
    }
    if (resultat.type === 'deconnecte') {
      void naviguer(ROUTE_CONNEXION, { replace: true });
      return;
    }
    setEtat(resultat.etat);
    setOccupe(false);
  }

  useEffect(() => {
    actif.current = true;
    void charger();
    return () => {
      actif.current = false;
    };
    // Chargement unique à l'ouverture de l'écran.
  }, []);

  return (
    <div className="ecran-produits" aria-busy={occupe ? 'true' : undefined} data-testid="produits-contenu">
      <title>{TEXTES.titresOnglet.produits}</title>
      {etat?.technique === true && (
        <AlerteErreur testId="produits-erreur-technique">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      {etat !== null && (
        <>
          {etat.catalogue !== null && <SectionCatalogue catalogue={etat.catalogue} onRafraichir={() => void charger()} />}
          {etat.catalogue === null && (
            <div>
              <BoutonSecondaire data-testid="produits-rafraichir" onClick={() => void charger()}>
                {T.rafraichir}
              </BoutonSecondaire>
            </div>
          )}
          {etat.traitements !== null && <SectionTraitements traitements={etat.traitements} />}
        </>
      )}
    </div>
  );
}

interface ProprietesCatalogue {
  catalogue: Catalogue;
  onRafraichir: () => void;
}

function SectionCatalogue({ catalogue, onRafraichir }: ProprietesCatalogue) {
  return (
    <section className="ecran-produits__section" aria-labelledby="produits-titre-catalogue" data-testid="produits-catalogue">
      <p className="ecran-produits__separateur">{T.catalogue}</p>
      <div className="ecran-produits__entete">
        <h2 id="produits-titre-catalogue" className="ecran-produits__titre">
          {T.catalogue}
        </h2>
        <BoutonSecondaire data-testid="produits-rafraichir" onClick={onRafraichir}>
          {T.rafraichir}
        </BoutonSecondaire>
      </div>
      <Groupe id="epargne" titre={T.epargne.titre} aucun={T.epargne.aucun} vide={catalogue.epargne.length === 0}>
        <thead>
          <tr>
            <th scope="col">{T.nom}</th>
            <th scope="col">{T.description}</th>
            <th scope="col">{T.taux}</th>
            <th scope="col">{T.epargne.depotMin}</th>
            <th scope="col">{T.epargne.capitalGaranti}</th>
            <th scope="col">{T.dureeMax}</th>
          </tr>
        </thead>
        <tbody>
          {catalogue.epargne.map((produit) => (
            <tr key={produit.id}>
              <td className="ecran-produits__nom">{produit.nom}</td>
              <td>{valeurOuAbsent(produit.description)}</td>
              <td>{formaterPourcentage(produit.taux)}</td>
              <td>{formaterDepotMin(produit.depotMin)}</td>
              <td>
                <Badge oui={produit.capitalGaranti} />
              </td>
              <td>{formaterDuree(produit.dureeMaxMois, DUREE_ILLIMITEE)}</td>
            </tr>
          ))}
        </tbody>
      </Groupe>
      <Groupe id="credit" titre={T.credit.titre} aucun={T.credit.aucun} vide={catalogue.credit.length === 0}>
        <thead>
          <tr>
            <th scope="col">{T.nom}</th>
            <th scope="col">{T.description}</th>
            <th scope="col">{T.taux}</th>
            <th scope="col">{T.dureeMax}</th>
            <th scope="col">{T.credit.ltvMax}</th>
            <th scope="col">{T.credit.assuranceRequise}</th>
          </tr>
        </thead>
        <tbody>
          {catalogue.credit.map((produit) => (
            <tr key={produit.id}>
              <td className="ecran-produits__nom">{produit.nom}</td>
              <td>{valeurOuAbsent(produit.description)}</td>
              <td>{formaterPourcentage(produit.taux)}</td>
              <td>{formaterDuree(produit.dureeMaxMois, ABSENT)}</td>
              <td>{formaterPourcentage(produit.ltvMax)}</td>
              <td>
                <Badge oui={produit.assuranceRequise} />
              </td>
            </tr>
          ))}
        </tbody>
      </Groupe>
      <Groupe
        id="investissement"
        titre={T.investissement.titre}
        aucun={T.investissement.aucun}
        vide={catalogue.investissement.length === 0}
      >
        <thead>
          <tr>
            <th scope="col">{T.nom}</th>
            <th scope="col">{T.description}</th>
            <th scope="col">{T.investissement.secteur}</th>
            <th scope="col">{T.investissement.niveauDeRisque}</th>
            <th scope="col">{T.investissement.rendementAttendu}</th>
          </tr>
        </thead>
        <tbody>
          {catalogue.investissement.map((produit) => (
            <tr key={produit.id}>
              <td className="ecran-produits__nom">{produit.nom}</td>
              <td>{valeurOuAbsent(produit.description)}</td>
              <td>{valeurOuAbsent(produit.secteur)}</td>
              <td>{formaterRisque(produit.niveauRisque)}</td>
              <td>{formaterPourcentage(produit.rendementAttendu)}</td>
            </tr>
          ))}
        </tbody>
      </Groupe>
    </section>
  );
}

interface ProprietesGroupe {
  id: string;
  titre: string;
  aucun: string;
  vide: boolean;
  /** En-tête et corps du tableau, absents si le groupe est vide. */
  children: ReactNode;
}

/** Un type de produit : son titre et son tableau, ou seulement son message « aucun produit » (A-011-1). */
function Groupe({ id, titre, aucun, vide, children }: ProprietesGroupe) {
  const identifiant = `produits-titre-${id}`;
  return (
    <section aria-labelledby={identifiant} data-testid={`produits-${id}`}>
      <Carte>
        <h3 id={identifiant} className="ecran-produits__sous-titre">
          {titre}
        </h3>
        {vide ? (
          <p className="ecran-produits__vide" data-testid={`produits-${id}-vide`}>
            {aucun}
          </p>
        ) : (
          <table className="ecran-produits__tableau" aria-labelledby={identifiant}>
            {children}
          </table>
        )}
      </Carte>
    </section>
  );
}

function Badge({ oui }: { oui: boolean }) {
  return <span className={`ecran-produits__badge ${oui ? 'ecran-produits__badge--oui' : 'ecran-produits__badge--non'}`}>{formaterBooleen(oui)}</span>;
}

function SectionTraitements({ traitements }: { traitements: readonly Traitement[] }) {
  const tr = T.traitements;
  return (
    <section className="ecran-produits__section" aria-labelledby="produits-titre-traitements" data-testid="produits-traitements">
      <p className="ecran-produits__separateur">{tr.separateur}</p>
      <Carte>
        <h2 id="produits-titre-traitements" className="ecran-produits__titre">
          {tr.titre}
        </h2>
        {traitements.length === 0 ? (
          <p className="ecran-produits__vide" data-testid="produits-traitements-vide">
            {tr.aucun}
          </p>
        ) : (
          <table className="ecran-produits__tableau" aria-labelledby="produits-titre-traitements">
            <thead>
              <tr>
                <th scope="col">{tr.job}</th>
                <th scope="col">{tr.debut}</th>
                <th scope="col">{tr.fin}</th>
                <th scope="col">{tr.duree}</th>
                <th scope="col">{tr.enregistrements}</th>
                <th scope="col">{tr.statut}</th>
                <th scope="col">{tr.details}</th>
              </tr>
            </thead>
            <tbody>
              {traitements.map((traitement, rang) => (
                <tr key={rang}>
                  <td className="ecran-produits__nom">{traitement.job}</td>
                  <td>{formaterInstant(traitement.debut)}</td>
                  <td>{formaterInstant(traitement.fin)}</td>
                  <td>{traitement.dureeMs}</td>
                  <td>{formaterNombre(traitement.enregistrements)}</td>
                  <td>
                    <span className={`ecran-produits__badge ${CLASSES_STATUT[traitement.statut ?? ''] ?? ''}`}>{traitement.statut ?? ''}</span>
                  </td>
                  <td>{traitement.details ?? ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Carte>
    </section>
  );
}
