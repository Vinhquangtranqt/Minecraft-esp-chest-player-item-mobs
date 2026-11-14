package com.kodari.esp.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("DEBUG_FILLED_SNIPPET")
    static RenderPipeline.Snippet esp$getDebugFilledSnippet() {
        throw new AssertionError();
    }

    @Accessor("LINES_SNIPPET")
    static RenderPipeline.Snippet esp$getLinesSnippet() {
        throw new AssertionError();
    }

    @Invoker("register")
    static RenderPipeline esp$register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}