package dev.anvilcraft.portableaddon;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = AnvilcraftPortableAddon.MOD_ID)
public class AddonConfig {
    @Comment("Teleport distance for the Double Walk enchantment (in blocks)\n雷行附魔的传送距离（以方块为单位）")
    @BoundedDiscrete(min = 1.0, max = 30.0)
    public double doubleWalkDistance = 5.0;

    @Comment("Power output for the Power Block\n能源方块的发电功率")
    @BoundedDiscrete(min = 0, max = 32)
    public int powerBlockOutput = 1;
}
