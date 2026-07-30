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

                        Equip Mark 15 to wear the full powered armor set.

                        Helmet: night vision.
                        Full set: hover, fall protection, repulsor beam, stealth, and suit energy.
                        """),
                page("""
                        Hover + Stealth

                        Open suits.

                        Toggle Hover to slow falling and unlock flight controls while the full suit is worn.

                        Toggle Stealth to turn invisible. Stealth drains energy over time.
                        """),
                page("""
                        Repulsor Beam

                        Open suits.

                        Fire Repulsor shoots a beam from your view direction.

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

                        Lasers damage mobs and make particles, but v1 does not break terrain.
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
                        Type /transform clear to return to normal.
                        """),
                page("""
                        Material Golems

                        Build the normal iron golem shape, but use four matching blocks instead of iron.

                        Put a carved pumpkin or jack o lantern on top.

                        Example: four bedrock blocks make a Bedrock Golem with huge health.
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
