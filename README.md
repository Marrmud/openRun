# openRun

Une appli de course à pied **libre, gratuite, sans compte ni publicité**, pour Android.
Elle te guide du canapé aux 5 puis 10 km (programmes type Couch to 5K), à ton rythme, depuis le point de départ
que tu choisis, et reste utile ensuite pour courir librement.

> Version actuelle: **0.1.0** (premier jet utilisable). Téléphone seul; la montre Wear OS vient ensuite.

## Ce qu'elle fait

- **Programmes guidés**: Pré-C25K, Du canapé au 5 km, Pont vers 10K, Du canapé au 10 km. Aucune semaine verrouillée:
  tu choisis où commencer, et sauter à une séance valide celles d'avant (après confirmation).
- **Course libre** avec distance, allure et tracé GPS.
- **Écran de course** en trois styles (Focus, Chronologie, Complet), avec intervalle précédent / suivant, pause,
  verrouillage de l'écran, et la possibilité de terminer en avance (enregistrer ou supprimer).
- **Voix, bips et vibrations**, chacun avec son interrupteur. Avec ta musique ou une vidéo en cours, tu choisis:
  *Superposer* (jamais touchée), *Baisser* ou *Pause*. Tout couper ne perturbe jamais ce que tu écoutes.
- **Rangs** (Fer à Diamant) qui montent avec les calories dépensées, un objectif par semaine, et un historique.
- **Health Connect**: chaque séance peut s'écrire dans Health Connect, que Google Health lit.
- **Reprise après plantage**: une séance interrompue est proposée à l'ouverture suivante.

## Vie privée

Pas de compte, pas de serveur, aucune mesure d'audience. Ta position et tes séances restent sur ton téléphone.
Voir [PRIVACY.md](PRIVACY.md).

## Installer

Voir [docs/INSTALL.md](docs/INSTALL.md): télécharger l'APK de la dernière version, l'ouvrir, autoriser l'installation.
Pas de Play Store pour l'instant.

## Construire soi-même

Windows, JDK 21, Android SDK (chemin dans `local.properties`).

```bash
./gradlew :core:test                      # la logique (programmes, progression, calories, rangs, moteur)
./gradlew :app:assembleDebug              # l'appli de debug (s'installe à côté de la vraie: org.openrun.app.debug)
./gradlew :app:connectedDebugAndroidTest  # stockage et réglages, sur un téléphone branché
./gradlew :app:assembleRelease            # l'APK de diffusion, signé si une clé est configurée (voir ci-dessous)
```

Notes de terrain:
- Si Gradle échoue avec « Unable to establish loopback connection »: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\gradle-home\uds` (dossier à créer).
- Tests sur téléphone en adb Wi-Fi: donner à `ANDROID_SERIAL` le nom mDNS (`adb devices`, ligne `adb-…_adb-tls-connect._tcp`), pas `IP:port`. Avec `IP:port`, les tests passent mais Gradle rapporte un échec à tort.
- Signature de la release: variables d'environnement `OPENRUN_KEYSTORE_FILE`, `OPENRUN_KEYSTORE_PASSWORD`, `OPENRUN_KEY_ALIAS`, `OPENRUN_KEY_PASSWORD`, ou un fichier `keystore.properties` à la racine (ignoré par git). **Garde la clé et ses mots de passe à l'abri: sans elle, plus de mise à jour possible pour ceux qui ont installé l'appli.**
- Debug seulement: `adb shell am broadcast -n org.openrun.app.debug/org.openrun.app.debug.DemoSeedReceiver` ajoute trois séances de démonstration; `adb shell pm clear org.openrun.app.debug` efface tout.
- `spike/` est le banc d'essai jetable qui a validé le GPS et l'audio (voir docs/T0-SPIKE.md).

## Où lire quoi

| Fichier | Contenu |
|---|---|
| [docs/BIBLE.md](docs/BIBLE.md) | Vision, décisions (avec leur raison), risques, journal |
| [docs/MVP.md](docs/MVP.md) | Périmètre du MVP et tranches de construction |
| [docs/FEATURES.md](docs/FEATURES.md) | Toutes les idées de fonctionnalités, par palier |
| [docs/DESIGN.md](docs/DESIGN.md) | Principes de design et contraintes |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phases et ordre de construction |
| [CHANGELOG.md](CHANGELOG.md) | Ce qui change à chaque version |
| [mockups/](mockups/index.html) | Maquettes HTML (téléphone et montre) |

## Architecture en deux mots

`core/` est du Kotlin pur, sans Android, avec tout ce qui *décide* (programmes, progression, calories, rangs, moteur de
séance, règles audio) et ses tests. `app/` est l'appli (Jetpack Compose, Material 3, Room, DataStore, Health Connect).

## Licence

GPL-3.0 ou plus. Voir [LICENSE](LICENSE).
