# Armored Arsenal

Armored Arsenal is a NeoForge Minecraft Java mod for the 26.1 line. It keeps the player in survival mode with normal health and danger, then adds creative-style item access, powered armor, and laser weapons through in-game commands and menus.

## Download

Download the latest mod and friend installation pack from [GitHub Releases](https://github.com/yin-wen-bin/armored-arsenal/releases/latest).

## Requirements

- Minecraft Java / NeoForge `26.1`
- Microsoft OpenJDK `25`
- Run with `.\gradlew build` after Java is installed and on PATH

## Commands

- `/suits` or chat message `suits`: opens the suit selector.
- `/guns` or chat message `guns`: opens the laser weapon selector.
- `/items`: opens the survival-safe item supply menu.
- `/manual`, `/guide`, or chat messages `manual`, `guide`, `book`: gives the in-game instruction book.
- Chat message `give ethan0315 bedrock`: gives the player two stacks of bedrock.
- `/wand`: gives a wooden axe for WorldEdit selection. Right-click two opposite corners.
- `/we fill <block>`, `/fill <block>`, or `/set <block>`: fills only the region selected with the wooden axe; no coordinates are needed. Material shortcuts work, so `/fill diamond` uses diamond blocks and `/fill netherite` uses netherite blocks.
- `/walls <block>` or `/we walls <block>`: builds the four vertical walls around the axe-selected region.
- `/worldedit clear` or `/we clear`: clears the saved selection.
- `/transform <mob>`: transforms your visible model and powers to match any living mob, such as `/transform blaze`, `/transform dolphin`, or `/transform minecraft:warden`.
- `/transform clear`: returns you to normal.
- Water Flood TNT: right-click a block to prime it. After four seconds it fills a bounded 11-block-wide, three-block-deep pool area without breaking solid blocks.
- Material golems: build the normal iron golem T shape with four matching blocks, then place a carved pumpkin or jack o lantern on top. Four bedrock blocks make a Bedrock Golem.

## Current V1 Features

- Mark 15-inspired powered armor using original mod assets.
- Full-suit flight/hover, fall protection, stealth, night vision, and repulsor firing.
- Laser rifle, pulse pistol, beam cannon, and charged sniper laser.
- Wooden-axe WorldEdit selection with safe `/worldedit` and `/we` fill commands.
- Material golems, including a high-health Bedrock Golem, spawned from iron-golem-style block shapes.
- Synchronized mob transformations with mob-based health, strength, speed, flight, water, fire, and vision powers.
- Water Flood TNT for quickly filling medium pools without terrain damage.
- Server-authoritative selection through Minecraft menu button packets.
- HUD/actionbar feedback for suit energy and status.

This mod does not ship Marvel-owned models, logos, or ripped textures. The resource paths are intentionally simple so private, legally usable replacement assets can be dropped in later.

