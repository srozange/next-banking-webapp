import { useEffect, useRef, useState, type FormEvent } from 'react';
import { chargerBudget, type ClientBudget } from '../../../application/consulterBudget';
import { enregistrerPlafond, supprimerPlafond, type ClientPlafonds } from '../../../application/gererPlafonds';
import type { Budget } from '../../../domain/budget';
import { CATEGORIES_PLAFONNABLES, FORMULAIRE_PLAFOND_VIDE, type FormulairePlafond, type RefusPlafond } from '../../../domain/plafond';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { AlerteSucces } from '../../ui/AlerteSucces';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonTexte } from '../../ui/BoutonTexte';
import { ChampListe } from '../../ui/ChampListe';
import { ChampTexte } from '../../ui/ChampTexte';
import { Tableau } from '../../ui/Tableau';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import { ListeDeroulante } from '../../ui/ListeDeroulante';
import './EcranBudget.css';

const T = TEXTES.budget;

const OPTIONS_CATEGORIES = CATEGORIES_PLAFONNABLES.map((categorie) => ({ valeur: categorie, libelle: categorie }));
const C = T.colonnes;
const COLONNES = [C.categorie, C.plafond, C.depense, C.ecart, C.action];

const TEXTE_DU_REFUS: Record<RefusPlafond, string> = {
  categorie: T.categorieRequise,
  plafond: T.plafondInvalide,
  plafondTropGrand: T.plafondTropGrand,
};

interface Proprietes {
  client: ClientBudget;
  plafonds: ClientPlafonds;
}

/** Identifiant de test d'une catégorie : son libellé en minuscules. */
const identifiant = (categorie: string) => categorie.toLowerCase();

/**
 * Écran /budget (FEAT-006) : sélecteur de période, trois indicateurs, répartition des dépenses du mois par catégorie.
 * La période suit le choix du sélecteur dès son changement ; toute panne du service garde l'écran sous un texte
 * technique générique. Sous « Plafonds budgetaires » : formulaire de plafond (contrôles locaux avant appel, saisie
 * conservée en cas d'erreur, écran rechargé après un succès) puis tableau des plafonds du mois, suppression sans
 * confirmation. Une erreur technique de plafond n'affiche jamais de détail (A-006-1).
 */
export function EcranBudget({ client, plafonds }: Proprietes) {
  const [contenu, setContenu] = useState<Budget | null>(null);
  const [selection, setSelection] = useState('');
  const [erreur, setErreur] = useState(false);
  const [formulaire, setFormulaire] = useState<FormulairePlafond>(FORMULAIRE_PLAFOND_VIDE);
  const [erreurPlafond, setErreurPlafond] = useState<string | null>(null);
  const [champEnErreur, setChampEnErreur] = useState<'categorie' | 'plafond' | null>(null);
  const [succes, setSucces] = useState<string | null>(null);
  const [occupe, setOccupe] = useState(false);
  const derniereDemande = useRef(0);
  const actionEnvoyee = useRef(false);
  const alerte = useRef<HTMLDivElement>(null);
  const champCategorie = useRef<HTMLSelectElement>(null);
  const champPlafond = useRef<HTMLInputElement>(null);

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

  async function enregistrer(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    if (actionEnvoyee.current || contenu === null) {
      return;
    }
    actionEnvoyee.current = true;
    setOccupe(true);
    const resultat = await enregistrerPlafond(formulaire, contenu.periode, plafonds);
    actionEnvoyee.current = false;
    setOccupe(false);
    setSucces(null);
    if (resultat.statut === 'enregistre') {
      setFormulaire(FORMULAIRE_PLAFOND_VIDE);
      setChampEnErreur(null);
      setErreurPlafond(null);
      setSucces(T.plafondEnregistre(resultat.categorie));
      charger(contenu.periode);
    } else if (resultat.statut === 'invalide') {
      const champ = resultat.refus === 'categorie' ? 'categorie' : 'plafond';
      setChampEnErreur(champ);
      setErreurPlafond(TEXTE_DU_REFUS[resultat.refus]);
      (champ === 'categorie' ? champCategorie : champPlafond).current?.focus();
    } else if (resultat.statut === 'refuse') {
      // Le service ne répond 400 que par un texte de validation ; il désigne le champ par son contenu.
      setChampEnErreur(resultat.message === T.categorieRequise ? 'categorie' : 'plafond');
      setErreurPlafond(resultat.message);
    } else {
      setChampEnErreur(null);
      setErreurPlafond(T.erreurPlafond);
      alerte.current?.focus();
    }
  }

  async function supprimer(id: number, periode: string) {
    if (actionEnvoyee.current) {
      return;
    }
    actionEnvoyee.current = true;
    setOccupe(true);
    const supprime = await supprimerPlafond(id, plafonds);
    actionEnvoyee.current = false;
    setOccupe(false);
    setSucces(null);
    setChampEnErreur(null);
    if (supprime) {
      setErreurPlafond(null);
      charger(periode);
    } else {
      setErreurPlafond(T.erreurPlafond);
      alerte.current?.focus();
    }
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
            <li
              key={ligne.categorie}
              className={ligne.progression?.depasse ? 'ecran-budget__ligne ecran-budget__ligne--depassement' : 'ecran-budget__ligne'}
              data-testid={`budget-ligne-${identifiant(ligne.categorie)}`}
            >
              <span>{ligne.categorie}</span>
              <span className={ligne.progression?.depasse ? 'ecran-budget__montant--depasse' : undefined} data-testid={`budget-montant-${identifiant(ligne.categorie)}`}>
                {ligne.montant}
              </span>
              {ligne.progression === null ? (
                <span className="ecran-budget__progression" data-testid={`budget-aucun-plafond-${identifiant(ligne.categorie)}`}>
                  {T.aucunPlafond}
                </span>
              ) : (
                <span className="ecran-budget__progression">
                  <span
                    className="ecran-budget__barre"
                    role="progressbar"
                    aria-label={ligne.categorie}
                    aria-valuenow={ligne.progression.remplissage}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuetext={T.pourcentage(ligne.progression.pourcentage)}
                    data-testid={`budget-barre-${identifiant(ligne.categorie)}`}
                  >
                    <span
                      className={`ecran-budget__remplissage ecran-budget__remplissage--${ligne.progression.couleur}`}
                      style={{ width: `${ligne.progression.remplissage}%` }}
                    />
                  </span>
                  <span data-testid={`budget-pourcentage-${identifiant(ligne.categorie)}`}>{T.pourcentage(ligne.progression.pourcentage)}</span>
                  {ligne.progression.depasse && (
                    <span className="ecran-budget__badge" data-testid={`budget-depassement-${identifiant(ligne.categorie)}`}>
                      {T.depassement}
                    </span>
                  )}
                </span>
              )}
            </li>
          ))}
        </ul>
      </Carte>
      <h2 className="ecran-budget__titre ecran-budget__separateur">{T.plafonds}</h2>
      <Carte>
        <h3 className="ecran-budget__sous-titre">{T.definirPlafond}</h3>
        {succes !== null && (
          <AlerteSucces testId="budget-message-succes">
            <p>{succes}</p>
          </AlerteSucces>
        )}
        {erreurPlafond !== null && (
          <AlerteErreur ref={alerte} testId="budget-message-erreur-plafond">
            <p>{erreurPlafond}</p>
          </AlerteErreur>
        )}
        <form className="ecran-budget__formulaire" noValidate onSubmit={(evenement) => void enregistrer(evenement)}>
          <ChampListe
            ref={champCategorie}
            id="budget-categorie"
            name="categorie"
            libelle={T.categorie}
            libelleVide={T.choisir}
            options={OPTIONS_CATEGORIES}
            valeur={formulaire.categorie}
            onChange={(valeur) => setFormulaire((precedent) => ({ ...precedent, categorie: valeur }))}
            invalide={champEnErreur === 'categorie'}
            testId="budget-categorie"
          />
          <ChampTexte
            ref={champPlafond}
            id="budget-plafond"
            name="plafond"
            type="text"
            libelle={T.plafondMensuel}
            valeur={formulaire.plafond}
            onChange={(valeur) => setFormulaire((precedent) => ({ ...precedent, plafond: valeur }))}
            placeholder={T.exemplePlafond}
            inputMode="decimal"
            autoComplete="off"
            invalide={champEnErreur === 'plafond'}
            testId="budget-plafond"
          />
          <BoutonPrimaire testId="budget-enregistrer" occupe={occupe || contenu === null}>
            {T.enregistrer}
          </BoutonPrimaire>
        </form>
      </Carte>
      {contenu !== null && contenu.plafonds.length > 0 && (
        <>
          <h3 className="ecran-budget__sous-titre ecran-budget__titre-tableau" data-testid="budget-plafonds-titre">
            {T.plafondsDuMois(contenu.periode)}
          </h3>
          <Carte>
            <Tableau testId="budget-tableau-plafonds" colonnes={COLONNES} libelle={T.plafondsDuMois(contenu.periode)}>
              {contenu.plafonds.map((plafond) => (
                <tr key={plafond.id} data-testid={`budget-plafond-ligne-${plafond.id}`}>
                  <td>{plafond.categorie}</td>
                  <td>{plafond.plafond}</td>
                  <td>{plafond.depense}</td>
                  <td className={plafond.ecartNegatif ? 'ecran-budget__ecart--negatif' : 'ecran-budget__ecart--positif'} data-testid={`budget-ecart-${plafond.id}`}>
                    {plafond.ecart}
                  </td>
                  <td>
                    <BoutonTexte data-testid={`budget-supprimer-${plafond.id}`} desactive={occupe} onClick={() => void supprimer(plafond.id, contenu.periode)}>
                      {T.supprimer}
                    </BoutonTexte>
                  </td>
                </tr>
              ))}
            </Tableau>
          </Carte>
        </>
      )}
    </div>
  );
}
