package dev.anvilcraft.portableaddon;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;
import dev.anvilcraft.lib.config.ConfigManager;
import dev.anvilcraft.portableaddon.init.AddonBlocks;
import dev.anvilcraft.portableaddon.init.AddonItems;
import dev.anvilcraft.portableaddon.init.AddonItemGroups;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(AnvilcraftPortableAddon.MOD_ID)
public class AnvilcraftPortableAddon {
    public static final String MOD_ID = "anvilcraft_portable_addon";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AddonConfig CONFIG = ConfigManager.register(AnvilcraftPortableAddon.MOD_ID, AddonConfig::new);
    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public AnvilcraftPortableAddon(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("Ciallo～(∠・ω< )⌒★");

        AddonItems.register(modEventBus);//注册物品
        dev.anvilcraft.portableaddon.DataComponents.register(modEventBus);//注册数据类型
        dev.anvilcraft.portableaddon.Packet.Packets.init(modEventBus);//注册数据包
        dev.anvilcraft.portableaddon.EnchantmentEffects.register(modEventBus);//注册附魔
        AddonBlocks.register(modEventBus);
        dev.anvilcraft.portableaddon.block.ModBlockEntity.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        dev.anvilcraft.portableaddon.init.AddonItemGroups.CREATIVE_MODE_TABS.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (dev.anvilcraft.portableaddon.Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", dev.anvilcraft.portableaddon.Config.MAGIC_NUMBER_INTRODUCTION.get(), dev.anvilcraft.portableaddon.Config.MAGIC_NUMBER.getAsInt());

        dev.anvilcraft.portableaddon.Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AddonItemGroups.ADDON_TAB.getKey()) {
            event.accept(dev.anvilcraft.portableaddon.init.AddonItem.);
            event.accept(dev.anvilcraft.portableaddon.ModItems.POWER_BLOCK_ITEM);
            event.accept(dev.anvilcraft.portableaddon.ModItems.ENDERPOLE_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        //LOGGER.info("HELLO from server starting");
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}