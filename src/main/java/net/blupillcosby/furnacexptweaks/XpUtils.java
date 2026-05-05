package net.blupillcosby.furnacexptweaks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Map;

public class XpUtils {

    /**
     * Returns the experience points required to reach the specified level from the previous level.
     */
    public static double getExperienceFromLevelDouble(final int level) {
        if (level >= 30) {
            return 112.0 + (level - 30) * 9.0;
        } else {
            return level >= 15 ? 37.0 + (level - 15) * 5.0 : 7.0 + level * 2.0;
        }
    }

    /**
     * Returns the total experience points required to reach a specific level starting from level 0.
     * 100% mathematical accuracy using Minecraft's exact quadratic logic.
     */
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

    /**
     * The inverse of Minecraft's quadratic XP formulas.
     * Converts a total XP point value into an exact fractional level.
     */
    public static double xpToLevel(double xp) {
        if (xp <= 0) return 0;
        if (xp <= 352) {
            // Inverse of: XP = L^2 + 6L
            return -3.0 + Math.sqrt(9.0 + xp);
        } else if (xp <= 1507) {
            // Inverse of: XP = 2.5L^2 - 40.5L + 360
            return (81.0 + Math.sqrt(40.0 * xp - 7839.0)) / 10.0;
        } else {
            // Inverse of: XP = 4.5L^2 - 162.5L + 2220
            return (325.0 + Math.sqrt(72.0 * xp - 54215.0)) / 18.0;
        }
    }

    /** Finds the current whole level based on total experience points using the inverse formula. */
    public static int getLevelFromExperienceExact(double totalXp) {
        return (int) Math.floor(xpToLevel(totalXp));
    }

    /** Reconstructs the player's total XP as an exact double. */
    public static double getPlayerExperienceExact(Player player) {
        return getExperienceToLevelDouble(player.experienceLevel)
                + (double) player.experienceProgress * getExperienceFromLevelDouble(player.experienceLevel);
    }

    /**
     * Grants XP with double precision.
     */
    public static void giveExperienceExact(Player player, double amount) {
        if (amount <= 0) return;
        
        double currentXp = getPlayerExperienceExact(player);
        double newXp = currentXp + amount;
        
        double exactLevel = xpToLevel(newXp);
        int newLevel = (int) Math.floor(exactLevel);
        double xpIntoLevel = newXp - getExperienceToLevelDouble(newLevel);
        
        player.experienceLevel = newLevel;
        player.experienceProgress = (float) (xpIntoLevel / getExperienceFromLevelDouble(newLevel));
        player.totalExperience = (int) Math.min(newXp, (double) Integer.MAX_VALUE);
    }

    public static double calculateXpFromRecipesExact(Map<ResourceKey<Recipe<?>>, Integer> recipesUsed, net.minecraft.world.item.crafting.RecipeAccess recipeAccess) {
        if (!(recipeAccess instanceof RecipeManager recipeManager)) return 0;

        double totalXp = 0;
        for (Map.Entry<ResourceKey<Recipe<?>>, Integer> entry : recipesUsed.entrySet()) {
            int count = entry.getValue();
            totalXp += recipeManager.byKey(entry.getKey())
                    .map(recipe -> (double)((AbstractCookingRecipe) recipe.value()).experience() * count)
                    .orElse(0.0);
        }
        return totalXp;
    }

    public static int calculateXpFromRecipes(Map<ResourceKey<Recipe<?>>, Integer> recipesUsed, net.minecraft.world.item.crafting.RecipeAccess recipeAccess) {
        return (int) calculateXpFromRecipesExact(recipesUsed, recipeAccess);
    }
}
