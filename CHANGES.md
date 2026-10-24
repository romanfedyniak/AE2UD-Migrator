# Changelog

All notable AE2UD Migrator changes are grouped by the version in which they first appeared.

## Important compatibility notice

- Back up the world before installing the mod. Migration is one-way: a migrated world does not open on AE2 UEL
  again.
- AE2UD Migrator runs only with AE2 Unofficial Deconstructed.

## Unreleased

- **Requires AE2UD 1.6.0 or newer.** AE2UD shares its mod id with AE2 and AE2 UEL, so the version range is
  what keeps the game from starting with either of them in its place.
- **A world saved with NAE2 keeps what it built with it.** The dense storage cells, crafting storage and
  co-processors, the exposer, the interface P2P tunnel, the cell components and the auto-complete card become
  AE2UD's own, with what the cells hold. NAE2's beam formers become Beam Formers' small and dense beam
  formers, and its gas cells Mekanism Energistics' gas cells, when those mods are installed. The improved,
  advanced and perfect portable cells become AE2UD's 1024k, 4096k and 16384k portable cells, keeping every
  entry, even past the number of types those take. The pattern multi-tool becomes AE2UD's. The void cells,
  the virtual pattern, the reconstruction chamber, the hyper-acceleration card and the GregTech circuit card
  have no equivalent and are gone; the chamber's items go into a chest beside where it stood.

- **A world saved with AE2FluidCraft-Rework keeps its fluid autocrafting.** Its processing patterns become
  AE2UD's, with the fluids in them, the same item twice as one entry; one that still needs more inputs than an
  AE2UD pattern holds, or reads as nothing, becomes a blank pattern, and the log has what it was. Its crafting
  patterns with fluid containers become crafting patterns that substitute fluids. The fluid pattern terminals,
  the wireless one included, become AE2UD's pattern terminals. A dual or trio interface, block or part, is
  AE2UD's interface: the fluid and gas slots move into the slots the items left free, each with what it
  stocked, and their cards into free card slots, the rest into a chest beside it. The fluid assembler becomes
  an interface with its patterns and its pattern expansion cards, crafted by a molecular assembler placed
  beside it. The fluid level maintainer becomes Lazy AE2's level maintainer, when that mod is installed.
  Fluid and gas drops and packets become the fluids and gases they stood for. The encoders, buffers, burette,
  packet decoder and discretizers have no equivalent and are gone; the items they held go into a chest
  beside where they stood, and their fluids into the log.

- **A wireless universal terminal becomes AE2UD's wireless terminal with the same modes.** Each mode keeps
  its own grid, the open one stays open, and the fluid and gas terminals become the plain terminal, which
  shows every kind. The extended crafting terminals' modes are kept with their grids for when a mod
  registers them again.

- **AE Network Visualization Tool's tool becomes AE2UD's network visualiser,** still bound to its block and
  drawing what it drew. Its colours and the rest of its settings are left behind.

- **AE Wireless Channel's buses become ae2stuff's wireless blocks.** Every output bus is a wireless hub and
  each input bus a wireless connector paired with it, and a key card bound to an output bus is a wireless
  kit bound to the hub that stands there. ae2stuff's blocks join a network on every side, where a bus joined
  only at its back, so a bus that stood against a cable or machine of another network now joins it to that
  network; the log has the position of every bus, and every link with its length and the power it takes at
  ae2stuff's default settings. A bus taken down with its link loses it, and chunks a bus kept loaded are not.
