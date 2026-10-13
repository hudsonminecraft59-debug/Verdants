# Verdant (Fabric, Minecraft 1.21.11)

Client-side Fabric mod with a green ClickGUI, **Storage ESP** and **Spawn Finder**.
Open the GUI with **Right Shift** (rebindable in Controls). Settings are saved to `config/verdant.json`.

## Build the JAR

Requires JDK 21 and internet access (Gradle downloads Minecraft + Fabric).

    gradle wrapper --gradle-version 8.14.3     # once, creates ./gradlew
    ./gradlew build

The mod JAR is `build/libs/verdant-1.0.0.jar` (ignore the `-sources` jar).
Drop it in `.minecraft/mods` together with Fabric API 0.141.x for 1.21.11.

No local setup? Push this folder to GitHub: `.github/workflows/build.yml` builds it and
uploads the JAR as a workflow artifact.

## Modules
- **Storage ESP**: chests, trapped chests, ender chests, barrels, hoppers, shulker boxes (dye colors optional).
- **Spawn Finder**: mob spawners, trial spawners, nearest highlight, optional 16-block activation range.
- **Interface**: accent color, panel width, corner radius, backdrop dim, animation speed, footer.

Both ESP modules share: render mode (outline / fill / both), outline style (solid / corners), width,
screen-constant width, per-type colors with HSB + alpha + rainbow, custom/rainbow outline and fill colors,
opacities, fill gradient, expand, through-walls, range, distance fade, pulse.
