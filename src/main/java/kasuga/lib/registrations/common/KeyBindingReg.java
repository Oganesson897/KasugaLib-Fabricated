package kasuga.lib.registrations.common;

import com.mojang.blaze3d.platform.InputConstants;
import kasuga.lib.core.KasugaLibStacks;
import kasuga.lib.core.annos.Inner;
import kasuga.lib.core.network.C2SPacket;
import kasuga.lib.registrations.Reg;
import kasuga.lib.registrations.registry.SimpleRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.function.Consumer;

public class KeyBindingReg extends Reg {
    public final int keyCode;
    public final String category;
    public final KeyMapping mapping;

    private Consumer<LocalPlayer> clientHandler;
    private Consumer<ServerPlayer> serverHandler;
    private static final LinkedList<KeyBindingReg> reference;

    public static ChannelReg keyChannel;

    static {
        keyChannel = new ChannelReg("kasuga_lib")
                .brand("1.0")
                .loadPacket(KeyBindingReg.KeySyncPacket.class, KeyBindingReg.KeySyncPacket::new)
                .submit(KasugaLibStacks.REGISTRY);
        reference = new LinkedList<>();
    }

    public KeyBindingReg(String translationKey, String categoryName, InputConstants.Type type, int defaultKeyCode, KeyModifier modifier) {
        super(translationKey);
        this.keyCode = defaultKeyCode;
        this.category = categoryName;
        this.mapping = new KeyMapping(translationKey, KeyConflictContext.UNIVERSAL, modifier, type, defaultKeyCode, categoryName);
        reference.add(this);
    }

    public KeyBindingReg setClientHandler(Consumer<LocalPlayer> consumer){
        clientHandler = consumer;
        return this;
    }

    public KeyBindingReg setServerHandler(Consumer<ServerPlayer> consumer){
        serverHandler = consumer;
        return this;
    }

    public static void onClientTick(){
        reference.stream().filter(reg -> reg.mapping.consumeClick())
                .forEach(reg -> {
                    if(reg.clientHandler != null){
                        reg.clientHandler.accept(Minecraft.getInstance().player);
                    }
                    keyChannel.sendToServer(new KeySyncPacket().setProperties(reg.toString()));
                });
    }

    @Inner
    public static void register(RegisterKeyMappingsEvent event) {
        reference.forEach(reg -> event.register(reg.mapping));
    }

    @Override
    public KeyBindingReg submit(SimpleRegistry registry) {
        return this;
    }

    @Override
    public String getIdentifier() {
        return "key_binding";
    }

    @Inner
    @Override
    public String toString() {
        return keyCode + category + mapping.getKey() + mapping.getName() + mapping.getCategory();
    }

    @Inner
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        KeyBindingReg that = (KeyBindingReg) o;

        return new EqualsBuilder()
                .append(keyCode, that.keyCode)
                .append(category, that.category)
                .append(registrationKey, that.registrationKey)
                .isEquals();
    }

    @Inner
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(keyCode)
                .append(category)
                .append(registrationKey)
                .toHashCode();
    }

    public static class KeySyncPacket extends C2SPacket{
        String key;

        public KeySyncPacket() {
            super();
        }

        public KeySyncPacket(FriendlyByteBuf buf) {
            super(buf);
            int length = buf.readInt();
            this.key = (String) buf.readCharSequence(length, StandardCharsets.UTF_8);
        }

        public KeySyncPacket setProperties(String key) {
            this.key = key;
            return this;
        }

        @Override
        public void handle(NetworkEvent.Context context) {
            KeyBindingReg keyBinding = KeyBindingReg.reference.stream().filter(reg -> reg.toString().equals(key)).toList().get(0);
            if(keyBinding.serverHandler != null){
                keyBinding.serverHandler.accept(context.getSender());
            }
        }

        @Override
        public void encode(FriendlyByteBuf buf) {
            buf.writeInt(key.length());
            buf.writeCharSequence(key, StandardCharsets.UTF_8);
        }
    }
}
