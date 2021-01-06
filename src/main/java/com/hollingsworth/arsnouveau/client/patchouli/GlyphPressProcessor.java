package com.hollingsworth.arsnouveau.client.patchouli;

import com.hollingsworth.arsnouveau.api.recipe.GlyphPressRecipe;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

public class GlyphPressProcessor implements IComponentProcessor {

    GlyphPressRecipe recipe;
    @Override
    public void setup(IVariableProvider variables) {
        RecipeManager manager = Minecraft.getInstance().world.getRecipeManager();
        String recipeID = variables.get("recipe").asString();
        recipe = (GlyphPressRecipe) manager.getRecipe(new ResourceLocation(recipeID)).orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public IVariable process(String s) {
        System.out.println(s);
        if(s.equals("clay_type"))
            return IVariable.from(recipe.getClay());
        if(s.equals("reagent"))
            return IVariable.from(recipe.reagent);
        if(s.equals("tier"))
            return IVariable.wrap(recipe.tier.toString());
        if(s.equals("mana_cost") ){
            if(recipe.output.getItem() instanceof Glyph){
                int cost =  ((Glyph) recipe.output.getItem()).spellPart.getManaCost();
                String costLang = "";
                if(cost == 0)
                    costLang = "None";
                if(cost < 20)
                    costLang = "Low";
                if(cost < 50)
                    costLang = "Medium";
                if(cost >= 50)
                    costLang = "High";
                return IVariable.wrap(costLang);
            }
            return IVariable.wrap("");
        }



        return null;
    }
}
