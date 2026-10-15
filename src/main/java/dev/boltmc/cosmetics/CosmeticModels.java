package dev.boltmc.cosmetics;

/**
 * The 3D shapes. Everything is built from boxes in vanilla player-model space (pixels, y points DOWN,
 * the face looks toward -z, the back is +z, the head box spans y -8..0 and the torso y 0..12).
 */
final class CosmeticModels {
    private CosmeticModels() {}

    private static final float DEG = (float) (Math.PI / 180.0);

    static int shade(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * f));
        int b = Math.min(255, Math.round((rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ wings (small, flapping gently)

    private static final float[] FEATHER_ANGLE = {-64, -46, -29, -13, 3, 19};
    private static final float[] FEATHER_LEN = {7f, 9f, 10.5f, 10f, 8.5f, 6.5f};

    static void wings(Mesh m, Xf body, int tint, long ms) {
        float spread = 24f + (float) Math.sin(ms / 260.0) * 6f;
        int dark = shade(tint, 0.72f), mid = shade(tint, 0.9f), light = shade(tint, 1.0f);
        for (int side = 1; side >= -1; side -= 2) {
            Xf root = body.copy().scaleLocal(side, 1, 1).translate(1.4f, 1.2f, 3.0f).ry(-spread * DEG);
            m.box(root.copy().rz(-58 * DEG), 0, -0.9f, 0, 5.5f, 0.9f, 1.1f, dark, dark); // shoulder bone
            for (int i = 0; i < FEATHER_ANGLE.length; i++) {
                float a = FEATHER_ANGLE[i] * DEG;
                float len = FEATHER_LEN[i];
                m.box(root.copy().rz(a), 0, -1.1f, 0.1f, len, 1.1f, 0.9f, dark, mid);              // long feather
                m.box(root.copy().rz(a + 4 * DEG), 0.8f, -1.0f, 0.9f, len * 0.62f, 1.0f, 1.5f, mid, light); // covert
            }
        }
    }

    // ------------------------------------------------------------------ halo

    static void halo(Mesh m, Xf head, int tint, long ms, float height) {
        Xf h = head.copy().translate(0, height + (float) Math.sin(ms / 400.0) * 0.5f, 0).rz(0.05f);
        int a = shade(tint, 1.0f), b = shade(tint, 0.85f);
        m.emissive(true);
        m.box(h, -4.5f, 0, -4.5f, 4.5f, 0.9f, -3.5f, a);
        m.box(h, -4.5f, 0, 3.5f, 4.5f, 0.9f, 4.5f, b);
        m.box(h, -4.5f, 0, -3.5f, -3.5f, 0.9f, 3.5f, a);
        m.box(h, 3.5f, 0, -3.5f, 4.5f, 0.9f, 3.5f, b);
        m.emissive(false);
    }

    // ------------------------------------------------------------------ hats

    static void crown(Mesh m, Xf head, int tint) {
        int gold = shade(tint, 1.0f), dark = shade(tint, 0.78f);
        m.box(head, -4.5f, -10.2f, -4.6f, 4.5f, -8.1f, -3.8f, gold);
        m.box(head, -4.5f, -10.2f, 3.8f, 4.5f, -8.1f, 4.6f, gold);
        m.box(head, -4.6f, -10.2f, -3.8f, -3.8f, -8.1f, 3.8f, dark);
        m.box(head, 3.8f, -10.2f, -3.8f, 4.6f, -8.1f, 3.8f, dark);
        float[] xs = {-3.6f, -1.2f, 1.2f, 3.6f};
        for (float x : xs) {
            m.box(head, x - 0.7f, -12.4f, -4.6f, x + 0.7f, -10.2f, -3.8f, gold);
            m.box(head, x - 0.7f, -12.4f, 3.8f, x + 0.7f, -10.2f, 4.6f, gold);
        }
        for (float z : new float[]{-1.2f, 1.2f}) {
            m.box(head, -4.6f, -12.4f, z - 0.7f, -3.8f, -10.2f, z + 0.7f, dark);
            m.box(head, 3.8f, -12.4f, z - 0.7f, 4.6f, -10.2f, z + 0.7f, dark);
        }
        m.box(head, -0.8f, -9.8f, -4.85f, 0.8f, -8.6f, -4.55f, 0xFFE53935); // red gem
    }

    static void cap(Mesh m, Xf head, int tint) {
        int main = shade(tint, 1.0f), dark = shade(tint, 0.78f);
        m.box(head, -4.6f, -9.0f, -4.6f, 4.6f, -7.2f, 4.6f, main);
        m.box(head, -3.9f, -10.1f, -3.9f, 3.9f, -9.0f, 3.9f, main);
        m.box(head, -0.5f, -10.6f, -0.5f, 0.5f, -10.1f, 0.5f, dark);               // button
        m.box(head.copy().rx(0.18f), -4.4f, -8.3f, -9.2f, 4.4f, -7.6f, -4.6f, dark); // brim
    }

    static void propeller(Mesh m, Xf head, int tint, long ms) {
        int main = shade(tint, 1.0f), dark = shade(tint, 0.8f);
        m.box(head, -4.6f, -9.0f, -4.6f, 4.6f, -7.2f, 4.6f, main);
        m.box(head, -3.9f, -10.1f, -3.9f, 3.9f, -9.0f, 3.9f, dark);
        m.box(head, -0.4f, -11.5f, -0.4f, 0.4f, -10.1f, 0.4f, 0xFF9AA0A8);        // pole
        float spin = (ms % 600L) / 600f * (float) (Math.PI * 2);
        Xf blade = head.copy().translate(0, -11.5f, 0).ry(spin);
        m.box(blade, -5f, -0.5f, -0.9f, 5f, 0f, 0.9f, shade(tint, 1.3f));
        m.box(blade.copy().ry((float) (Math.PI / 2)), -5f, -0.5f, -0.9f, 5f, 0f, 0.9f, 0xFFF2F3F5);
    }

    static void topHat(Mesh m, Xf head, int tint) {
        int main = shade(tint, 1.0f);
        m.box(head, -6f, -8.5f, -6f, 6f, -7.7f, 6f, shade(tint, 0.9f));
        m.box(head, -3.8f, -14.8f, -3.8f, 3.8f, -8.5f, 3.8f, main);
        m.box(head, -3.95f, -10.0f, -3.95f, 3.95f, -8.6f, 3.95f, 0xFFC62828);     // band
    }

    static void catEars(Mesh m, Xf head, int tint) {
        int main = shade(tint, 1.0f), tip = shade(tint, 0.85f), pink = 0xFFFF9EC4;
        for (int side = 1; side >= -1; side -= 2) {
            Xf e = head.copy().scaleLocal(side, 1, 1);
            m.box(e, 1.0f, -9.2f, -2.4f, 3.8f, -7.9f, -0.6f, main);
            m.box(e, 1.5f, -10.4f, -2.0f, 3.4f, -9.2f, -1.0f, main);
            m.box(e, 2.0f, -11.4f, -1.8f, 3.0f, -10.4f, -1.2f, tip);
            m.box(e, 1.5f, -9.0f, -2.5f, 3.3f, -8.1f, -2.2f, pink);
            m.box(e, 1.9f, -10.2f, -2.1f, 3.0f, -9.3f, -1.9f, pink);
        }
    }
}
