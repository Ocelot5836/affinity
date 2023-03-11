package io.wispforest.affinity.client.render.blockentity;

import io.wispforest.affinity.block.impl.ItemTransferNodeBlock;
import io.wispforest.affinity.blockentity.impl.ItemTransferNodeBlockEntity;
import io.wispforest.affinity.item.IridescenceWandItem;
import io.wispforest.owo.ui.util.Delta;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class ItemTransferNodeBlockEntityRenderer implements BlockEntityRenderer<ItemTransferNodeBlockEntity>, RotatingItemRenderer {

    private static float linkAlpha = 0f;

    public ItemTransferNodeBlockEntityRenderer(BlockEntityRendererFactory.Context context) {}

    @Override
    public void render(ItemTransferNodeBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (linkAlpha > .05f) {
            var nodeLinkPos = nodeLinkPos(entity);

            for (var link : entity.links()) {
                if (!(entity.getWorld().getBlockEntity(link) instanceof ItemTransferNodeBlockEntity node)) continue;
                LinkRenderer.addLink(nodeLinkPos, nodeLinkPos(node), ((int) (0xFF * linkAlpha) << 24) | 0x00FFAB);
            }
        }

        matrices.push();

        matrices.translate(.5, .5, .5);
        matrices.multiply(entity.getCachedState().get(ItemTransferNodeBlock.FACING).getOpposite().getRotationQuaternion());
        matrices.translate(-.5, -.5, -.5);

        final var stack = entity.getItem();
        if (!stack.isEmpty()) {
            this.renderItem(
                    entity, matrices, vertexConsumers, stack,
                    3000, .5f,
                    .5f,
                    .335f,
                    .5f,
                    light, overlay
            );
        }

        final var filterStack = entity.getFilterItem();
        if (!filterStack.isEmpty()) {
            final var client = MinecraftClient.getInstance();
            final var depthModel = client.getItemRenderer().getModel(filterStack, client.world, null, 0).hasDepth();

            matrices.translate(.5, .255, .5325);

            if (depthModel) {
                matrices.scale(1.2f, 1.2f, 1.2f);
                matrices.translate(0, -.01, .02);
            }

            matrices.scale(.25f, .25f, .25f);
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));

            MinecraftClient.getInstance().getItemRenderer().renderItem(filterStack, ModelTransformation.Mode.GROUND, light, overlay, matrices, vertexConsumers, 0);
        }

        matrices.pop();
    }

    private static Vec3d nodeLinkPos(ItemTransferNodeBlockEntity node) {
        var facing = node.getCachedState().get(ItemTransferNodeBlock.FACING);
        return new Vec3d(
                node.getPos().getX() + .5 + facing.getOffsetX() * .3,
                node.getPos().getY() + .5 + facing.getOffsetY() * .3,
                node.getPos().getZ() + .5 + facing.getOffsetZ() * .3
        );
    }

    static {
        WorldRenderEvents.LAST.register(context -> {
            var client = MinecraftClient.getInstance();
            if (client.player == null) return;

            linkAlpha += Delta.compute(
                    linkAlpha,
                    client.player.isHolding(stack -> stack.getItem() instanceof IridescenceWandItem) ? 1f : 0f,
                    client.getLastFrameDuration() * .25f
            );
        });
    }
}
