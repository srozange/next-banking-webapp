# FLOW — FEAT-001 Connexion

```mermaid
stateDiagram-v2
    [*] --> Vide
    Vide --> Chargé : GET http://localhost:8080/banking-legacy/login.xhtml
    Chargé --> Erreur : POST loginForm — champs vides
    Chargé --> Erreur : POST loginForm — identifiants incorrects (retro-compte-inexistant / retro-mauvais-mot-de-passe)
    Chargé --> Confirmation : POST loginForm — identifiants valides (rôle CLIENT)
    Chargé --> Confirmation : POST loginForm — identifiants valides (rôle CONSEILLER)
    Chargé --> Confirmation : POST loginForm — identifiants valides (rôle ADMIN)
```

## Captures par état

| État | Screenshot | Rôle | Notes |
|---|---|---|---|
| Chargé | SCR-001-01_vide.png | anonymous | Formulaire affiché avec deux champs vides (Identifiant, Mot de passe) et bouton 'Me connecter'. Page titre : 'BankApp Demo - Connexion'. Accessible sans authentification. redirige toute requête non authentifiée vers cette page. |
| Erreur | SCR-001-02_erreur_validation.png | anonymous | Validation JSF required : messages 'Identifiant requis' et 'Mot de passe requis' affichés dans une liste (classe 'Project Stage[Development]: Unhandled Messages'). La page reste sur login.xhtml. |
| Confirmation | SCR-001-03_confirmation_redirect_dashboard.png | CLIENT | Redirect JSF 'faces-redirect=true' vers /dashboard.xhtml. Source :. |