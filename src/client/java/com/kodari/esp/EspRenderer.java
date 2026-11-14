package com.kodari.esp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kodari.esp.mixin.RenderPipelinesAccessor;
import com.kodari.esp.mixin.RenderTypeAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class EspRenderer {
    private static final Color CHEST_COLOR = new Color(255, 135, 35);
    private static final Color TRAPPED_CHEST_COLOR = new Color(255, 45, 45);
    private static final Color ENDER_CHEST_COLOR = new Color(185, 70, 255);
    private static final Color HOSTILE_COLOR = new Color(255, 55, 55);
    private static final Color FRIENDLY_COLOR = new Color(55, 235, 90);
    private static final Color PLAYER_COLOR = new Color(65, 135, 255);
    private static final int[][] EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int[][] FACES = {
            {0, 2, 3, 1}, {4, 5, 7, 6},
            {0, 4, 6, 2}, {1, 3, 7, 5},
            {0, 1, 5, 4}, {2, 6, 7, 3}
    };

    private static volatile List<Target> targets = List.of();
    private static RenderType lineRenderType;
    private static RenderType fillRenderType;

    private EspRenderer() {
    }

    static void initialize() {
        RenderPipeline fillPipeline = RenderPipeline.builder(RenderPipelinesAccessor.esp$getDebugFilledSnippet())
                .withLocation(Identifier.fromNamespaceAndPath("esp", "through_wall_filled"))
                .withDepthStencilState(Optional.empty())
                .build();
        RenderPipeline linePipeline = RenderPipeline.builder(RenderPipelinesAccessor.esp$getLinesSnippet())
                .withLocation(Identifier.fromNamespaceAndPath("esp", "through_wall_lines"))
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.empty())
                .build();

        RenderPipelinesAccessor.esp$register(fillPipeline);
        RenderPipelinesAccessor.esp$register(linePipeline);
        fillRenderType = RenderTypeAccessor.esp$create(
                "esp_through_wall_filled",
                RenderSetup.builder(fillPipeline).createRenderSetup()
        );
        lineRenderType = RenderTypeAccessor.esp$create(
                "esp_through_wall_lines",
                RenderSetup.builder(linePipeline).createRenderSetup()
        );

        LevelExtractionEvents.END_EXTRACTION.register(EspRenderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(EspRenderer::submit);
    }

    private static void extract(LevelExtractionContext context) {
        if (!EspClient.enabled) {
            targets = List.of();
            return;
        }

        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            targets = List.of();
            return;
        }

        List<Target> extracted = new ArrayList<>();

        if (EspClient.chestsEnabled) {
            for (BlockEntityRenderState renderState : context.levelState().blockEntityRenderStates) {
                if (renderState instanceof ChestRenderState chestState) {
                    Color color = switch (chestState.material) {
                        case REGULAR, CHRISTMAS -> CHEST_COLOR;
                        case TRAPPED -> TRAPPED_CHEST_COLOR;
                        case ENDER_CHEST -> ENDER_CHEST_COLOR;
                        default -> null;
                    };
                    if (color != null) {
                        BlockPos pos = chestState.blockPos;
                        extracted.add(new Target(
                                pos.getX(), pos.getY(), pos.getZ(),
                                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0,
                                color
                        ));
                    }
                }
            }
        }

        ClientLevel level = context.level();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Player player) {
                if (EspClient.playersEnabled && player != localPlayer) {
                    extracted.add(target(entity.getBoundingBox(), PLAYER_COLOR));
                }
                continue;
            }

            if (!EspClient.entitiesEnabled) {
                continue;
            }

            if (entity instanceof Monster) {
                extracted.add(target(entity.getBoundingBox(), HOSTILE_COLOR));
            } else if (entity instanceof Animal || entity instanceof Villager) {
                extracted.add(target(entity.getBoundingBox(), FRIENDLY_COLOR));
            }
        }

        targets = List.copyOf(extracted);
    }

    private static Target target(AABB box, Color color) {
        return new Target(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
    }

    private static void submit(LevelRenderContext context) {
        List<Target> frameTargets = targets;
        if (!EspClient.enabled || frameTargets.isEmpty()) {
            return;
        }

        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        EspClient.DisplayMode mode = EspClient.displayMode;
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Point camera = new Point(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        for (Target target : frameTargets) {
            switch (mode) {
                case BOX -> collector.submitCustomGeometry(poseStack, lineRenderType,
                        (pose, buffer) -> drawBox(pose, buffer, target));
                case HIGHLIGHT -> collector.submitCustomGeometry(poseStack, fillRenderType,
                        (pose, buffer) -> drawHighlight(pose, buffer, target));
                case TRACER -> collector.submitCustomGeometry(poseStack, lineRenderType,
                        (pose, buffer) -> drawTracer(pose, buffer, target, camera));
            }
        }
    }

    private static void drawBox(PoseStack.Pose pose, VertexConsumer buffer, Target target) {
        float[][] corners = corners(target);
        for (int[] edge : EDGES) {
            lineVertex(pose, buffer, corners[edge[0]], target.color);
            lineVertex(pose, buffer, corners[edge[1]], target.color);
        }
    }

    private static void drawTracer(PoseStack.Pose pose, VertexConsumer buffer, Target target, Point camera) {
        double targetX = (target.minX + target.maxX) * 0.5;
        double targetY = (target.minY + target.maxY) * 0.5;
        double targetZ = (target.minZ + target.maxZ) * 0.5;
        lineVertex(pose, buffer, new double[]{camera.x, camera.y, camera.z}, target.color);
        lineVertex(pose, buffer, new double[]{targetX, targetY, targetZ}, target.color);
    }

    private static void drawHighlight(PoseStack.Pose pose, VertexConsumer buffer, Target target) {
        float[][] corners = corners(target);
        Color color = target.color;
        for (int[] face : FACES) {
            for (int index : face) {
                float[] corner = corners[index];
                buffer.addVertex(pose, corner[0], corner[1], corner[2])
                        .setColor(color.red, color.green, color.blue, 64);
            }
        }
    }

    private static void lineVertex(PoseStack.Pose pose, VertexConsumer buffer, float[] point, Color color) {
        buffer.addVertex(pose, point[0], point[1], point[2])
                .setColor(color.red, color.green, color.blue, 255)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                .setLineWidth(2.0F);
    }

    private static void lineVertex(PoseStack.Pose pose, VertexConsumer buffer, double[] point, Color color) {
        buffer.addVertex(pose, (float) point[0], (float) point[1], (float) point[2])
                .setColor(color.red, color.green, color.blue, 255)
                .setNormal(pose, 0.0F, 1.0F, 0.0F)
                .setLineWidth(2.0F);
    }

    private static float[][] corners(Target target) {
        return new float[][]{
                {(float) target.minX, (float) target.minY, (float) target.minZ},
                {(float) target.maxX, (float) target.minY, (float) target.minZ},
                {(float) target.minX, (float) target.maxY, (float) target.minZ},
                {(float) target.maxX, (float) target.maxY, (float) target.minZ},
                {(float) target.minX, (float) target.minY, (float) target.maxZ},
                {(float) target.maxX, (float) target.minY, (float) target.maxZ},
                {(float) target.minX, (float) target.maxY, (float) target.maxZ},
                {(float) target.maxX, (float) target.maxY, (float) target.maxZ}
        };
    }

    private record Color(int red, int green, int blue) {
    }

    private record Point(double x, double y, double z) {
    }

    private record Target(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color) {
    }
}