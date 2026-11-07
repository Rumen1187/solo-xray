# Solo X-Ray (Ore ESP) - Fabric, Minecraft 26.2

Singleplayer / own-LAN-world ore finder. Automatically disables itself on servers.

Commands (client-side, typed in chat):
  /xray                toggle glowing, color-coded outlines through walls
  /xray scan           text radar: nearest of each ore with distance + direction
  /xray add <ore>      diamond netherite emerald gold redstone lapis iron copper coal quartz
  /xray remove <ore>
  /xray targets        show active targets
  /xray radius <1-4>   chunks around you to scan (default 3)

Default targets: diamond + netherite. Outline colors: diamond=aqua,
netherite(ancient debris)=light purple, emerald=green, gold=yellow, redstone=red,
lapis=blue, iron=white, copper=orange-ish, coal=dark gray, quartz=gray.

Tips: ancient debris is in the Nether (mostly low, around Y 8-22 - verify in-game);
diamonds are deep underground, richest near the world bottom.

Build: see the GitHub Actions workflow (.github/workflows/build.yml) or run
`gradle build` with JDK 25 + Gradle 9.3+. Jar: build/libs/solo-xray-1.0.0.jar
