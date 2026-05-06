package net.blupillcosby.furnacexptweaks;

import net.blupillcosby.furnacexptweaks.access.AbstractFurnaceBlockEntityAccessor;
import net.blupillcosby.furnacexptweaks.access.TechRebornExperienceAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.Map;

public class XpUtils {

    public static boolean isModSupportEnabled() {
        return FurnaceXPTweaks.CONFIG.modSupport.get();
    }

    /**
     * Attempts to extract stored XP from any BlockEntity.
     */
    public static double getStoredXpFromBlockEntity(BlockEntity be) {
        if (be == null) return 0;

        // Priority 1: Universal Accessor
        double universalXp = 0;
        if (isModSupportEnabled() && be instanceof TechRebornExperienceAccessor acc) {
            universalXp = acc.furnaceXpTweaks$getExperience();
        }

        // Case 1: Vanilla / AbstractFurnaceBlockEntity
        double recipeXp = 0;
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            var recipes = ((AbstractFurnaceBlockEntityAccessor) furnace).furnaceXpTweaks$getRecipesUsed();
            if (be.getLevel() != null) {
                recipeXp = calculateXpFromRecipesExact(recipes, be.getLevel().recipeAccess());
            }
        }
        
        if (universalXp > 0 || recipeXp > 0) return universalXp + recipeXp;

        if (!isModSupportEnabled()) return 0;

        // Case 2: Native fields / Maps (Cumulative for modded furnaces that haven't been cleared yet)
        double totalXp = 0;
        
        Class<?> clazz = be.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                // Try 'experience' field (Mod native style)
                try {
                    Field expField = clazz.getDeclaredField("experience");
                    expField.setAccessible(true);
                    totalXp += ((Number) expField.get(be)).doubleValue();
                } catch (NoSuchFieldException ignored) {}

                // Try 'recipes' or 'recipesUsed' map (Mod compatibility)
                try {
                    Field recipesField = null;
                    try { recipesField = clazz.getDeclaredField("recipes"); }
                    catch (NoSuchFieldException e) { recipesField = clazz.getDeclaredField("recipesUsed"); }
                    
                    if (recipesField != null) {
                        recipesField.setAccessible(true);
                        Object map = recipesField.get(be);
                        if (map instanceof java.util.Map<?, ?> jMap && be.getLevel() != null) {
                            if (be.getLevel().recipeAccess() instanceof RecipeManager manager) {
                                for (var entry : jMap.entrySet()) {
                                    Object key = entry.getKey();
                                    int count = ((Number) entry.getValue()).intValue();
                                    
                                    if (key instanceof ResourceKey<?> rk) {
                                        @SuppressWarnings("unchecked")
                                        ResourceKey<Recipe<?>> recipeKey = (ResourceKey<Recipe<?>>) (Object) rk;
                                        totalXp += manager.byKey(recipeKey)
                                                .map(XpUtils::getRecipeExperience)
                                                .orElse(0.0f) * count;
                                    } else if (key instanceof Identifier rl) {
                                        ResourceKey<Recipe<?>> rrk = ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, rl);
                                        totalXp += manager.byKey(rrk)
                                                .map(XpUtils::getRecipeExperience)
                                                .orElse(0.0f) * count;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }

        if (totalXp > 0) return totalXp;

        // Case 3: NBT Fallback (Last resort)
        if (be.getLevel() != null) {
            CompoundTag tag = be.saveWithFullMetadata(be.getLevel().registryAccess());
            return getStoredXpFromTag(tag);
        }

        return 0;
    }

    public static double getGainedExperience(BlockEntity be, Player player) {
        if (be == null) return 0;
        return Math.floor(getStoredXpFromBlockEntity(be));
    }

    public static double getGainedExperience(double furnaceXp, Player player) {
        return Math.floor(furnaceXp);
    }

    public static void claimXp(ServerPlayer player, BlockEntity be) {
        if (player == null || be == null) return;
        
        double furnaceXp = getStoredXpFromBlockEntity(be);
        int pointsToGive = (int) Math.floor(furnaceXp);
        
        // Ensure its not claimable until its reaches the first floored integer
        if (pointsToGive < 1) return;

        double remainder = furnaceXp - pointsToGive;

        if (FurnaceXPTweaks.CONFIG.useBubbles.get() && be.getLevel() instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, be.getBlockPos().getCenter(), pointsToGive);
        } else {
            player.giveExperiencePoints(pointsToGive);
        }

        setStoredXpInBlockEntity(be, remainder);
        be.setChanged();
        syncXpToClients(be);
    }

    public static void setStoredXpInBlockEntity(BlockEntity be, double amount) {
        if (be == null) return;

        // Priority 1: Universal Accessor
        if (be instanceof TechRebornExperienceAccessor acc) {
            acc.furnaceXpTweaks$setExperience((float) amount);
            be.setChanged();
        }

        // Case 2: Native fields (TechReborn electric furnaces, etc.)
        Class<?> clazz = be.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                // Try TR's 'experience' field
                try {
                    Field expField = clazz.getDeclaredField("experience");
                    expField.setAccessible(true);
                    expField.set(be, (float) amount);
                } catch (NoSuchFieldException ignored) {}

                // Try recipes maps
                try {
                    Field recipesField = null;
                    try { recipesField = clazz.getDeclaredField("recipes"); }
                    catch (NoSuchFieldException e) { recipesField = clazz.getDeclaredField("recipesUsed"); }
                    
                    if (recipesField != null) {
                        recipesField.setAccessible(true);
                        Object map = recipesField.get(be);
                        if (map instanceof java.util.Map<?, ?> jMap) {
                            jMap.clear();
                            be.setChanged();
                            return;
                        }
                    }
                } catch (Exception ignored) {}
                
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }
    }

    /** Extracts XP from an ItemStack by performing a deep scan of all potential data components. */
    public static double getStoredXpFromStack(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        // 1. Check if the block is vanilla if modSupport is disabled
        if (!isModSupportEnabled()) {
            if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
                if (!(bi.getBlock() instanceof net.minecraft.world.level.block.AbstractFurnaceBlock)) {
                    // Not a vanilla furnace, skip if mod support is off
                    return 0;
                }
            } else {
                return 0; // Not a block item
            }
        }

        // 2. Check BLOCK_ENTITY_DATA
        TypedEntityData<?> entityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (entityData != null) {
            double xp = getStoredXpFromTag(entityData.copyTagWithoutId());
            if (xp > 0) return xp;
        }
        
        // 2. Check CUSTOM_DATA (Commonly used by modern mods for extra item data)
        net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            double xp = getStoredXpFromTag(customData.copyTag());
            if (xp > 0) return xp;
        }

        return 0;
    }

    /** Performs a case-insensitive deep scan of an NBT tag for XP-related data. */
    private static double getStoredXpFromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return 0;

        // Priority 1: Our custom key
        if (tag.contains("furnacexptweaks:experience")) return tag.getFloatOr("furnacexptweaks:experience", 0.0f);
        if (tag.contains("furnacexptweaks:stored_xp")) return tag.getFloatOr("furnacexptweaks:stored_xp", 0.0f);

        // Priority 2: Standard modded recipe maps (IronFurnaces, TechReborn, etc.)
        for (String key : tag.keySet()) {
            if (key.equalsIgnoreCase("recipes") || key.equalsIgnoreCase("recipesUsed") || key.equalsIgnoreCase("recipes_used")) {
                Tag t = tag.get(key);
                if (t instanceof CompoundTag mapTag) {
                    return calculateMapXpFromNbt(mapTag);
                }
            }
        }

        // Priority 3: Deep scan for any field that smells like experience
        for (String key : tag.keySet()) {
            String lower = key.toLowerCase();
            // Check for any field that smells like experience. ID 5 is FloatTag.
            Tag t = tag.get(key);
            if (t != null && (lower.contains("experience") || lower.contains("storedxp") || lower.contains("stored_xp") || (lower.equals("xp") && t.getId() == 5))) {
                try {
                    return tag.getFloatOr(key, 0.0f);
                } catch (Exception ignored) {}
            }
        }

        return 0;
    }

    private static double calculateMapXpFromNbt(CompoundTag mapTag) {
        double total = 0;
        for (String key : mapTag.keySet()) {
            try {
                int count = mapTag.getIntOr(key, 0);
                // Estimate 0.7 XP per item if we can't resolve the recipe (safe average for modded ores/food)
                total += 0.7 * count;
            } catch (Exception ignored) {}
        }
        return total;
    }

    public static void syncXpToClients(BlockEntity be) {
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide()) return;
        double xp = getStoredXpFromBlockEntity(be);
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            var packet = new net.blupillcosby.furnacexptweaks.network.SyncXpPayload(be.getBlockPos(), xp);
            for (var player : serverLevel.players()) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, packet);
            }
        }
    }

    public static double getExperienceFromLevelDouble(final int level) {
        if (level >= 30) {
            return 112.0 + (level - 30) * 9.0;
        } else {
            return level >= 15 ? 37.0 + (level - 15) * 5.0 : 7.0 + level * 2.0;
        }
    }

    public static double getExperienceToLevelDouble(final int level) {
        double L = (double) level;
        if (L <= 16) {
            return L * L + 6.0 * L;
        } else if (L <= 31) {
            return 2.5 * L * L - 40.5 * L + 360.0;
        } else {
            return 4.5 * L * L - 162.5 * L + 2220.0;
        }
    }

    public static double xpToLevel(double xp) {
        if (xp <= 352.0) {
            return Math.sqrt(xp + 9.0) - 3.0;
        } else if (xp <= 1507.0) {
            return 8.1 + Math.sqrt(0.4 * (xp - 195.975));
        } else {
            return 18.055555555555557 + Math.sqrt(0.2222222222222222 * (xp - 752.9861111111111));
        }
    }

    public static double getPlayerExperienceExact(Player player) {
        double currentLevelXp = getExperienceToLevelDouble(player.experienceLevel);
        double progressXp = player.experienceProgress * getExperienceFromLevelDouble(player.experienceLevel);
        return currentLevelXp + progressXp;
    }

    public static void setPlayerExperienceExact(ServerPlayer player, double newXp) {
        double exactLevel = xpToLevel(newXp);
        int newLevel = (int) Math.floor(exactLevel);
        double xpAtLevel = getExperienceToLevelDouble(newLevel);
        double xpIntoLevel = newXp - xpAtLevel;

        player.experienceLevel = newLevel;
        player.experienceProgress = (float) (xpIntoLevel / getExperienceFromLevelDouble(newLevel));
        player.totalExperience = (int) Math.min(newXp, (double) Integer.MAX_VALUE);
    }

    public static double calculateXpFromRecipesExact(Map<ResourceKey<Recipe<?>>, Integer> recipesUsed, net.minecraft.world.item.crafting.RecipeAccess recipeAccess) {
        if (!(recipeAccess instanceof RecipeManager recipeManager)) return 0;
        double totalXp = 0;
        for (Map.Entry<ResourceKey<Recipe<?>>, Integer> entry : recipesUsed.entrySet()) {
            totalXp += recipeManager.byKey(entry.getKey())
                    .map(recipe -> (double)((AbstractCookingRecipe) recipe.value()).experience() * entry.getValue())
                    .orElse(0.0);
        }
        return totalXp;
    }

    public static float getRecipeExperience(RecipeHolder<?> holder) {
        if (holder.value() instanceof AbstractCookingRecipe cr) {
            return cr.experience();
        }
        return 0.0f;
    }

    public static float getRecipeExperience(Recipe<?> recipe) {
        if (recipe instanceof AbstractCookingRecipe cr) {
            return cr.experience();
        }
        return 0.0f;
    }
}
