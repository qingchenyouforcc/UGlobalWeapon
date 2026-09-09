package org.mmga.uglobal.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Explicit press/release plus heartbeat; the server alone advances charge and spends ammo. */
public final class ChargeInput implements ClientModInitializer {
    private static boolean down;
    private static int session,ticks;
    private static Object connection;
    public record Payload(byte action,int session) implements CustomPayload {
        public static final Id<Payload> ID=new Id<>(Identifier.of("uglobalweapon","charge"));
        public static final PacketCodec<RegistryByteBuf,Payload> CODEC=PacketCodec.of(Payload::write,buf->new Payload(buf.readByte(),buf.readInt()));
        private void write(RegistryByteBuf buf) {buf.writeByte(action);buf.writeInt(session);}
        @Override public Id<Payload> getId() {return ID;}
    }
    @Override public void onInitializeClient() {PayloadTypeRegistry.playC2S().register(Payload.ID,Payload.CODEC);}
    public static void tick(MinecraftClient client) {
        if(connection!=client.getNetworkHandler()) {connection=client.getNetworkHandler();down=false;ticks=0;session=0;}
        if(client.player==null || client.getNetworkHandler()==null) {down=false;return;}
        var stack=client.player.getMainHandStack();
        var data=stack.get(DataComponentTypes.CUSTOM_DATA);
        boolean weapon=RpgViewEffects.isRpg(stack) && data!=null
                && data.copyNbt().getCompound("PublicBukkitValues").getByte("uglobalweapon:annihilation")!=0;
        boolean pressed=weapon && client.currentScreen==null && client.isWindowFocused() && !client.player.isDead() && client.options.useKey.isPressed();
        if(pressed && !down) {session++;ClientPlayNetworking.send(new Payload((byte)1,session));ticks=0;}
        else if(!pressed && down) ClientPlayNetworking.send(new Payload((byte)0,session));
        else if(pressed && ++ticks%2==0) ClientPlayNetworking.send(new Payload((byte)2,session));
        down=pressed;
    }
}
