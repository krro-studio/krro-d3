package top.kzre.krro.d3.core.geometry;

import top.kzre.krro.util.math.Polynomial;
import top.kzre.krro.util.pool.DoublesHolder;
import top.kzre.krro.util.pool.DoublesPool;
import top.kzre.krro.util.pool.PoolManagers;

/**
 * Y 轴对齐的有限圆柱（中心 (cx,cy,cz)、半径 radius、总高 height）。
 */
public final class Cylinder implements Primitive {

    private static final double EPS = 1e-14;
    private static final DoublesPool QUAD_POOL;
    static {
        DoublesHolder holder = PoolManagers.doubles().getHolder();
        QUAD_POOL = holder.getPool(2);
    }

    public final float cx, cy, cz;
    public final float radius;
    public final float height;

    public Cylinder(float cx, float cy, float cz, float radius, float height) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.radius = radius; this.height = height;
    }

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded() || !(radius > 0f) || !(height > 0f)) {
            return RayIntersection.miss();
        }

        final double hHalf = 0.5 * (double) height;

        if (AabbClipper.misses(ray,
                (double) cx - radius, (double) cx + radius,
                (double) cy - hHalf, (double) cy + hHalf,
                (double) cz - radius, (double) cz + radius)) {
            return RayIntersection.miss();
        }

        final double yMin = (double) cy - hHalf;
        final double yMax = (double) cy + hHalf;

        final double ox = (double) ray.ox - cx;
        final double oy = ray.oy;
        final double oz = (double) ray.oz - cz;
        final double dx = ray.dx;
        final double dy = ray.dy;
        final double dz = ray.dz;

        // 侧面：径向二次
        final double a = dx * dx + dz * dz;
        final double bHalf = ox * dx + oz * dz;
        final double c = ox * ox + oz * oz - (double) radius * radius;

        double tCylMin, tCylMax;
        if (a < EPS) {
            if (c > 0.0) return RayIntersection.miss();
            tCylMin = Double.NEGATIVE_INFINITY;
            tCylMax = Double.POSITIVE_INFINITY;
        } else {
            double[] roots = QUAD_POOL.acquire();
            try {
                int n = Polynomial.solveQuadratic(a, 2.0 * bHalf, c, roots);
                if (n == 0) return RayIntersection.miss();
                if (n == 1) {
                    tCylMin = tCylMax = roots[0];
                } else {
                    tCylMin = Math.min(roots[0], roots[1]);
                    tCylMax = Math.max(roots[0], roots[1]);
                }
            } finally {
                QUAD_POOL.release(roots);
            }
        }

        // slab 裁剪
        double tSlabMin, tSlabMax;
        if (Math.abs(dy) < EPS) {
            if (oy < yMin || oy > yMax) return RayIntersection.miss();
            tSlabMin = Double.NEGATIVE_INFINITY;
            tSlabMax = Double.POSITIVE_INFINITY;
        } else {
            double ta = (yMin - oy) / dy;
            double tb = (yMax - oy) / dy;
            if (ta > tb) { double tmp = ta; ta = tb; tb = tmp; }
            tSlabMin = ta;
            tSlabMax = tb;
        }

        final double tEnter = Math.max(tCylMin, tSlabMin);
        final double tExit  = Math.min(tCylMax, tSlabMax);
        if (tEnter > tExit || tExit < 0.0) return RayIntersection.miss();

        final double t;
        final boolean onLateral;
        if (tEnter >= 0.0) {
            t = tEnter;
            onLateral = tCylMin >= tSlabMin;
        } else {
            t = tExit;
            onLateral = tCylMax <= tSlabMax;
        }

        float tf = (float) t;
        float px = (float) ((double) ray.ox + (double) ray.dx * t);
        float py = (float) ((double) ray.oy + (double) ray.dy * t);
        float pz = (float) ((double) ray.oz + (double) ray.dz * t);

        float nx, ny, nz;
        if (onLateral) {
            double rx = (double) px - cx;
            double rz = (double) pz - cz;
            double len = Math.sqrt(rx * rx + rz * rz);
            if (len < EPS) {
                nx = 0f; ny = 1f; nz = 0f;
            } else {
                nx = (float) (rx / len);
                ny = 0f;
                nz = (float) (rz / len);
            }
        } else {
            nx = 0f; nz = 0f;
            ny = (py < cy) ? -1f : 1f;
        }

        return RayIntersection.hit(tf, new Point(px, py, pz),
                new Normal(nx, ny, nz));
    }

    @Override
    public String toString() {
        return "Cylinder[(" + cx + ", " + cy + ", " + cz + ")"
                + " r=" + radius + " h=" + height + "]";
    }
}