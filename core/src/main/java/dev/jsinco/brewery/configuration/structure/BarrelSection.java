package dev.jsinco.brewery.configuration.structure;

import dev.jsinco.brewery.api.config.Configuration;
import dev.jsinco.brewery.api.moment.Moment;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;

import java.util.List;

public class BarrelSection extends OkaeriConfig implements Configuration.Barrels {

    @Comment("How many ticks it will take to age a brew one year")
    @CustomKey("aging-year-ticks")
    private long agingYearTicks = Moment.DEFAULT_AGING_YEAR;

    @Comment("Should players only be able to create barrels with a sign that has a keyword on the first line?")
    @CustomKey("require-sign-keyword")
    private boolean requireSignKeyword = false;

    @Comment("For what keywords should we check? (case insensitive)")
    @CustomKey("sign-keywords")
    private List<String> signKeywords = List.of("barrel");

    @Override
    public long agingYearTicks() {
        return this.agingYearTicks;
    }

    public boolean requireSignKeyword() {
        return this.requireSignKeyword;
    }

    public List<String> signKeywords() {
        return this.signKeywords;
    }

}
