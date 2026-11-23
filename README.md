# FocusGuard

Pour les applis cochées :
- **Pause 10 s** : un écran bloque l'appli à son ouverture. « Continuer » ne devient cliquable qu'après 10 s ; « Retour » renvoie à l'accueil.
- **Scroll bloqué** : tout scroll dans l'appli (après 1,5 s d'ouverture) renvoie immédiatement à l'accueil.

## Obtenir l'APK sans Android Studio
Le dépôt compile l'APK tout seul via GitHub Actions. Après chaque push, l'APK est publié dans
l'onglet **Releases** (release « latest », fichier `FocusGuard.apk`) : ouvre-la depuis le navigateur
du téléphone et installe le fichier. Pour relancer un build à la main : onglet Actions > Build APK > Run workflow.

(Avec Android Studio : ouvrir le dossier, puis Run ou Build > Build APK(s).)

Pour mettre à jour une version déjà installée, désinstalle l'ancienne d'abord (la signature de debug change à chaque build).

## Activer sur Samsung (One UI)
1. Réglages > Applications > FocusGuard > ⋮ > **Autoriser les paramètres restreints** (Android 13+, appli installée hors Play Store).
2. Réglages > Accessibilité > Applications installées > FocusGuard > activer.
3. Réglages > Batterie : mettre FocusGuard sur « Non restreinte » et le retirer des « Applications en veille ».

## Réglages dans le code
- `Prefs.WAIT_SECONDS` : durée de l'attente (10).
- `GuardService.SCROLL_GRACE_MS` : délai d'ignorance des scrolls après ouverture (1500 ms).
- `GuardService.ALWAYS_IGNORED` : fenêtres système ignorées.
