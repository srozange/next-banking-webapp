import { useEffect, useRef, useState } from 'react';
import { chargerReleve, type ClientReleve, type Releve } from '../../../application/consulterReleve';
import type { ClientVirement } from '../../../application/virement';
import { eurosDepuisChaine } from '../../../domain/releve';
import { TEXTES } from '../../textes';
import { AlerteErreur } from '../../ui/AlerteErreur';
import { Carte } from '../../ui/Carte';
import { Etiquette } from '../../ui/Etiquette';
import { ListeDeroulante } from '../../ui/ListeDeroulante';
import { formaterDate } from './formaterDate';
import './EcranReleve.css';

const T = TEXTES.releve;

interface Proprietes {
  /** Comptes du client (soldes) : rechargés à chaque changement de compte. */
  comptes: ClientVirement;
  releve: ClientReleve;
}

/**
 * Écran /releve (FEAT-005) : sélecteur des comptes du client, informations du compte, tableau de toutes ses
 * opérations. Toute erreur du service reste sur l'écran sous un texte technique, valeurs conservées.
 */
export function EcranReleve({ comptes, releve }: Proprietes) {
  const [contenu, setContenu] = useState<Releve>({ comptes: [], compteId: null, lignes: [] });
  const [selection, setSelection] = useState('');
  const [erreur, setErreur] = useState(false);
  const derniereDemande = useRef(0);

  function charger(compteId: number | null) {
    const demande = ++derniereDemande.current;
    void chargerReleve(compteId, comptes, releve).then((lu) => {
      if (demande !== derniereDemande.current) {
        return;
      }
      setErreur(lu === null);
      if (lu !== null) {
        setContenu(lu);
        setSelection(lu.compteId === null ? '' : String(lu.compteId));
      }
    });
  }

  useEffect(() => {
    charger(null);
    return () => {
      derniereDemande.current++;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- chargement initial par clients, sans dépendre de `charger`
  }, [comptes, releve]);

  function changer(valeur: string) {
    setSelection(valeur);
    charger(Number(valeur));
  }

  const compte = contenu.comptes.find((candidat) => String(candidat.id) === selection);
  const options = contenu.comptes.map((candidat) => ({ valeur: String(candidat.id), libelle: `${candidat.type} ${candidat.numero}` }));

  return (
    <div className="ecran-releve">
      <title>{TEXTES.titresOnglet.releve}</title>
      <Carte>
        <h1 className="ecran-releve__titre">{T.titre}</h1>
        <ListeDeroulante id="releve-compte" name="compte" libelle={T.compte} options={options} valeur={selection} onChange={changer} testId="releve-compte" />
        {erreur && (
          <AlerteErreur testId="releve-message-erreur">
            <p>{TEXTES.erreurTechnique}</p>
          </AlerteErreur>
        )}
        {compte !== undefined && (
          <p className="ecran-releve__informations" data-testid="releve-informations">
            {T.informations(compte.type, compte.numero, eurosDepuisChaine(compte.solde))}
          </p>
        )}
        <p className="ecran-releve__compteur" aria-live="polite" data-testid="releve-compteur">
          {contenu.lignes.length > 0 && T.compteur(contenu.lignes.length)}
        </p>
        <table className="ecran-releve__tableau" data-testid="releve-tableau">
          <thead>
            <tr>
              <th scope="col">{T.colonnes.date}</th>
              <th scope="col">{T.colonnes.description}</th>
              <th scope="col">{T.colonnes.type}</th>
              <th scope="col" className="ecran-releve__nombre">
                {T.colonnes.montant}
              </th>
              <th scope="col" className="ecran-releve__nombre">
                {T.colonnes.soldeApres}
              </th>
            </tr>
          </thead>
          <tbody>
            {contenu.lignes.length === 0 ? (
              <tr>
                <td colSpan={5} data-testid="releve-aucune-transaction">
                  {T.aucuneTransaction}
                </td>
              </tr>
            ) : (
              contenu.lignes.map((ligne, index) => (
                <tr key={index} className={ligne.enAttente ? 'ecran-releve__ligne--attente' : undefined} data-testid={`releve-ligne-${index}`}>
                  <td>{formaterDate(ligne.date)}</td>
                  <td>
                    {ligne.enAttente && <Etiquette data-testid={`releve-en-attente-${index}`}>{T.enAttente}</Etiquette>}
                    {ligne.enAttente && ' '}
                    {ligne.libelle}
                  </td>
                  <td className={`ecran-releve__type ecran-releve__type--${ligne.type === 'CREDIT' ? 'credit' : 'debit'}`} data-testid={`releve-type-${index}`}>
                    {ligne.type}
                  </td>
                  <td className={`ecran-releve__nombre ecran-releve__type--${ligne.type === 'CREDIT' ? 'credit' : 'debit'}`} data-testid={`releve-montant-${index}`}>
                    {ligne.montant}
                  </td>
                  <td className="ecran-releve__nombre" data-testid={`releve-solde-apres-${index}`}>
                    {ligne.soldeApres}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </Carte>
    </div>
  );
}
