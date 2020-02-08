package com.hollingsworth.craftedmagic.spell.augment;

import com.hollingsworth.craftedmagic.ModConfig;

public class AugmentFortune extends AugmentType{
    public AugmentFortune() {
        super(ModConfig.AugmentFortuneID, "Fortune");
    }

    @Override
    public int getManaCost() {
        return 0;
    }
}
