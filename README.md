# openRun (nom de travail)

Appli de course à pied **gratuite, open source, sans compte, sans pub**, pour Android et Wear OS.
Pensée pour qu'un débutant complet démarre en 10 secondes (Couch to 5K / 10K), mais qui reste
assez profonde pour un coureur avancé (séances structurées, calendrier, plans maison).

> Statut: **cadrage + maquettes.** Aucun code pour l'instant.

## Où lire quoi

| Fichier | Contenu |
|---|---|
| [docs/BIBLE.md](docs/BIBLE.md) | Vision, décisions actées (avec leur raison), risques et défis |
| [docs/FEATURES.md](docs/FEATURES.md) | Toutes les idées de fonctionnalités, triées par palier |
| [docs/DESIGN.md](docs/DESIGN.md) | Principes de design, contraintes, brief pour Claude Design |
| [docs/MVP.md](docs/MVP.md) | Périmètre du MVP, tranches de construction, critères de réussite |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phases et ordre de construction |
| [mockups/index.html](mockups/index.html) | Maquette interactive téléphone (à ouvrir dans un navigateur) |
| [mockups/watch.html](mockups/watch.html) | Maquette interactive montre Wear OS |

## Développement (Windows, JDK 21, Android SDK dans `local.properties`)

```bash
./gradlew :core:test                      # logique pure (programmes, progression, calories, rangs, moteur)
./gradlew :app:assembleDebug              # l'appli
./gradlew :app:connectedDebugAndroidTest  # stockage et réglages, sur un téléphone branché
```

Notes de terrain:
- Si Gradle échoue avec « Unable to establish loopback connection »: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\gradle-home\uds` (dossier à créer).
- Tests sur téléphone en adb Wi-Fi: donner à `ANDROID_SERIAL` le nom mDNS (`adb devices`, ligne `adb-…_adb-tls-connect._tcp`), pas `IP:port`. Avec `IP:port`, les 16 tests passent mais Gradle rapporte un échec à tort.
- Build debug seulement: `adb shell am broadcast -n org.openrun.app/.debug.DemoSeedReceiver` ajoute trois séances de démonstration (avec tracé, tours et données de montre simulées) pour voir l'historique sans courir. Pour tout effacer: `adb shell pm clear org.openrun.app`.
- `spike/` est le banc d'essai jetable de T0 (voir docs/T0-SPIKE.md).

## Licence

Prévue: GPL-3.0-or-later (à confirmer, voir BIBLE.md).
