package com.ethan.armoredarsenal.content;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

public final class InstructionBook {
    private static final String TITLE = "Armored Arsenal Guide";

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.ITEM_NAME, Component.literal(TITLE));
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(TITLE),
                "Codex",
                0,
                pages(),
                true));
        return book;
    }

    private static List<Filterable<Component>> pages() {
        return List.of(
                page("""
                        Armored Arsenal

                        Press T to open chat.

                        Type these and press Enter:
                        suits
                        guns
                        items
                        manual
                        give ethan0315 bedrock

                        Slash commands work too:
                        /suits
                        /guns
                        /items
                        /manual
                        """),
                page("""
                        Survival + Access

                        You stay in survival mode.

                        Hearts, hunger, mob damage, fall damage, and death still work.

                        Use /items for creative-style supply kits without true creative invulnerability.
                        """),
                page("""
                        Suit Menu

                        Type suits or /suits.

                        Choose Mark 15 or Infinity Armor to equip a full powered set.

                        Both sets: night vision, hover, flight, fall protection, an energy beam, and suit energy.
                        """),
                page("""
                        Mark 15 Special

                        Open suits.

                        Toggle Hover to slow falling and unlock flight controls while the full suit is worn.

                        Toggle Special while wearing Mark 15 to turn invisible. Stealth drains energy over time.
                        """),
                page("""
                        Infinity Armor

                        Its obsidian, violet, and cyan design is completely original.

                        The full set gives fire resistance, underwater breathing, and slow self-repair.

                        Toggle Special activates a cosmic damage shield. The shield drains suit energy.
                        """),
                page("""
                        Repulsor Beam

                        Open suits.

                        Fire Energy Beam shoots a beam from your view direction.

                        It costs suit energy and has a cooldown, so wait if it says Repulsor charging.
                        """),
                page("""
                        Guns Menu

                        Type guns or /guns.

                        Pick one:
                        Laser Rifle
                        Pulse Pistol
                        Beam Cannon
                        Charged Sniper Laser

                        Hold the weapon and right-click to fire.
                        """),
                page("""
                        Weapon Notes

                        Laser Rifle: balanced.
                        Pulse Pistol: fast.
                        Beam Cannon: heavy.
                        Charged Sniper: long range.

                        Lasers damage mobs and render as solid beams, but they do not break terrain.
                        """),
                page("""
                        Items Menu

                        Type items or /items.

                        Building Kit: blocks.
                        Valuables Kit: rich blocks.
                        Redstone Kit: circuits.
                        Survival Kit: food/tools.
                        Suit + Guns: fast start.
                        Mob Eggs: test enemies.

                        Type give ethan0315 bedrock for 2 stacks of bedrock.
                        """),
                page("""
                        Wooden Axe WorldEdit

                        Type /wand to get an axe.

                        Right-click the first corner with the wooden axe, then right-click the opposite corner.

                        /fill stone fills the box.
                        /fill diamond uses diamond blocks.
                        /fill netherite uses netherite blocks.
                        /walls stone builds its four walls.

                        Look at a structure and type //giant to make the structure and its nearby surroundings three times larger. No coordinates or axe selection are needed.

                        Full block names like oak_planks also work.
                        """),
                page("""
                        Water Flood TNT

                        Find it in the Armored Arsenal creative tab or the Suit + Guns kit.

                        Hold it and right-click a block to prime it.

                        After four seconds it floods a medium pool area without breaking solid pool walls.
                        """),                page("""
                        Transformation Power

                        /transform blaze changes you into a Blaze.
                        /transform dolphin gives water powers.
                        /transform warden gives massive strength and health.

                        Use any living mob name.
                        Type /power to use that mob's active power. Blaze fires a volley, Wither shoots skulls, and Ender Dragon launches damaging dragon breath.

                        As a Warden, aim within 20 blocks and use /power or /sonicboom.
                        Type /transform clear to return to normal.
                        """),
                page("""
                        Buildable End Portal

                        Make a flat 5 x 5 obsidian ring with an empty 3 x 3 center.

                        Right-click any obsidian in the ring while holding an Eye of Ender.

                        Enter the portal to reach the main End island.
                        """),                page("""
                        Lasers and Turrets

                        Two Portable Lasers on directly opposite walls connect across up to 64 clear blocks. Their beam damages anything crossing it.

                        Place an Auto Turret on the floor. Right-click it with an Armored Arsenal gun to load it. It automatically targets hostile mobs.

                        Crouch and right-click the turret with an empty hand to take the gun back.
                        """),                page("""
                        Rocket Arsenal

                        Right-click while holding a rocket to launch it.

                        Stinger: fast, compact power 2.0.
                        Siegebreaker: power 2.5.
                        Titan: slow, power 3.0.

                        Rockets can also be loaded into Auto Turrets. Rocket turrets do not fire at targets closer than five blocks.
                        """),                page("""
                        Heavy Weapons

                        Rifle: accurate, long-range single shots.
                        Minigun: a rapid six-shot burst.
                        Bazooka: fires a straight Titan-class rocket.
                        Grenade Launcher: fires an arcing Siegebreaker-class shell.

                        Right-click to fire. Every weapon can be installed in an Auto Turret.

                        Rifle and Laser Rifle must be loaded manually. Crouch and right-click to reload. Rifle uses an iron nugget; Laser Rifle uses redstone dust.
                        """),                page("""
                        Arsenal Dimension

                        Type /arsenal to enter the new dimension. Type it again there to return to the Overworld.

                        Mine underground for Arcanite Ore. Arcanite crafts into a complete tool and armor set using normal Minecraft recipe shapes.
                        """),                page("""
                        Kitchen and Bathroom

                        Counters, sinks, ovens, and toilets must be placed on a sturdy floor.

                        Cabinets and kitchen towels attach to walls. Right-click the toilet to sit on it.
                        """),                page("""
                        Material Golems

                        Build the normal iron golem shape, but use four matching blocks instead of iron.

                        Put a carved pumpkin or jack o lantern on top.

                        Example: four bedrock blocks make a Bedrock Golem with huge health.

                        A large connected structure made of diamond blocks, netherite blocks, heads, and pumpkins can become a Netherite-Diamond Colossus. Stand near it and type /golem inspect, then /golem awaken to confirm.
                        """),                page("""
                        Wither Storm

                        Type /witherstorm or chat message witherstorm to summon the nine-headed boss.

                        Fly into the front of any rounded black head to enter the core chamber. The storm is built from small blocks. Two purple tractor beams shine from the side bodies, but do not pull you in. Creative players can enter safely.

                        The two inner heads and black tentacles guard the command block core. Craft a Command Block Axe or type /witherstorm axe. Hit the core once with it to trigger the death sequence.

                        When the core breaks, the heads open their square, toothed mouths before the white-out. You are ejected, the two outer bodies fall into the void, the hole seals, and purple light shines as the main body slowly disintegrates. Three Withered Wither Stars and nine levels drop. Craft a Withered Beacon to become a mini storm while it stands. Break it to turn back. /witherstorm clear removes the boss.
                        """),                page("""
                        Quick Start

                        1. Type suits.
                        2. Click Equip Mark 15.
                        3. Type guns.
                        4. Click Give All.
                        5. Make a survival world and test against mobs.

                        Have fun, Ethan.
                        """));
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text.stripIndent().trim()));
    }

    private InstructionBook() {}
}
