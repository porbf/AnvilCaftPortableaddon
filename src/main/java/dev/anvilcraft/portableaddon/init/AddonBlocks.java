package dev.anvilcraft.portableaddon.init;

import dev.anvilcraft.portableaddon.AnvilcraftPortableAddon;
import dev.anvilcraft.portableaddon.block.blocks.PowerBlock.PowerBlock;
import dev.anvilcraft.portableaddon.block.blocks.ender_transmission_pole.EnderPoleBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AnvilcraftPortableAddon.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.block.Block, PowerBlock> POWER_BLOCK
            = BLOCKS.register("power_block", PowerBlock::new);
    public static final DeferredHolder<net.minecraft.world.level.block.Block, EnderPoleBlock> ENDERPOLE=
            BLOCKS.register("ender_pole", EnderPoleBlock::new);

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
    }
}

public class ModBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AnvilcraftPortableAddon.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.anvilcraft.portableaddon.block.blocks.PowerBlock.PowerBlockEntity>> APOWER_BE =
            BLOCK_ENTITIES.register("power_block_entity", () ->
                    BlockEntityType.Builder.of(dev.anvilcraft.portableaddon.init.blocks.PowerBlock.PowerBlockEntity::new, ModBlocks.POWER_BLOCK.get()).build(null)
            );

    public static final Supplier<BlockEntityType<dev.anvilcraft.portableaddon.block.blocks.ender_transmission_pole.EnderPoleBlockEntity>> ENDER_POLE_ENTITY =
            BLOCK_ENTITIES.register("ender_pole_entity", () ->
                    BlockEntityType.Builder.of(
                            dev.anvilcraft.portableaddon.block.blocks.ender_transmission_pole.EnderPoleBlockEntity::new,
                            ModBlocks.ENDERPOLE.get() // 核心：这里直接传你的方块对象，不要写任何过滤
                    ).build(null)
            );


    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ModBlockEntity.BLOCK_ENTITIES.register(modEventBus);
    }
}
