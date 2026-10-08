import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useNavigate, useOutletContext } from 'react-router';
import { ROUTE_CONNEXION } from '../../../domain/connexion';
import { prenomDe, saisieVide, type Echange } from '../../../domain/conversation';
import type { ResultatProfil } from '../../../application/chargerProfil';
import { dialoguer, type ClientConversation } from '../../../application/dialoguerAvecSophie';
import type { SessionLocale } from '../../../application/session';
import { TEXTES } from '../../textes';
import { BoutonFantome } from '../../ui/BoutonFantome';
import { BoutonPrimaire } from '../../ui/BoutonPrimaire';
import { BoutonSecondaire } from '../../ui/BoutonSecondaire';
import { Carte } from '../../ui/Carte';
import './EcranConseiller.css';

const T = TEXTES.conseiller;

interface Proprietes {
  client: ClientConversation;
  session: SessionLocale;
}

/**
 * Écran /mon-conseiller (FEAT-010), identique pour CLIENT, CONSEILLER et ADMIN : conversation avec Sophie tenue par
 * le front (aucun état serveur, aucune persistance). Le profil vient du gabarit (`Outlet context`, null tant qu'il
 * se charge) ; rien n'est affiché avant sa résolution. Une panne est une bulle de Sophie. Les actions rapides ne
 * sont visibles que sur une conversation neuve ; « Nouvelle conversation » ignore une réponse encore en vol.
 */
export function EcranConseiller({ client, session }: Proprietes) {
  const naviguer = useNavigate();
  const profil = useOutletContext<ResultatProfil | null>();
  const [echanges, setEchanges] = useState<readonly Echange[]>([]);
  const [saisie, setSaisie] = useState('');
  const [occupe, setOccupe] = useState(false);
  /** Numéro de la conversation en cours : une réponse d'une conversation effacée est ignorée. */
  const conversation = useRef(0);
  const actif = useRef(true);
  const liste = useRef<HTMLDivElement>(null);

  useEffect(() => {
    actif.current = true;
    return () => {
      actif.current = false;
    };
  }, []);

  // Défilement jusqu'au dernier message.
  useEffect(() => {
    if (liste.current !== null) {
      liste.current.scrollTop = liste.current.scrollHeight;
    }
  }, [echanges, occupe, profil]);

  async function envoyer(message: string) {
    if (saisieVide(message) || occupe) {
      return;
    }
    const historique = echanges;
    const numero = conversation.current;
    setEchanges([...historique, { role: 'utilisateur', contenu: message }]);
    setOccupe(true);
    const resultat = await dialoguer(client, session, historique, message, T.indisponible);
    if (!actif.current || numero !== conversation.current) {
      return;
    }
    if (resultat.type === 'deconnecte') {
      void naviguer(ROUTE_CONNEXION, { replace: true });
      return;
    }
    setEchanges((courants) => [...courants, { role: 'assistant', contenu: resultat.texte }]);
    setOccupe(false);
  }

  function nouvelleConversation() {
    conversation.current += 1;
    setEchanges([]);
    setOccupe(false);
  }

  function surSoumission(evenement: FormEvent) {
    evenement.preventDefault();
    void envoyer(saisie);
  }

  function surTouche(evenement: KeyboardEvent<HTMLTextAreaElement>) {
    if (evenement.key === 'Enter' && !evenement.shiftKey) {
      evenement.preventDefault();
      void envoyer(saisie);
    }
  }

  const resolu = profil !== null && profil.type !== 'deconnecte';
  const prenom = profil?.type === 'profil' ? prenomDe(profil.nomComplet) : null;

  return (
    <div className="ecran-conseiller" aria-busy={profil === null ? 'true' : undefined} data-testid="chat-contenu">
      <title>{TEXTES.titresOnglet.monConseiller}</title>
      {resolu && (
        <Carte>
          <div className="ecran-conseiller__carte">
            <div className="ecran-conseiller__entete">
              <div>
                <h2 className="ecran-conseiller__nom">{T.nom}</h2>
                <p className="ecran-conseiller__statut">{T.statut}</p>
              </div>
              <BoutonSecondaire data-testid="chat-nouvelle-conversation" onClick={nouvelleConversation}>
                {T.nouvelleConversation}
              </BoutonSecondaire>
            </div>
            <div ref={liste} className="ecran-conseiller__messages" aria-live="polite" data-testid="chat-messages">
              <Bulle role="assistant" contenu={T.bienvenue(prenom)} />
              {echanges.map((echange, index) => (
                <Bulle key={index} role={echange.role} contenu={echange.contenu} />
              ))}
            </div>
            {echanges.length === 0 && (
              <div className="ecran-conseiller__actions">
                {T.actions.map((action) => (
                  <BoutonFantome
                    key={action.cle}
                    data-testid={`chat-action-${action.cle}`}
                    onClick={() => void envoyer(action.message)}
                  >
                    {action.libelle}
                  </BoutonFantome>
                ))}
              </div>
            )}
            <form className="ecran-conseiller__saisie" onSubmit={surSoumission}>
              <textarea
                className="ecran-conseiller__champ"
                rows={2}
                placeholder={T.placeholder}
                aria-label={T.placeholder}
                value={saisie}
                onChange={(evenement) => setSaisie(evenement.target.value)}
                onKeyDown={surTouche}
                data-testid="chat-saisie"
              />
              <div className="ecran-conseiller__envoyer">
                <BoutonPrimaire testId="chat-envoyer" occupe={occupe}>
                  {occupe ? T.envoiEnCours : T.envoyer}
                </BoutonPrimaire>
              </div>
            </form>
          </div>
        </Carte>
      )}
    </div>
  );
}

function Bulle({ role, contenu }: Echange) {
  const sophie = role === 'assistant';
  return (
    <div
      className={sophie ? 'ecran-conseiller__bulle' : 'ecran-conseiller__bulle ecran-conseiller__bulle--client'}
      data-testid={sophie ? 'chat-message-sophie' : 'chat-message-utilisateur'}
    >
      <Carte attenuee={sophie}>
        <p className="ecran-conseiller__texte">{contenu}</p>
      </Carte>
    </div>
  );
}
