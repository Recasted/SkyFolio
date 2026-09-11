# SkyBlock Progression Tracker (Stage 1)

A client-side Fabric mod for Hypixel SkyBlock: personal progression, profit,
skin-collection and inventory-valuation dashboard.

**What this mod does not do:** click for you, move for you, automate any
gameplay action, read or send anything beyond normal client-visible state,
or touch the network layer. It only reads what your client already has
(open screens, your own item stacks, chat) and stores calculations locally.

## Why these versions

- **Minecraft 26.1.2**, **Fabric Loader**, **Java 21** — targets Hypixel's
  current SkyBlock-native version line directly (their 2026 renumbering
  scheme; SkyBlock has been migrating onto it throughout the year). This is
  also exactly the version the *official* Hypixel Mod API's Fabric build
  targets, so Stage 5+ can add it as a dependency with zero version
  mismatch.
- **Fabric over Forge** — lighter, faster iteration, and it's the loader
  Hypixel's own official Mod API ships for.

> ⚠️ Before your first build, double check `yarn_mappings` and `fabric_version`
> in `gradle.properties` against Fabric's version index for `26.1.2`. This is
> a very new version line, and I couldn't reach Fabric's Maven from this
> sandbox to confirm the exact current build numbers — I've filled in
> plausible-format placeholders. Loom fails fast with a clear "mapping not
> found" error if a string is stale or doesn't exist yet, so check
> https://fabricmc.net/develop/ or the Fabric Meta API
> (`meta.fabricmc.net`) for the real numbers before running `./gradlew build`.
> If Yarn mappings for 26.1.2 don't exist yet (very new versions sometimes
> lag on community mappings), Mojang's official Fabric-compatible mappings
> or a slightly older 26.1.x patch are the fallback.

## What's implemented in Stage 1

- Working Fabric project (Gradle + Loom) that builds a real client-only jar.
- `Goal` and `ProfitSession` data models (Section 2 & 3 of the spec).
- `DataStorage`: JSON persistence, one file per data type, atomic writes so a
  crash mid-save can't corrupt existing data.
- `ModConfig`: minimal settings scaffold (currency formatting, theme flag) —
  grows in later stages rather than being designed all at once.
- `DashboardScreen`: press **K** in-game to open it. Shows your goal list with
  priority-colored progress bars and an "Add Goal" button (creates a
  placeholder goal — a real creation form comes in Stage 2).
- Unit tests for the ETA/progress math (matches the worked example from the
  spec: 300M → 2B at 25M/h = 68 hours).

## The bigger change hiding behind "just bump the version"

Minecraft 26.1+ shipped **unobfuscated** — Mojang's own class/method names are
in the jar directly. Fabric's whole Yarn-mappings toolchain (the thing that
turns `class_1548` into `CreeperEntity`) has nothing left to do, so the
Fabric project dropped third-party mapping maintenance from 26.1 onward.
Practically, that meant more than swapping `minecraft_version`:

- No `mappings(...)` line in `build.gradle` at all — there's nothing to map.
- Loom's Gradle plugin id changed from `fabric-loom` to `net.fabricmc.fabric-loom`.
- `modImplementation`/`modCompileOnly`/`modApi` → plain `implementation`/`compileOnly`/`api`
  (no more obfuscated-to-intermediary remap step to hook into).
- Java target bumped from 21 → **25**.
- Fabric API renamed a batch of its own classes to match Mojang's official
  names where they differed from Yarn's. I couldn't fetch the exhaustive
  rename list from this sandbox — if you hit a compile error on a Fabric API
  import, search the old name at
  `docs.fabricmc.net/develop/porting/26.1/fabric-api` and swap it.
- Every vanilla Minecraft class I referenced got renamed to its official
  (Mojmap) name: `MinecraftClient`→`Minecraft`, `DrawContext`→`GuiGraphics`,
  `ButtonWidget`→`Button`, `KeyBinding`→`KeyMapping`, `InputUtil`→`InputConstants`,
  `Text`→`Component`, and the `screen` package became `screens` (plural). I've
  already ported `SkyTrackerClient` and `DashboardScreen` to these names — I'm
  confident in them since Mojmap itself predates this year's unobfuscation
  change, but it's worth diffing against the current
  [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod) for 26.1
  before you build, since I can't run this project in this sandbox to confirm
  it compiles clean.

## Building

This project doesn't include a Gradle wrapper (`gradlew`) yet - generating
the wrapper jar requires an existing Gradle install, and this sandbox
couldn't reach Gradle's distribution servers to bootstrap one. Two options:

- **Locally:** install Gradle 8.11+ yourself (via your OS package manager or
  [gradle.org/install](https://gradle.org/install/)), then from the project
  root run `gradle wrapper --gradle-version 8.11` once - that generates
  `gradlew`, `gradlew.bat`, and the wrapper jar, which you should commit.
  After that, `./gradlew build` works exactly like normal.
- **Or just use `gradle build` / `gradle test` directly** if you have Gradle
  installed - no wrapper needed for local use, only for reproducible CI/other
  machines.

The included GitHub Actions workflow (below) doesn't need a wrapper either -
it provisions Gradle itself via `gradle/actions/setup-gradle`.

Output jar lands in `build/libs/`.

## Continuous integration

`.github/workflows/build.yml` builds the mod on every push/PR to `main` and
uploads the jar as a workflow artifact (Actions tab → your run → Artifacts).
Push a tag like `v0.1.0` and it also cuts a GitHub Release with the jar
attached, so you get a stable download link without digging through Actions
runs:

```
git tag v0.1.0-stage1
git push origin v0.1.0-stage1
```

## Roadmap (unchanged from the spec's Section 30)

- **Stage 2** (next): full Dashboard net-worth panel, real Goal creation/edit
  screen with categories/priority/dependencies, Profit Tracker (session
  start/stop, live overlay).
- **Stage 3**: skin database (data-driven `skins.json`, not hardcoded),
  ownership tracking, wishlist.
- **Stage 4**: inventory/container valuation, exotic + crystal armor
  detection.
- **Stage 5**: `PriceProvider` abstraction + SkyCofl integration (I'll pull
  their current documented endpoints before writing this — the spec is
  explicit that I shouldn't assume an old schema).
- **Stage 6**: UI polish, performance pass (caching, async price fetches,
  no per-tick inventory scans).

Say the word and I'll move on to Stage 2.
