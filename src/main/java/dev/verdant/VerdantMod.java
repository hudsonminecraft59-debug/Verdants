package dev.verdant;

import com.mojang.blaze3d.platform.InputConstants;
import dev.verdant.config.ConfigManager;
import dev.verdant.gui.ClickGuiScreen;
import dev.verdant.module.Module;
import dev.verdant.module.ModuleManager;
import dev.verdant.render.EspRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class VerdantMod implements ClientModInitializer {
    public static final String ID = "verdant";
    public static final String VERSION = "1.0.0";

    private static KeyMapping openGui;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        openGui = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.verdant.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ClickGuiScreen());
            }
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled()) m.onTick(mc);
            }
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(EspRenderer::render);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
            EspRenderer.close();
        });
    }
}
