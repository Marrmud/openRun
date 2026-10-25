# T0: spike technique

But: vérifier avant toute UI que la course tient (GPS écran verrouillé) et que l'audio ne casse pas la musique.
Code jetable dans `spike/` (appli « openRun spike », `org.openrun.spike`). Banc d'essai: voix, bips, vibrations,
mode musique, volume de voix, GPS, journal `files/spike.log`.

Séance de test: échauffement 10 s, puis course 20 s / marche 20 s en boucle jusqu'à l'arrêt.

## Résultats déjà mesurés (2026-10-25, Pixel 9 Pro, YouTube ReVanced en incrustation)

| Test | Résultat |
|---|---|
| Compilation (AGP 9.4.1, Gradle 9.8.1, JDK 21) | OK, sans dépendance externe |
| Foreground service `location` + wake lock | Démarre, tient |
| GPS via `LocationManager` (sans Google Play Services) | Points reçus chaque seconde (52 en 1 min 38, en intérieur, précision 60 à 110 m) |
| **Superposer**: voix 3 fois pendant que YouTube joue | **Aucun focus audio demandé**; la pile de focus reste « YouTube seul ». Le moteur vocal ne prend pas de focus de lui-même |
| **Baisser**: voix pendant YouTube | Focus `GAIN_TRANSIENT_MAY_DUCK` empilé pendant la phrase puis abandonné |
| **Pause**: voix pendant YouTube | Focus `GAIN_TRANSIENT` empilé puis abandonné |

Conclusion partielle: la règle de la bible tient (voix coupée ou mode Superposer = zéro focus demandé). Le bug de RunDouble
n'a pas de raison technique d'exister chez nous.

Décision technique prise: **`LocationManager` plutôt que Fused Location** (la bible disait Fused). Pas de dépendance
Google Play Services, donc compatible F-Droid et téléphones sans GMS. À revoir si la précision déçoit.

## Reste à vérifier sur le terrain (tests de l'auteur)

À faire dehors, une vidéo ou une musique qui joue, écran verrouillé, téléphone en poche.

1. **Superposer, voix ON**: la vidéo ne se met jamais en pause, on entend « Course »/« Marche » par-dessus.
2. **Baisser**: la vidéo baisse puis remonte (ou pas du tout si l'appli ne gère pas le ducking: à noter).
3. **Pause**: la vidéo se met en pause pendant l'annonce, puis reprend (ou pas: à noter).
4. **Voix OFF + Bips OFF + Vibrations OFF** (tout coupé): rien ne perturbe la vidéo, aucun focus.
5. **Vibrations écran verrouillé**: vibration longue pour « Course », deux courtes pour « Marche ».
6. **GPS 30 min écran verrouillé**: noter après coup « trou GPS max » et le nombre de points (attendu: environ 1 800).
7. **Volume de la voix**: le curseur change bien la force de la voix par rapport à la musique.

Le journal est lisible dans l'appli (écran de test) ou avec:

```bash
adb shell run-as org.openrun.spike cat files/spike.log
```
