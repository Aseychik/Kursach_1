import java.awt.*;

public class Point2D {
    double x, y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point2D() {
        x = 0;
        y = 0;
    }

    public Point2D(Point point) {
        x = point.x;
        y = point.y;
    }

    public static Point2D multiply(Point2D point, double k){
        return new Point2D(point.x * k, point.y * k);
    }

    public static Point2D add(Point2D a, Point2D b) {
        return new Point2D(a.x + b.x, a.y + b.y);
    }

    public static Point2D div(Point2D a, Point2D b) { return new Point2D(a.x - b.x, a.y - b.y); }

    public double length() { return Math.sqrt(x * x + y * y); }

    public Point2D normalize() throws Exception {
        double l = length();
        if (l == 0) throw new Exception("Attempt of normalizing zero length vector");

        return Point2D.multiply(this, 1 / l);
    }

    public Point toIntegerPoint(int type) {
        if (type == 2) return new Point((int)Math.ceil(x), (int)Math.ceil(y));
        if (type == 1) return new Point((int)Math.round(x), (int)Math.round(y));
        return new Point((int)x, (int)y);
    }

    @Override
    public String toString() {
        return "Point2D(" + x + " " + y + ")";
    }
}
