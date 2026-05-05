package net.blupillcosby.furnacexptweaks.config;

import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.blupillcosby.furnacexptweaks.FurnaceXPTweaks;

@Version(version = 1)
public class ModConfig extends Config {

    public ValidatedInt maxStoredLevels = new ValidatedInt(100, 1000, 1);
    public ValidatedBoolean matchGuiColors = new ValidatedBoolean(true);

    public ModConfig() {
        super(FurnaceXPTweaks.id("main"));
    }

    @Override
    public int defaultPermLevel() {
        return 2;
    }
}
