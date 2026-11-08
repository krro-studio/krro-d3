package top.kzre.krro.d3.core.geometry;

import top.kzre.krro.util.math.KMath;
import top.kzre.krro.util.math.Polynomial;
import top.kzre.krro.util.pool.DoublesHolder;
import top.kzre.krro.util.pool.DoublesPool;
import top.kzre.krro.util.pool.PoolManagers;

public final class Sphere implements Primitive {

    private static final DoublesPool QUAD_POOL;
    static {
        DoublesHolder holder = PoolManagers.doubles().getHolder();
        QUAD_POOL = holder.getPool(2);
    }

    public final float cx, cy, cz;
    public final float radius;

    public Sphere(float cx, float cy, float cz, float radius) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.radius = radius;
    }

    public static Sphere fromAABB(IAABB box) {
        float cx = box.centerX();
        float cy = box.centerY();
        float cz = box.centerZ();
        float dx = box.getMaxX() - box.getMinX();
        float dy = box.getMaxY() - box.getMinY();
        float dz = box.getMaxZ() - box.getMinZ();
        float radius = 0.5f * KMath.sqrt(dx * dx + dy * dy + dz * dz);
        return new Sphere(cx, cy, cz, radius);
    }

    public boolean contains(float x, float y, float z) {
        float dx = x - cx, dy = y - cy, dz = z - cz;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded() || !(radius > 0f)) return RayIntersection.miss();

        if (AabbClipper.misses(ray,
                (double) cx - radius, (double) cx + radius,
                (double) cy - radius, (double) cy + radius,
                (double) cz - radius, (double) cz + radius)) {
            return RayIntersection.miss();
        }

        final double lx = (double) ray.ox - cx;
        final double ly = (double) ray.oy - cy;
        final double lz = (double) ray.oz - cz;
        final double a = (double) ray.dx * ray.dx
                + (double) ray.dy * ray.dy
                + (double) ray.dz * ray.dz;
        final double b = 2.0 * (lx * ray.dx + ly * ray.dy + lz * ray.dz);
        final double c = lx * lx + ly * ly + lz * lz - (double) radius * radius;

        double bestT = Double.POSITIVE_INFINITY;
        double[] roots = QUAD_POOL.acquire();
        try {
            int n = Polynomial.solveQuadratic(a, b, c, roots);
            for (int i = 0; i < n; i++) {
                double t = roots[i];
                if (t >= 0.0 && t < bestT) bestT = t;
            }
        } finally {
            QUAD_POOL.release(roots);
        }
        if (bestT == Double.POSITIVE_INFINITY) return RayIntersection.miss();

        float tf = (float) bestT;
        float px = (float) ((double) ray.ox + (double) ray.dx * bestT);
        float py = (float) ((double) ray.oy + (double) ray.dy * bestT);
        float pz = (float) ((double) ray.oz + (double) ray.dz * bestT);

        float invR = 1f / radius;
        return RayIntersection.hit(tf,
                new Point(px, py, pz),
                new Normal((px - cx) * invR, (py - cy) * invR, (pz - cz) * invR));
    }

    @Override
    public String toString() {
        return "Sphere[(" + cx + ", " + cy + ", " + cz + ") r=" + radius + "]";
    }
}