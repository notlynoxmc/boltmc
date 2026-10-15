package dev.boltmc.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Writes colored boxes into a vertex buffer. Every face is written twice (front and back) so culling never hides it. */
final class Mesh {
    static final int FULL_BRIGHT = 0xF000F0;

    private final VertexConsumer vc;
    private final PoseStack.Pose pose;
    private final int baseLight;
    int light;

    Mesh(VertexConsumer vc, PoseStack.Pose pose, int light) {
        this.vc = vc; this.pose = pose; this.baseLight = light; this.light = light;
    }

    void emissive(boolean on) { light = on ? FULL_BRIGHT : baseLight; }

    // corner bits: 1 = x1, 2 = y1, 4 = z1
    private static final int[][] FACES = {
            {0, 1, 3, 2}, {5, 4, 6, 7}, {4, 0, 2, 6}, {1, 5, 7, 3}, {4, 5, 1, 0}, {2, 3, 7, 6}};
    private static final float[][] NORMALS = {
            {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}};

    /** Box from (x0,y0,z0) to (x1,y1,z1) in pixels; color goes from cA at x0 to cB at x1. */
    void box(Xf xf, float x0, float y0, float z0, float x1, float y1, float z1, int cA, int cB) {
        for (int f = 0; f < 6; f++) {
            float[] n = xf.rotate(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2]);
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    int idx = FACES[f][pass == 0 ? k : 3 - k];
                    boolean bx = (idx & 1) != 0;
                    float px = bx ? x1 : x0;
                    float py = (idx & 2) != 0 ? y1 : y0;
                    float pz = (idx & 4) != 0 ? z1 : z0;
                    float[] p = xf.apply(px, py, pz);
                    float s = pass == 0 ? 1f : -1f;
                    vc.addVertex(pose, p[0] / 16f, p[1] / 16f, p[2] / 16f)
                            .setColor(bx ? cB : cA)
                            .setUv(0.5f, 0.5f)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, n[0] * s, n[1] * s, n[2] * s);
                }
            }
        }
    }

    void box(Xf xf, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        box(xf, x0, y0, z0, x1, y1, z1, c, c);
    }
}
