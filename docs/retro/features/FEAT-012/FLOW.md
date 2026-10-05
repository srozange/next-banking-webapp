# FLOW — FEAT-012 Comptes Tiers — Open Banking Powens

```mermaid
stateDiagram-v2
    [*] --> Vide
    Vide --> Chargé : GET /banking-legacy/comptes-tiers.xhtml (powensBean.connected=false, aucun compte connecté)
    Chargé --> Chargé : GET /banking-legacy/comptes-tiers.xhtml (rôle ADMIN)
    Chargé --> Chargé : GET /banking-legacy/comptes-tiers.xhtml (rôle CONSEILLER)
    Chargé --> OAuthRedirect : POST Connecter une banque → powensBean.connectBank() → PowensService.initAndGetWebviewUrl() → redirect https://webview.powens.com/en/connect?domain=demi-sandbox.biapi.pro&client_id=99961298&redirect_uri=http://localhost:8080/banking-legacy/powens-callback.xhtml&connector_ids=59
    OAuthRedirect --> Confirmation : Powens Webview redirect → GET /banking-legacy/powens-callback.xhtml?connection_id={id} → powensBean.handleCallback(connectionId) → powensBean.getAccounts(accessToken) → redirect /comptes-tiers
    OAuthRedirect --> Erreur : GET /banking-legacy/powens-callback.xhtml (sans paramètres) → powensBean.setErrorMessage('Callback Powens incomplet. Parametres recus: []') → redirect /comptes-tiers
    OAuthRedirect --> Erreur : GET /banking-legacy/powens-callback.xhtml?error=user_cancelled → powensBean.disconnect() → redirect /comptes-tiers (errorMessage non défini dans ce chemin)
    Confirmation --> Chargé : POST Deconnecter → powensBean.disconnect() via AJAX f:ajax execute render=:mainContent
```

## Captures par état

| État | Screenshot | Rôle | Notes |
|---|---|---|---|
| Chargé | SCR-012-01_charge.png | CLIENT | État initial : bloc non-connecté affiché avec logos banques (BNP Paribas, Crédit Agricole, La Banque Postale, CIC, LCL, +1800) et bouton 'Connecter une banque'. Vide et Chargé sont identiques pour cette feature (aucun compte tiers déjà connecté). |
| OAuthRedirect | SCR-012-02_oauth_powens_webview.png | CLIENT | La méthode initAndGetWebviewUrl() crée un utilisateur anonyme Powens, obtient un auth_token permanent stocké en session (powensBean.accessToken), et retourne l'URL Webview. FacesContext.redirect() navigue vers la Webview Powens externe. connector_ids=59. L'accessToken est déjà disponible en session à ce stade. |
| Confirmation | SCR-012-04_connecte_comptes_vides.png | CLIENT | État connecté avec comptes vides (sandbox). Badge 'Connected via Powens'. Patrimoine total EUR0.00. Mes comptes externes (0). Simultanément : message erreur 'Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.' car getAccounts() retourne liste vide. Thread.sleep(2000) avant getAccounts pour attendre synchro Powens. |
| Erreur | SCR-012-03_erreur.png | CLIENT | Erreur affichée : '⚠️ Callback Powens incomplet. Parametres recus: []'. Bloc non-connecté toujours visible. Survient quand ni connection_id ni code OAuth n'est présent dans les paramètres du callback. |