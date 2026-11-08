package top.kzre.krro.d3.core.geometry;

import top.kzre.krro.util.math.Polynomial;
import top.kzre.krro.util.pool.DoublesHolder;
import top.kzre.krro.util.pool.DoublesPool;
import top.kzre.krro.util.pool.PoolManagers;

/**
 * Y 轴对齐的环面（torus），GL 右手系。
 */
public class Torus implements Primitive {

    private static final double EPS      = 1e-14;
    private static final double ROOT_TOL = 1e-5;

    private static final DoublesPool QUARTIC_POOL;
    static {
        DoublesHolder holder = PoolManagers.doubles().getHolder();
        QUARTIC_POOL = holder.getPool(4);
    }

    public final float cx, cy, cz;
    public final float majorRadius;
    public final float tubeRadiusA;
    public final float tubeRadiusB;

    public Torus(float cx, float cy, float cz,
                 float majorRadius, float tubeRadius) {
        this(cx, cy, cz, majorRadius, tubeRadius, tubeRadius);
    }

    public Torus(float cx, float cy, float cz,
                 float majorRadius, float tubeRadiusA, float tubeRadiusB) {
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.majorRadius = majorRadius;
        this.tubeRadiusA = tubeRadiusA;
        this.tubeRadiusB = tubeRadiusB;
    }

    @Override
    public RayIntersection intersect(Ray ray) {
        if (ray.isDegraded()) return RayIntersection.miss();
        if (!(majorRadius > 0f) || !(tubeRadiusA > 0f) || !(tubeRadiusB > 0f)) {
            return RayIntersection.miss();
        }

        final double ext = (double) majorRadius + tubeRadiusA;
        if (AabbClipper.misses(ray,
                (double) cx - ext, (double) cx + ext,
                (double) cy - tubeRadiusB, (double) cy + tubeRadiusB,
                (double) cz - ext, (double) cz + ext)) {
            return RayIntersection.miss();
        }

        final double ox = (double) ray.ox - cx;
        final double oy = ray.oy;
        final double oz = (double) ray.oz - cz;
        final double dx = ray.dx;
        final double dy = ray.dy;
        final double dz = ray.dz;

        final double a  = tubeRadiusA;
        final double b  = tubeRadiusB;
        final double R  = majorRadius;
        final double a2 = a * a, b2 = b * b, R2 = R * R;

        final double A = dx * dx + dz * dz;
        final double B = 2.0 * (ox * dx + oz * dz);
        final double C = ox * ox + oz * oz;

        final double D = dy * dy;
        final double E = 2.0 * oy * dy;
        final double F = oy * oy;

        final double K4 = b2 * A + a2 * D;
        final double K3 = b2 * B + a2 * E;
        final double K2 = b2 * C + a2 * F + b2 * R2 - a2 * b2;

        final double fourB4R2 = 4.0 * b2 * b2 * R2;
        final double C4 = K4 * K4;
        final double C3 = 2.0 * K4 * K3;
        final double C2 = K3 * K3 + 2.0 * K4 * K2 - fourB4R2 * A;
        final double C1 = 2.0 * K3 * K2 - fourB4R2 * B;
        final double C0 = K2 * K2 - fourB4R2 * C;

        double bestT = Double.POSITIVE_INFINITY;
        double[] roots = QUARTIC_POOL.acquire();
        try {
            int n = Polynomial.solveQuartic(C4, C3, C2, C1, C0, roots);
            for (int i = 0; i < n; i++) {
                double t = roots[i];
                if (!(t >= 0.0) || !Double.isFinite(t) || t >= bestT) continue;

                double f = K4 * t * t + K3 * t + K2;
                if (f < -EPS * Math.max(1.0, Math.abs(K2))) continue;

                double y    = oy + dy * t;
                double px   = ox + dx * t;
                double pz   = oz + dz * t;
                double rho2 = px * px + pz * pz;
                double rho  = Math.sqrt(rho2);
                double dr   = rho - R;
                double val  = (dr * dr) / a2 + (y * y) / b2;
                if (Math.abs(val - 1.0) > ROOT_TOL) continue;

                bestT = t;
            }
        } finally {
            QUARTIC_POOL.release(roots);
        }
        if (bestT == Double.POSITIVE_INFINITY) return RayIntersection.miss();

        double hx = ox + dx * bestT;
        double hy = oy + dy * bestT;
        double hz = oz + dz * bestT;
        double rho = Math.sqrt(hx * hx + hz * hz);

        double nx, ny, nz;
        if (rho > EPS) {
            double k = (rho - R) / a2 / rho;
            nx = k * hx;
            nz = k * hz;
            ny = hy / b2;
        } else {
            nx = 0.0; nz = 0.0; ny = (hy >= 0.0) ? 1.0 : -1.0;
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < EPS) {
            nx = 0.0; ny = 1.0; nz = 0.0;
        } else {
            nx /= len; ny /= len; nz /= len;
        }

        return RayIntersection.hit((float) bestT,
                new Point((float) (hx + cx), (float) hy, (float) (hz + cz)),
                new Normal((float) nx, (float) ny, (float) nz));
    }

    @Override
    public String toString() {
        return "Torus[(" + cx + ", " + cy + ", " + cz + ")"
                + " R=" + majorRadius
                + " a=" + tubeRadiusA + " b=" + tubeRadiusB + "]";
    }
}