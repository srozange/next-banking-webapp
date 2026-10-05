# FIELDS — FEAT-013 Gestion des bénéficiaires externes

Source : accessibility snapshots MCP Playwright (retro-ui, 2026-10-03).
Rôle de référence : CLIENT (user1). Vérifiés : CONSEILLER (sophie), ADMIN (admin).

---

## Formulaire « Nouveau bénéficiaire SEPA »

| Champ | Type | Label affiché | Obligatoire | Valeur par défaut | Notes |
|---|---|---|---|---|---|
| libelle | textbox | Libelle * | oui | — | Message si vide : « Le libellé est obligatoire. » (JSF h:messages) |
| nomTitulaire | textbox | Nom du titulaire * | oui | — | Nom du titulaire du compte bénéficiaire |
| iban | textbox | IBAN * | oui | — | Validé MOD-97 par le bouton « Verifier l'IBAN » (voir activations dynamiques). Affiché masqué en lecture (ex. FR76***0189) |
| bic | textbox | BIC (optionnel) | non | — | BIC SEPA facultatif |
| banque | textbox | Banque (optionnel) | non | — | Nom de la banque facultatif |

### Boutons du formulaire

| Bouton | Action | Notes |
|---|---|---|
| Verifier l'IBAN | Validation IBAN inline (Ajax) | Affiche message sous le champ (voir Activations dynamiques) |
| Ajouter le beneficiaire | Soumission du formulaire (POST) | Déclenche la validation JSF puis l'ajout en base |

---

## Tableau « Mes bénéficiaires enregistrés » (lecture seule)

| Colonne | Label affiché | Type | Notes |
|---|---|---|---|
| libelle | Libelle | texte | Libellé choisi par l'utilisateur |
| titulaire | Titulaire | texte | Nom du titulaire saisi |
| iban | IBAN | texte | Masqué à l'affichage : FR76***XXXX (4 derniers chiffres visibles) |
| bic | BIC | texte | « — » si non renseigné |
| banque | Banque | texte | « — » si non renseignée |
| zoneSepa | Zone SEPA | calculé | Auto-détectée depuis le code pays IBAN. Affiché « ✓ SEPA » |
| action | Action | bouton | « Supprimer » — suppression immédiate sans confirmation modale |

---

## Activations dynamiques

| Élément | Condition de déclenchement | Message(s) |
|---|---|---|
| Message de validation IBAN | Clic sur « Verifier l'IBAN » | IBAN valide : « IBAN valide — {pays} (Zone SEPA) » · IBAN invalide : « IBAN invalide (echec MOD-97). Verifiez la saisie. » |
| Message de succès | Soumission réussie (« Ajouter le beneficiaire ») | « Bénéficiaire ajouté avec succès. » (JSF h:messages, persiste jusqu'à prochaine action) |
| Message d'erreur obligatoire | Soumission avec champ obligatoire vide | « Le libellé est obligatoire. » (JSF h:messages) |

---

## Comportement par rôle

| Rôle | Accès à la page | Liste | Formulaire | Notes |
|---|---|---|---|---|
| CLIENT | oui | Propre liste (per-user) | Accessible | user1 avait 3 bénéficiaires initialement |
| CONSEILLER | oui | Propre liste (vide pour sophie) | Accessible | Message « Aucun beneficiaire enregistre. » si liste vide |
| ADMIN | oui | Propre liste (vide pour admin) | Accessible | Même comportement que CONSEILLER |

Pas de « droits_réduits » détecté : la page est identique pour tous les rôles authentifiés.

---

## Dettes legacy observées

| ID | Observation |
|---|---|
| DL-013-01 | Compteur « Beneficiaires (N) » dans le titre du tableau affiche N-1 après un ajout (valeur stale JSF — vue non rafraîchie avant rendu) |
| DL-013-02 | Bouton « Supprimer » supprime immédiatement sans confirmation modale (risque de suppression accidentelle) |
| DL-013-03 | L'erreur IBAN persiste dans le champ après rechargement de page (JSF conserve la valeur du view state dans le champ IBAN) |
