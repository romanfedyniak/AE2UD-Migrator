# AE2UD Migrator

Carries a Minecraft 1.12.2 world played on AE2 Unofficial Extended Life over to
[AE2 Unofficial Deconstructed](https://github.com/romanfedyniak/AE2UD), for the mods AE2UD took in or
replaced:

- [NAE2](https://github.com/AE2-UEL/NAE2): dense cells, storage and co-processors, the exposer, the interface
  P2P tunnel, cards and cell components; its beam formers go to
  [Beam Formers](https://github.com/romanfedyniak/BeamFormers), its gas cells to
  [Mekanism Energistics](https://github.com/romanfedyniak/MekanismEnergistics).
- [AE2FluidCraft-Rework](https://github.com/Circulate233/AE2FluidCraft-Rework-Unofficial): fluid patterns,
  terminals, dual and trio interfaces, the fluid assembler; its level maintainer goes to
  [Lazy AE2 UD](https://github.com/romanfedyniak/Lazy-AE2UD).
- AE2UEL Wireless Universal Terminal, AE Network Visualization Tool, and AE Wireless Channel, whose buses go
  to [ae2stuff](https://github.com/romanfedyniak/ae2stuff).

AE2UD itself carries its own UEL content over, and each of its addons its own; this mod only tells AE2UD what
the other mods' content becomes. It changes a world as each part of it loads, once, and only a world last
saved on AE2 UEL. What has no equivalent, and where things were put, is written to
`logs/ae2ud-migration.log`.

It adds nothing to the game and stays installed: parts of a world that have not been loaded yet are migrated
when they are.

## Compatibility

- Back up the world first. Migration is one-way.
- Only the latest release of each old mod is read. An older one is read as far as its saves match.

## License

LGPL-3.0; see [LICENSE.txt](LICENSE.txt). Nothing here is copied from the mods it reads; it knows only their
ids and the names in their saves.
