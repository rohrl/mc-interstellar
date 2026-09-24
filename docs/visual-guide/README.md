# Illustrated rendering guide

Open **[interstellar-visual-guide.html](interstellar-visual-guide.html)** in a browser.
It is a self-contained, offline artifact: 18 chapters, about 9,200 words, 20 vector
diagrams (including interactive ray and AA examples), and three actual development
images. The text covers code snapshot `fb0c827`; later benchmark evidence is linked
separately. External links provide references, not runtime dependencies.

Rebuild from the repository root with Node 18 or newer:

```sh
node docs/visual-guide/build.mjs
```

No package installation is needed. Edit `chapters.html`, `diagrams.mjs`, `style.css`
and `interactions.js`; the builder embeds diagrams, scripts, styles and PNG assets.
Keep the generated HTML committed so readers do not need build tools. Browser Print
uses a separate print layout and expands optional explanations.
The local `.ignore` keeps the generated image payload out of default ripgrep searches;
search the authoring files, or name the generated file explicitly when necessary.

The interactive orbit illustration is an independent educational RK4 integration,
not a production GPU test. Most drawings are explicitly schematic. The native-world
image is a historical 22 September checkpoint; the interior and body images are
from the horizon/body study. They are not new benchmark captures.

QA: rendered in Edge at desktop and mobile widths, inspected the cover, diagrams
and embedded imagery; checked internal/source-file links, SVG text bounds, image
loading and page errors. Exercised captured, near-critical and escaping ray presets,
and the AA coverage control. No production renderer code changes are involved.
