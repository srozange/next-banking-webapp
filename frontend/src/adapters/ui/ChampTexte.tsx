import type { ChangeEvent, Ref } from 'react';
import './ChampTexte.css';

interface Proprietes {
  id: string;
  name: string;
  type: 'text' | 'password';
  libelle: string;
  valeur: string;
  onChange: (valeur: string) => void;
  testId: string;
  autoComplete?: string;
  /** Message d'erreur affiché sous le champ, relié par aria-describedby. */
  erreur?: string;
  ref?: Ref<HTMLInputElement>;
}

/** Champ obligatoire avec libellé relié (label for) ; l'astérisque est purement visuel (CSS). */
export function ChampTexte({ id, name, type, libelle, valeur, onChange, testId, autoComplete, erreur, ref }: Proprietes) {
  const idErreur = `${id}-erreur`;
  return (
    <div className="champ-texte">
      <label className="champ-texte__libelle" htmlFor={id}>
        {libelle}
      </label>
      <input
        ref={ref}
        id={id}
        name={name}
        type={type}
        className={erreur === undefined ? 'champ-texte__saisie' : 'champ-texte__saisie champ-texte__saisie--erreur'}
        value={valeur}
        onChange={(evenement: ChangeEvent<HTMLInputElement>) => onChange(evenement.target.value)}
        autoComplete={autoComplete}
        aria-required="true"
        aria-invalid={erreur === undefined ? undefined : 'true'}
        aria-describedby={erreur === undefined ? undefined : idErreur}
        data-testid={testId}
      />
      {erreur !== undefined && (
        <p id={idErreur} className="champ-texte__erreur">
          {erreur}
        </p>
      )}
    </div>
  );
}
