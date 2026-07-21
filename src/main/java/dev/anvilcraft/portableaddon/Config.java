package dev.anvilcraft.portableaddon;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ==========================================
    // Double Walk (雷行) — 传送距离
    // ==========================================
    public static final ModConfigSpec.DoubleValue DOUBLE_WALK_DISTANCE = BUILDER
            .comment("Teleport distance for the Double Walk enchantment (in blocks)",
                    "雷行附魔的传送距离（以方块为单位）")
            .defineInRange("doubleWalkDistance", 5.0, 1.0, 100.0);

    // ==========================================
    // Power Block (能源方块) — 发电功率
    // ==========================================
    public static final ModConfigSpec.IntValue POWER_BLOCK_OUTPUT = BUILDER
            .comment("Power output for the Power Block (in AE/t)",
                    "能源方块的发电功率（AE/刻）")
            .defineInRange("powerBlockOutput", 1, 0, 1000000);

    static final ModConfigSpec SPEC = BUILDER.build();
}
