package dev.anvilcraft.portableaddon.network;

import dev.anvilcraft.portableaddon.AnvilcraftPortableAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class Packets {

    public static void register(RegisterPayloadHandlersEvent event) {

        final PayloadRegistrar registrar = event.registrar(AnvilcraftPortableAddon.MOD_ID);
        registrar.playToServer(DoubleJumpPacket.TYPE, DoubleJumpPacket.CODEC, DoubleJumpPacket::DoubleJump);
        registrar.playToServer(ResetDoubleJumpPacket.TYPE, ResetDoubleJumpPacket.CODEC, ResetDoubleJumpPacket::handle);
        registrar.playToServer(DoubleWalk.TYPE, DoubleWalk.CODEC, (packet, context) -> DoubleWalk.Double_Walk(context));
        registrar.playToClient(DoubleWalkCooldownS2CPacket.TYPE, DoubleWalkCooldownS2CPacket.CODEC, DoubleWalkCooldownS2CPacket::handle);


    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(Packets::register);
    }
}