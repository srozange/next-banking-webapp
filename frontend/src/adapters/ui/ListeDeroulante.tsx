import type { ChangeEvent } from 'react';
import './ListeDeroulante.css';

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  valeur: string;
  options: ReadonlyArray<{ valeur: string; libelle: string }>;
  onChange: (valeur: string) => void;
  testId: string;
  /** Texte placé à côté du libellé, hors du libellé relié (par exemple « (9 valeurs) »). */
  complement?: string;
}

/** Liste déroulante native avec libellé relié (label for) ; le choix est transmis dès qu'il change. */
export function ListeDeroulante({ id, name, libelle, valeur, options, onChange, testId, complement }: Proprietes) {
  return (
    <div className="liste-deroulante">
      <div className="liste-deroulante__entete">
        <label className="liste-deroulante__libelle" htmlFor={id}>
          {libelle}
        </label>
        {complement !== undefined && <span className="liste-deroulante__complement">{complement}</span>}
      </div>
      <select
        id={id}
        name={name}
        className="liste-deroulante__saisie"
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLSelectElement>) => onChange(evenement.target.value)}
        data-testid={testId}
      >
        {options.map((option) => (
          <option key={option.valeur} value={option.valeur}>
            {option.libelle}
          </option>
        ))}
      </select>
    </div>
  );
}
