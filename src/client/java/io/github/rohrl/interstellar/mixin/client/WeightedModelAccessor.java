package io.github.rohrl.interstellar.mixin.client;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.util.collection.Weighted;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(WeightedBakedModel.class)
public interface WeightedModelAccessor {
    @Accessor("models") List<Weighted.Present<BakedModel>> interstellar$models();
}
