import type { ChangeEvent, Ref } from 'react';
import './ListeDeroulante.css';

export interface OptionListe {
  valeur: string;
  libelle: string;
}

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  /**
   * Première option, valeur vide : aucun choix. Sa présence rend le choix obligatoire
   * (astérisque visuel, aria-required) ; sans elle, la liste a toujours une valeur choisie.
   */
  libelleVide?: string;
  options: readonly OptionListe[];
  valeur: string;
  onChange: (valeur: string) => void;
  testId: string;
  /** Texte placé à côté du libellé, hors du libellé relié (par exemple « (9 valeurs) »). */
  complement?: string;
  erreur?: string;
  ref?: Ref<HTMLSelectElement>;
}

/** Liste déroulante native, libellé relié (label for), mêmes erreurs que ChampTexte ; le choix est transmis dès qu'il change. */
export function ListeDeroulante({ id, name, libelle, libelleVide, options, valeur, onChange, testId, complement, erreur, ref }: Proprietes) {
  const idErreur = `${id}-erreur`;
  const obligatoire = libelleVide !== undefined;
  return (
    <div className="liste-deroulante">
      <div className="liste-deroulante__entete">
        <label className={obligatoire ? 'liste-deroulante__libelle liste-deroulante__libelle--obligatoire' : 'liste-deroulante__libelle'} htmlFor={id}>
          {libelle}
        </label>
        {complement !== undefined && <span className="liste-deroulante__complement">{complement}</span>}
      </div>
      <select
        ref={ref}
        id={id}
        name={name}
        className={erreur === undefined ? 'liste-deroulante__saisie' : 'liste-deroulante__saisie liste-deroulante__saisie--erreur'}
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLSelectElement>) => onChange(evenement.target.value)}
        aria-required={obligatoire ? 'true' : undefined}
        aria-invalid={erreur === undefined ? undefined : 'true'}
        aria-describedby={erreur === undefined ? undefined : idErreur}
        data-testid={testId}
      >
        {obligatoire && <option value="">{libelleVide}</option>}
        {options.map((option) => (
          <option key={option.valeur} value={option.valeur}>
            {option.libelle}
          </option>
        ))}
      </select>
      {erreur !== undefined && (
        <p id={idErreur} className="liste-deroulante__erreur">
          {erreur}
        </p>
      )}
    </div>
  );
}
