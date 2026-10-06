import type { ChangeEvent } from 'react';
import './ChampSaisie.css';

interface Proprietes {
  id: string;
  name: string;
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  testId: string;
  /** Clavier proposé sur mobile (nombres décimaux, entiers). */
  modeSaisie?: 'text' | 'decimal' | 'numeric';
}

/** Champ de saisie facultatif avec libellé relié (label for). */
export function ChampSaisie({ id, name, libelle, valeur, onChange, testId, modeSaisie = 'text' }: Proprietes) {
  return (
    <div className="champ-saisie">
      <label className="champ-saisie__libelle" htmlFor={id}>
        {libelle}
      </label>
      <input
        id={id}
        name={name}
        type="text"
        inputMode={modeSaisie}
        className="champ-saisie__saisie"
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLInputElement>) => onChange(evenement.target.value)}
        autoComplete="off"
        data-testid={testId}
      />
    </div>
  );
}
