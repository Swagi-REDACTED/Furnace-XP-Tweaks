package net.blupillcosby.furnacexptweaks.config;

import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;

@Version(version = 1)
public class ModConfig extends Config {

    public ValidatedBoolean modSupport = new ValidatedBoolean(true);
    public ValidatedInt maxStoredLevels = new ValidatedInt(100, 1000, 1);
    
    // These two are client-only and should not be synced from the server
    public ValidatedBoolean matchGuiColors = new ValidatedBoolean(true);
    public ValidatedBoolean useAltTextures = new ValidatedBoolean(true);

    public ModConfig() {
        super(FurnaceXPTweaks.id("main"));
    }

    @Override
    public int defaultPermLevel() {
        return 2;
    }
}
