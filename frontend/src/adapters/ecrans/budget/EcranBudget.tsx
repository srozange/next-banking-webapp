import { useEffect, useRef, useState } from 'react';
import { chargerBudget, type ClientBudget } from '../../../application/consulterBudget';
import type { Budget } from '../../../domain/budget';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { ListeDeroulante } from '../../ui/ListeDeroulante';
import './EcranBudget.css';

const T = TEXTES.budget;

interface Proprietes {
  client: ClientBudget;
}

/** Identifiant de test d'une catégorie : son libellé en minuscules. */
const identifiant = (categorie: string) => categorie.toLowerCase();

/**
 * Écran /budget (FEAT-006) : sélecteur de période, trois indicateurs, répartition des dépenses du mois par catégorie.
 * La période suit le choix du sélecteur dès son changement ; toute panne du service garde l'écran sous un texte
 * technique générique.
 */
export function EcranBudget({ client }: Proprietes) {
  const [contenu, setContenu] = useState<Budget | null>(null);
  const [selection, setSelection] = useState('');
  const [erreur, setErreur] = useState(false);
  const derniereDemande = useRef(0);

  function charger(periode: string | null) {
    const demande = ++derniereDemande.current;
    void chargerBudget(periode, client).then((lu) => {
      if (demande !== derniereDemande.current) {
        return;
      }
      setErreur(lu === null);
      if (lu !== null) {
        setContenu(lu);
        setSelection(lu.periode);
      }
    });
  }

  useEffect(() => {
    charger(null);
    return () => {
      derniereDemande.current++;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- chargement initial par client, sans dépendre de `charger`
  }, [client]);

  function changer(valeur: string) {
    setSelection(valeur);
    charger(valeur);
  }

  const options = (contenu?.periodes ?? []).map((periode) => ({ valeur: periode, libelle: periode }));

  return (
    <div className="ecran-budget">
      <title>{TEXTES.titresOnglet.budget}</title>
      <div className="ecran-budget__periode">
        <ListeDeroulante id="budget-periode" name="periode" libelle={T.periode} options={options} valeur={selection} onChange={changer} testId="budget-periode" />
        <BoutonSecondaire data-testid="budget-afficher" onClick={() => charger(selection === '' ? null : selection)}>
          {T.afficher}
        </BoutonSecondaire>
      </div>
      {erreur && (
        <AlerteErreur testId="budget-message-erreur">
          <p>{TEXTES.erreurTechnique}</p>
        </AlerteErreur>
      )}
      <div className="ecran-budget__cartes">
        <Carte>
          <h2 className="ecran-budget__titre ecran-budget__titre--depenses">{T.totalDepenses}</h2>
          <p className="ecran-budget__valeur ecran-budget__valeur--depenses" data-testid="budget-total-depenses">
            {contenu?.totalDepenses}
          </p>
        </Carte>
        <Carte>
          <h2 className="ecran-budget__titre ecran-budget__titre--revenus">{T.totalRevenus}</h2>
          <p className="ecran-budget__valeur ecran-budget__valeur--revenus" data-testid="budget-total-revenus">
            {contenu?.totalRevenus}
          </p>
        </Carte>
        <Carte>
          <h2 className="ecran-budget__titre-carte">{T.plafondsDefinis}</h2>
          <p className="ecran-budget__valeur ecran-budget__valeur--plafonds" data-testid="budget-plafonds-definis">
            {contenu?.plafondsDefinis}
          </p>
        </Carte>
      </div>
      <h2 className="ecran-budget__titre ecran-budget__separateur">{T.repartition}</h2>
      <Carte>
        <h3 className="ecran-budget__sous-titre" data-testid="budget-repartition-titre">
          {contenu !== null && T.depensesDuMois(contenu.periode)}
        </h3>
        {contenu !== null && contenu.repartition.length === 0 && <p data-testid="budget-aucune-depense">{T.aucuneDepense}</p>}
        <ul className="ecran-budget__repartition">
          {(contenu?.repartition ?? []).map((ligne) => (
            <li key={ligne.categorie} className="ecran-budget__ligne" data-testid={`budget-ligne-${identifiant(ligne.categorie)}`}>
              <span>{ligne.categorie}</span>
              <span data-testid={`budget-montant-${identifiant(ligne.categorie)}`}>{ligne.montant}</span>
            </li>
          ))}
        </ul>
      </Carte>
      <h2 className="ecran-budget__titre ecran-budget__separateur">{T.plafonds}</h2>
    </div>
  );
}
