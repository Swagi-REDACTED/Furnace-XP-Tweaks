package net.blupillcosby.furnacexptweaks.mixin.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T furnaceXpTweaks$addRenderableWidget(T widget);

    @Accessor("renderables")
    List<Renderable> furnaceXpTweaks$getRenderables();

    @Accessor("children")
    List<GuiEventListener> furnaceXpTweaks$getChildren();

    @Accessor("narratables")
    List<NarratableEntry> furnaceXpTweaks$getNarratables();
}
