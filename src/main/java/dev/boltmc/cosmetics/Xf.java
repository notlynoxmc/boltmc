package dev.boltmc.cosmetics;

/** Tiny 3x3 matrix + translation, used to place and rotate the cosmetic boxes (model space, pixels). */
final class Xf {
    final float[] m = {1, 0, 0, 0, 1, 0, 0, 0, 1};
    float x, y, z;

    Xf copy() {
        Xf c = new Xf();
        System.arraycopy(m, 0, c.m, 0, 9);
        c.x = x; c.y = y; c.z = z;
        return c;
    }

    /** Moves along the current local axes. */
    Xf translate(float dx, float dy, float dz) {
        x += m[0] * dx + m[1] * dy + m[2] * dz;
        y += m[3] * dx + m[4] * dy + m[5] * dz;
        z += m[6] * dx + m[7] * dy + m[8] * dz;
        return this;
    }

    private Xf mulRot(float[] r) {
        float[] n = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                n[i * 3 + j] = m[i * 3] * r[j] + m[i * 3 + 1] * r[3 + j] + m[i * 3 + 2] * r[6 + j];
        System.arraycopy(n, 0, m, 0, 9);
        return this;
    }

    Xf rx(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mulRot(new float[]{1, 0, 0, 0, c, -s, 0, s, c}); }
    Xf ry(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mulRot(new float[]{c, 0, s, 0, 1, 0, -s, 0, c}); }
    Xf rz(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mulRot(new float[]{c, -s, 0, s, c, 0, 0, 0, 1}); }

    /** Same order as the vanilla ModelPart: Z, then Y, then X. */
    Xf rotZYX(float z, float y, float x) { return rz(z).ry(y).rx(x); }

    Xf scaleLocal(float sx, float sy, float sz) {
        for (int i = 0; i < 3; i++) { m[i * 3] *= sx; m[i * 3 + 1] *= sy; m[i * 3 + 2] *= sz; }
        return this;
    }

    float[] apply(float px, float py, float pz) {
        return new float[]{
                m[0] * px + m[1] * py + m[2] * pz + x,
                m[3] * px + m[4] * py + m[5] * pz + y,
                m[6] * px + m[7] * py + m[8] * pz + z};
    }

    float[] rotate(float nx, float ny, float nz) {
        return new float[]{
                m[0] * nx + m[1] * ny + m[2] * nz,
                m[3] * nx + m[4] * ny + m[5] * nz,
                m[6] * nx + m[7] * ny + m[8] * nz};
    }
}
