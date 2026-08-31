package com.kompi.orelocator.mixin;

import com.kompi.orelocator.event.HighlightedOreStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {

    // Указываем как человекочитаемое имя, так и SRG-имя метода в 1.19.2 (m_5577_)
    @Inject(
            method = {"shouldRenderFace", "m_5577_"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onShouldRenderFace(BlockState state, BlockGetter level, BlockPos pos, Direction side,
                                           BlockPos neighborPos, CallbackInfoReturnable<Boolean> cir) {
        if (HighlightedOreStorage.isHighlighted(neighborPos) || HighlightedOreStorage.isHighlighted(pos)) {
            cir.setReturnValue(true);
        }
    }
}