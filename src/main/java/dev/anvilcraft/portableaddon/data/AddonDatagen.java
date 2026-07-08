package dev.anvilcraft.portableaddon.data;

import com.tterrag.registrate.providers.ProviderType;
import dev.anvilcraft.portableaddon.AnvilcraftPortableAddon;
import dev.anvilcraft.portableaddon.data.lang.AddonLangHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.portableaddon.AnvilcraftPortableAddon.REGISTRATE;

@EventBusSubscriber(modid = AnvilcraftPortableAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class AddonDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {}

    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
    }
}
