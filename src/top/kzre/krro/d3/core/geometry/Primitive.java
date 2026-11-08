package top.kzre.krro.d3.core.geometry;

/**
 * 可以用解析法表达的基元形状
 */
public interface Primitive {
    RayIntersection intersect(Ray ray);
}
