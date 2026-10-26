# Design

Les sources du design d'openRun, telles que livrées (voir [docs/DESIGN.md](../docs/DESIGN.md) pour les principes).

| Dossier | Contenu |
|---|---|
| [`logo/`](logo) | Le pack de logos exporté de l'outil de design: SVG de la mascotte (`logo/logo/head.svg`, `logo/logo/runner_*.svg`), icône complète, icônes Android (fond, premier plan, monochrome), icône iOS « liquid glass » (SVG et PNG 1024), icône Play Store (PNG 512) |
| [`brand-sheet/`](brand-sheet) | La planche de marque (HTML): icône principale, mot-symbole « openRun » dont le **o** est la mascotte coureuse (versions claire et verte), variantes de l'icône (menthe, jaune, sombre, thématique), icône iOS 26 en liquid glass, palette |

## Identité

- **Mascotte**: un anneau qui court, avec des yeux, et un soleil jaune.
- **Police**: Fredoka (600 pour le mot-symbole), libre sous licence OFL.
- **Palette**: `#00695C` vert · `#B2DFD8` menthe · `#0B2A25` encre · `#FFC93C` jaune · `#FF7A9A` rose.

## Où c'est utilisé

- Icône de l'appli: `app/src/main/res/drawable/ic_launcher_*.xml` (convertis depuis `logo/android/*.svg`).
- Bannière du README: `docs/assets/banner.png`, générée depuis `docs/assets/banner.html`.
- iOS et mode sombre: prêts dans le pack, pour quand ces cibles existeront.

Pour voir la planche de marque: servir le dossier (`python -m http.server`) et ouvrir `brand-sheet/Buddy.dc.html`.
