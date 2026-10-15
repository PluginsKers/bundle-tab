# Bundle Tab

Browse your bundles in the vanilla recipe book. The bundle tab appears first and opens with your inventory. Also works at crafting tables, with no server mod required.

Search by item name, ID, or Pinyin, and click a bundle to browse its contents.

| Action | Result |
| --- | --- |
| Drop held items onto the bundle list | Store in the first matching bundle with space |
| Drop held items onto a bundle | Store in that bundle |
| Click an item inside a bundle | Take out the selected stack |
| Click inside a bundle while holding items | Store the held items |
| Shift-click an item inside a bundle | Move it to your inventory |
| Space + left-click an inventory item | Store it in the selected bundle, or the first available bundle |

Items that do not fit stay where they are. Mouse Tweaks gestures inside the bundle panel are not supported.

## Installation

Requires Minecraft 26.3.x, Fabric Loader 0.19.5 or newer, and Java 25.
Place the mod jar in your client's `mods` folder. Remove older Better Bundle or Bundle Tab jars first; the mod ID remains `better-bundle`.

## Building

```sh
./gradlew build
./gradlew runClientGameTest
```

Use `gradlew.bat` on Windows. Builds require JDK 25; game tests also require a display. Jars are written to `build/libs`.
Game tests include Mouse Tweaks by default; use `-PmouseTweaksTest=false` to run without it.

## Credits and license

Based on [BetterBundleHUDMod](https://github.com/zeroowo24/BetterBundleHUDMod) by zeroowo24. Maintained by PluginsKers under [CC0-1.0](LICENSE). Bundled pinyin4j retains its own license.
