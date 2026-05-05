package net.blupillcosby.furnacexptweaks.mixin.client;

import net.blupillcosby.furnacexptweaks.client.XpButton;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin<T extends AbstractFurnaceMenu> extends AbstractRecipeBookScreen<T> {

    public AbstractFurnaceScreenMixin(T menu, net.minecraft.client.gui.screens.recipebook.RecipeBookComponent<?> recipeBook, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, recipeBook, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void furnaceXpTweaks$addXpButton(CallbackInfo ci) {
        int x = this.leftPos + 144;
        int y = this.topPos + 37;
        this.addRenderableWidget(new XpButton(x, y, () -> this.menu));
    }
}
