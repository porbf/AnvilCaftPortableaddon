package dev.anvilcraft.portableaddon.client;

import dev.anvilcraft.portableaddon.AnvilcraftPortableAddon;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AnvilcraftPortableAddon.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AnvilcraftPortableAddon.MOD_ID, value = Dist.CLIENT)
public class AnvilCraftAddonTemplateClient {
    public AnvilCraftAddonTemplateClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AnvilcraftPortableAddon.LOGGER.info("HELLO FROM CLIENT SETUP");
        AnvilcraftPortableAddon.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
