package top.kzre.krro.d3.core.geometry;

import top.kzre.krro.util.math.Polynomial;
import top.kzre.krro.util.pool.DoublesHolder;
import top.kzre.krro.util.pool.DoublesPool;
import top.kzre.krro.util.pool.PoolManagers;

/**
 * Y 轴对齐的圆锥 / 截锥（GL 右手系，实体）。
 * 底面 y = cy−h/2 半径 rB，顶面 y = cy+h/2 半径 rT。
 */
public class Cone implements Primitive {

    private static final double EPS        = 1e-14;
    private static final double Y_EPS_REL  = 1e-9;
    private static final double R_EPS_REL  = 1e-9;
    private static final DoublesPool QUAD_POOL;
    static {
        DoublesHolder holder = PoolManagers.doubles().getHolder();
        QUAD_POOL = holder.getPool(2);
    }

    public final float cx, cy, cz;
    public final float radiusBottom;
    public final float radiusTop;
    public final float height;

    public Cone(float cx, float cy, float cz, float radius, float height) {
        this(cx, cy, cz, radius, 0f, height);
    }

    public Cone(float cx, float cy, float cz,
                float radiusBottom, float radiusTop, float height) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.radiusBottom = radiusBottom;
        this.radiusTop = radiusTop;
        this.height = height;
    }

    public static Cone apexUp(float cx, float cy, float cz, float radius, float height) {
        return new Cone(cx, cy, cz, radius, 0f, height);
    }

    public static Cone apexDown(float cx, float cy, float cz, float radius, float height) {
        return new Cone(cx, cy, cz, 0f, radius, height);
    }

    public static Cone frustum(float cx, float cy, float cz,
                               float rB, float rT, float height) {
        return new Cone(cx, cy, cz, rB, rT, height);
    }

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded()
                || !(height > 0f)
                || radiusBottom < 0f
                || radiusTop < 0f
                || (radiusBottom == 0f && radiusTop == 0f)) {
            return RayIntersection.miss();
        }

        final double h  = height;
        final double hHalf = 0.5 * h;
        final double rMax = Math.max(radiusBottom, radiusTop);

        if (AabbClipper.misses(ray,
                (double) cx - rMax, (double) cx + rMax,
                (double) cy - hHalf, (double) cy + hHalf,
                (double) cz - rMax, (double) cz + rMax)) {
            return RayIntersection.miss();
        }

        final double y0 = (double) cy - hHalf;
        final double y1 = (double) cy + hHalf;
        final double rB = radiusBottom;
        final double rT = radiusTop;
        final double k  = (rT - rB) / h;

        final double ox = (double) ray.ox - cx;
        final double oy = ray.oy;
        final double oz = (double) ray.oz - cz;
        final double dx = ray.dx;
        final double dy = ray.dy;
        final double dz = ray.dz;

        double bestT = Double.POSITIVE_INFINITY;
        double bestNx = 0, bestNy = 0, bestNz = 0;

        // ---------- 侧面二次 ----------
        final double Ly = oy - y0;
        final double R0 = rB + k * Ly;
        final double R1 = k * dy;

        final double a = dx * dx + dz * dz - R1 * R1;
        final double b = 2.0 * (ox * dx + oz * dz - R0 * R1);
        final double c = ox * ox + oz * oz - R0 * R0;

        final double yTol = Y_EPS_REL * Math.max(1.0, h);

        double[] roots = QUAD_POOL.acquire();
        try {
            int n = Polynomial.solveQuadratic(a, b, c, roots);
            for (int i = 0; i < n; i++) {
                double t = roots[i];
                if (!(t >= 0.0) || !Double.isFinite(t) || t >= bestT) continue;

                double y = oy + dy * t;
                if (y < y0 - yTol || y > y1 + yTol) continue;

                double px = ox + dx * t;
                double pz = oz + dz * t;
                double r  = rB + k * (y - y0);

                bestT = t;
                bestNx = px;
                bestNy = -r * k;
                bestNz = pz;
            }
        } finally {
            QUAD_POOL.release(roots);
        }

        // ---------- 底盖 ----------
        if (rB > 0.0 && Math.abs(dy) > EPS) {
            double t = (y0 - oy) / dy;
            if (t >= 0.0 && t < bestT) {
                double px = ox + dx * t;
                double pz = oz + dz * t;
                double r2 = px * px + pz * pz;
                double tol = R_EPS_REL * Math.max(1.0, rB * rB);
                if (r2 <= rB * rB + tol) {
                    bestT = t;
                    bestNx = 0; bestNy = -1; bestNz = 0;
                }
            }
        }

        // ---------- 顶盖 ----------
        if (rT > 0.0 && Math.abs(dy) > EPS) {
            double t = (y1 - oy) / dy;
            if (t >= 0.0 && t < bestT) {
                double px = ox + dx * t;
                double pz = oz + dz * t;
                double r2 = px * px + pz * pz;
                double tol = R_EPS_REL * Math.max(1.0, rT * rT);
                if (r2 <= rT * rT + tol) {
                    bestT = t;
                    bestNx = 0; bestNy = 1; bestNz = 0;
                }
            }
        }

        if (bestT == Double.POSITIVE_INFINITY) return RayIntersection.miss();

        double fx = ox + dx * bestT + cx;
        double fy = oy + dy * bestT;
        double fz = oz + dz * bestT + cz;

        double len = Math.sqrt(bestNx * bestNx + bestNy * bestNy + bestNz * bestNz);
        if (len < EPS) {
            bestNx = 0; bestNy = (fy >= cy) ? 1 : -1; bestNz = 0;
        } else {
            bestNx /= len; bestNy /= len; bestNz /= len;
        }

        return RayIntersection.hit((float) bestT,
                new Point((float) fx, (float) fy, (float) fz),
                new Normal((float) bestNx, (float) bestNy, (float) bestNz));
    }

    @Override
    public String toString() {
        return "Cone[(" + cx + ", " + cy + ", " + cz + ")"
                + " rB=" + radiusBottom + " rT=" + radiusTop
                + " h=" + height + "]";
    }
}