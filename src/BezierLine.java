import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BezierLine {
    public List<Point2D> points;

    private double[] worldX;
    private double[] worldY;
    boolean needsWorldUpdate = true;

    private int[] screenX;
    private int[] screenY;
    private double lastScale = -1;
    private double lastCamX = Double.NaN;
    private double lastCamY = Double.NaN;

    private double[] calcBufferX;
    private double[] calcBufferY;

    int[] pointSize = new int[]{12, 12};
    Color lineColor = Color.black, pointColor = Color.green;
    int is_composite = 0;
    Point2D[][] joined_points = new Point2D[2][2];
    BezierLine[] joinedLines = new BezierLine[2];


    boolean is_on_line = false;
    Point2D[] res_points;


    public void markDirty(boolean b) {
        needsWorldUpdate = true;
        if (b) {
            if (joinedLines[0] != null) joinedLines[0].markDirty(false);
            if (joinedLines[1] != null) joinedLines[1].markDirty(false);
        }
    }

    public void find_points() {
        if (points.size() != 4) return;
        res_points = new Point2D[4];
        res_points[0] = points.getFirst();
        res_points[3] = points.getLast();

        double t1 = 0.33;
        double t2 = 0.67;

        double A1 = Math.pow(1 - t1, 3);
        double B1 = 3 * Math.pow(1 - t1, 2) * t1;
        double C1 = 3 * (1 - t1) * Math.pow(t1, 2);
        double D1 = Math.pow(t1, 3);

        double A2 = Math.pow(1 - t2, 3);
        double B2 = 3 * Math.pow(1 - t2, 2) * t2;
        double C2 = 3 * (1 - t2) * Math.pow(t2, 2);
        double D2 = Math.pow(t2, 3);

        double det = B1 * C2 - B2 * C1;

        double right1x = points.get(1).x - A1 * res_points[0].x - D1 * res_points[3].x;
        double right2x = points.get(2).x - A2 * res_points[0].x - D2 * res_points[3].x;

        double x1 = (right1x * C2 - right2x * C1) / det;
        double x2 = (B1 * right2x - B2 * right1x) / det;

        double right1y = points.get(1).y - A1 * res_points[0].y - D1 * res_points[3].y;
        double right2y = points.get(2).y - A2 * res_points[0].y - D2 * res_points[3].y;

        double y1 = (right1y * C2 - right2y * C1) / det;
        double y2 = (B1 * right2y - B2 * right1y) / det;

        res_points[1] = new Point2D(x1, y1);
        res_points[2] = new Point2D(x2, y2);
        is_on_line = true;
        //points = List.of(res_points[0], res_points[1], res_points[2], res_points[3]);
    }

    private void updateWorldCache(int count) {
        if (!needsWorldUpdate && worldX != null && worldX.length == count) return;

        int n = points.size();

        if (worldX == null || worldX.length != count || n != calcBufferX.length) {
            worldX = new double[count];
            worldY = new double[count];
            calcBufferX = new double[n];
            calcBufferY = new double[n];
        }

        double dt = 1.0 / (count - 1);

        n = points.size();
        if (is_on_line) find_points();
        for (int c = 0; c < count; c++) {
            double t = c * dt;
            double mt = 1.0 - t;

            if (is_on_line && points.size() == 4) {
                for (int i = 0; i < n; i++) {
                    calcBufferX[i] = res_points[i].x;
                    calcBufferY[i] = res_points[i].y;
                }
            } else {
                for (int i = 0; i < n; i++) {
                    calcBufferX[i] = points.get(i).x;
                    calcBufferY[i] = points.get(i).y;
                }
            }

            for (int j = 1; j < n; j++) {
                for (int i = 0; i < n - j; i++) {
                    calcBufferX[i] = calcBufferX[i] * mt + calcBufferX[i + 1] * t;
                    calcBufferY[i] = calcBufferY[i] * mt + calcBufferY[i + 1] * t;
                }
            }
            worldX[c] = calcBufferX[0];
            worldY[c] = calcBufferY[0];
        }
        needsWorldUpdate = false;
    }

    public void drawBezierPoints(Point2D half_window, double scale, Point2D pos, Graphics g) {
        if (!isVisible(half_window, scale, pos)) return;

        int count = calculateDynamicCount(scale);
        if (count < 2) return;
        boolean b = needsWorldUpdate;
        updateWorldCache(count);

        if (b || scale != lastScale || pos.x != lastCamX || pos.y != lastCamY || screenX == null || screenX.length != count) {
            if (screenX == null || screenX.length != count) {
                screenX = new int[count];
                screenY = new int[count];
            }

            for (int i = 0; i < count; i++) {
                screenX[i] = (int) (half_window.x + (worldX[i] + pos.x) / scale);
                screenY[i] = (int) (half_window.y + (worldY[i] + pos.y) / scale);
            }
            lastScale = scale;
            lastCamX = pos.x;
            lastCamY = pos.y;
        }

        g.setColor(lineColor);
        g.drawPolyline(screenX, screenY, count);
    }

    private int calculateDynamicCount(double scale) {
        if (points.size() <= 2) return 2;
        double totalLength = 0;
        Point2D lastPoint = null;
        for (Point2D point : points) {
            if (lastPoint != null) {
                totalLength += Math.hypot(point.x - lastPoint.x, point.y - lastPoint.y);
            }
            lastPoint = point;
        }
        double pixelsPerPoint = 4.0;
        return Math.max(10, Math.min((int) ((totalLength / scale) / pixelsPerPoint), 5000));
    }

    private boolean isVisible(Point2D half_window, double scale, Point2D pos) {
        if (points.isEmpty()) return false;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Point2D p : points) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        double screenMinX = half_window.x + (minX + pos.x) / scale;
        double screenMaxX = half_window.x + (maxX + pos.x) / scale;
        double screenMinY = half_window.y + (minY + pos.y) / scale;
        double screenMaxY = half_window.y + (maxY + pos.y) / scale;

        return !(screenMaxX < 0 || screenMinX > half_window.x * 2 ||
                screenMaxY < 0 || screenMinY > half_window.y * 2);
    }

    public void add_comp(int t) {
        if (is_composite == 1 && t == 0) is_composite = 2;
        if (is_composite == -1 && t == 1) is_composite = 2;
        if (is_composite == 0) is_composite = 2 * t - 1;
    }

    public BezierLine addCompositeLine(Point2D selectedPoint) {
        if (is_composite == 2) return null;
        if (points.size() < 2) return null;

        Point2D pointNow = points.getFirst();
        if (selectedPoint == pointNow) {
            if (is_composite == -1) return null;
            if (is_composite == 1) is_composite = 2;
            else is_composite = -1;
            BezierLine line = new BezierLine(new Point2D(pointNow.x, pointNow.y));
            line.is_composite = -1;

            Point2D secPoint = points.get(1);
            line.joined_points[0][0] = pointNow;
            line.joined_points[0][1] = secPoint;
            joinedLines[0] = line;
            line.joinedLines[0] = this;
            line.points.add(new Point2D(2 * pointNow.x - secPoint.x, 2 * pointNow.y - secPoint.y));

            joined_points[0][0] = line.points.getFirst();
            joined_points[0][1] = line.points.getLast();
            return line;
        } else {
            pointNow = points.getLast();
            if (selectedPoint == pointNow) {
                if (is_composite == 1) return null;
                if (is_composite == -1) is_composite = 2;
                else is_composite = 1;
                BezierLine line = new BezierLine(new Point2D(pointNow.x, pointNow.y));
                line.is_composite = -1;

                Point2D secPoint = points.get(points.size() - 2);
                line.joined_points[0][0] = pointNow;
                line.joined_points[0][1] = secPoint;
                line.joinedLines[0] = this;
                line.points.add(new Point2D(2 * pointNow.x - secPoint.x, 2 * pointNow.y - secPoint.y));

                joined_points[1][0] = line.points.getFirst();
                joined_points[1][1] = line.points.getLast();
                joinedLines[1] = line;
                return line;
            }
        }
        return null;
    }

    public BezierLine(Point2D p0) {
        points = new ArrayList<>();
        points.add(p0);
    }

    public BezierLine(Point p0) {
        points = new ArrayList<>();
        points.add(new Point2D(p0));
    }

    public BezierLine(List<Point2D> points) {
        this.points = points;
    }

    /*public void drawBezierPoints(Point2D half_window, double scale, Point2D pos, int count, Graphics g, boolean redraw) {
        if (!isVisible(half_window, scale, pos)) {
            System.out.println("nd");
            return;
        }

        count = calculateDynamicCount(scale);
        if (count <= 1) return;
        Point2D[] resPoints = redraw ? BezierPoints(half_window, scale, pos, points, count) : cachedScreenPoints;
        cachedScreenPoints = resPoints;
        if (resPoints.length == 0) return;

        int r = 1;
        g.setColor(Color.black);
        Point2D lastPoint = null;
        for (Point2D point : resPoints) {
            if (lastPoint != null) g.drawLine((int)lastPoint.x, (int)lastPoint.y, (int)point.x, (int)point.y);
            lastPoint = point;
        }
    }*/

    public void drawDottedLine(int x1, int y1, int x2, int y2, int len, Graphics g) {
        if (len <= 0) return;
        double lineLen = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        int count = (int) (Math.sqrt(lineLen) / len);
        if (count < 7) count = 7;

        double dx = (double) (x2 - x1) / count, dy = (double) (y2 - y1) / count;
        double lastX = x1, lastY = y1, newX, newY;

        for (int i = 0; i < count; i += 2) {
            newX = lastX + dx;
            newY = lastY + dy;
            g.drawLine((int) lastX, (int) lastY, (int) newX, (int) newY);
            lastX = newX + dx;
            lastY = newY + dy;
        }
    }

    public Point2D[] BezierPoints(Point2D half_window, double scale, Point2D position, List<Point2D> ys, int count) {
        if (ys.isEmpty()) return new Point2D[0];

        double k = 1. / (count - 1);
        int n = ys.size(), cnt = n * (n + 1) / 2;
        Point2D[] res = new Point2D[count];
        Point2D[] arrY = new Point2D[cnt];

        for (int i = 0; i < n; i++)
            arrY[i] = ys.get(i).transpose(half_window, scale, position);

        double x = 0, mx = 1;
        int pos = n, index = 0;
        res[0] = ys.getFirst().transpose(half_window, scale, position);

        for (int c = 1; c < count - 1; c++) {
            x += k;
            mx -= k;
            for (int j = n - 1; j > 0; j--) {
                for (int i = 0; i < j; i++) {
                    arrY[pos] = Point2D.add(Point2D.multiply(arrY[index], mx), Point2D.multiply(arrY[index + 1], x));
                    index++;
                    pos++;
                }
                index++;
            }
            res[c] = new Point2D((int) arrY[cnt - 1].x, (int) arrY[cnt - 1].y);
            pos = n;
            index = 0;
        }
        res[count - 1] = ys.getLast().transpose(half_window, scale, position);
        return res;
    }

    public Point2D[] getDerivative(double x) {
        if (points.size() < 2) return new Point2D[]{new Point2D(0, 0), new Point2D(0, 0)};

        int n = points.size(), cnt = n * (n + 1) / 2;
        Point2D[] arrY = new Point2D[cnt];
        int pos = n, index = 0;
        double mx = 1 - x;

        for (int i = 0; i < n; i++)
            arrY[i] = new Point2D(points.get(i));

        for (int j = n - 1; j > 0; j--) {
            for (int i = 0; i < j; i++) {
                arrY[pos] = Point2D.add(Point2D.multiply(arrY[index], mx), Point2D.multiply(arrY[index + 1], x));
                index++;
                pos++;
            }
            index++;
        }
        return new Point2D[]{new Point2D((arrY[cnt - 2].x - arrY[cnt - 3].x) * n, (arrY[cnt - 2].y - arrY[cnt - 3].y) * n),
                new Point2D((int) arrY[cnt - 1].x, (int) arrY[cnt - 1].y)
        };
    }

    public boolean drawTangent(double t, Color lineColor, Graphics g, int width, int height) {
        if (t < 0 || t > 1) return false;

        Point2D[] der = getDerivative(t);
        if (der[0].x == 0 && der[0].y == 0) return false;


        g.setColor(lineColor);
        if (Math.abs(der[0].x) <= 1e-5) {
            if (Math.abs(der[0].y) <= 1e-5) return false;
            double k = der[0].x / der[0].y, b;
            b = der[1].x - der[1].y * k;
            g.drawLine((int) b, 0, (int) (k * height + b), height);
            return true;
        }
        double k = der[0].y / der[0].x, b;
        b = der[1].y - der[1].x * k;
        g.drawLine(0, (int) b, width, (int) (k * width + b));
        return true;
    }


    private boolean isSegmentVisible(Point2D p1, Point2D p2, Point2D half_window, double scale, Point2D position) {
        double screenLeft = position.x - half_window.x / scale;
        double screenTop = position.y - half_window.y / scale;
        double screenRight = position.x + half_window.x / scale;
        double screenBottom = position.y + half_window.y / scale;

        boolean p1Inside = p1.x >= screenLeft && p1.x <= screenRight &&
                p1.y >= screenTop && p1.y <= screenBottom;
        boolean p2Inside = p2.x >= screenLeft && p2.x <= screenRight &&
                p2.y >= screenTop && p2.y <= screenBottom;

        if (p1Inside || p2Inside) {
            return true;
        }
        Point2D leftTop = new Point2D(screenLeft, screenTop);
        Point2D leftBottom = new Point2D(screenLeft, screenBottom);
        Point2D rightTop = new Point2D(screenRight, screenTop);
        Point2D rightBottom = new Point2D(screenRight, screenBottom);

        if (segmentsIntersect(p1, p2, leftTop, leftBottom)) return true;
        if (segmentsIntersect(p1, p2, rightTop, rightBottom)) return true;
        if (segmentsIntersect(p1, p2, leftTop, rightTop)) return true;
        return segmentsIntersect(p1, p2, leftBottom, rightBottom);
    }

    private boolean segmentsIntersect(Point2D a1, Point2D a2, Point2D b1, Point2D b2) {
        double o1 = orientation(a1, a2, b1);
        double o2 = orientation(a1, a2, b2);
        double o3 = orientation(b1, b2, a1);
        double o4 = orientation(b1, b2, a2);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        if (o1 == 0 && onSegment(a1, b1, a2)) return true;
        if (o2 == 0 && onSegment(a1, b2, a2)) return true;
        if (o3 == 0 && onSegment(b1, a1, b2)) return true;
        if (o4 == 0 && onSegment(b1, a2, b2)) return true;

        return false;
    }

    private double orientation(Point2D p, Point2D q, Point2D r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    }

    private boolean onSegment(Point2D p, Point2D q, Point2D r) {
        return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
                q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
    }


    private void redrawWithColors(Point2D half_window, double scale, Point2D position, Color pointC, Color line, Graphics g) {
        if (points.isEmpty())
            return;

        Point2D lastPoint = points.getFirst().transpose(half_window, scale, position);
        Point2D pointNow;

        g.setColor(line);
        for (int i = 1; i < points.size(); i++) {
            pointNow = points.get(i).transpose(half_window, scale, position);


            Point2D worldLast = points.get(i - 1);
            Point2D worldNow = points.get(i);

            if (isSegmentVisible(worldLast, worldNow, half_window, scale, position)) {
                drawDottedLine((int) lastPoint.x, (int) lastPoint.y, (int) pointNow.x, (int) pointNow.y, 50, g);
            }

            //drawDottedLine((int)lastPoint.x, (int)lastPoint.y, (int)pointNow.x, (int)pointNow.y, 50, g);
            lastPoint = pointNow;
        }

        g.setColor(pointC);
        for (Point2D point2 : points) {
            Point2D point = point2.transpose(half_window, scale, position);
            g.fillOval((int) point.x - pointSize[0] / 2, (int) point.y - pointSize[1] / 2, pointSize[0], pointSize[1]);
        }
        g.setColor(Color.white);
        lastPoint = points.getLast().transpose(half_window, scale, position);
        g.fillOval((int) lastPoint.x - pointSize[0] / 2 + 2, (int) lastPoint.y - pointSize[1] / 2 + 2, pointSize[0] - 4, pointSize[1] - 4);
    }

    public Point2D proec(Point2D p1, Point2D p2, Point2D p3) {
        Point2D vec = Point2D.div(p2, p1);
        Point2D t1 = Point2D.div(p3, p1);

        double len2 = vec.x * vec.x + vec.y * vec.y;
        if (len2 == 0) return null;

        return Point2D.add(p1, Point2D.multiply(vec, (t1.x * vec.x + t1.y * vec.y) / len2));
    }

    public boolean shift_compose(Point2D selectedPoint, Point2D ePoint, int type_m) {
        if (type_m == 0) {
            if ((is_composite == -1 || is_composite == 2) && points.getFirst() == selectedPoint) {
                Point2D res = proec(joined_points[0][1], points.get(1), ePoint);
                if (res == null) return true;
                Point2D p1 = points.getFirst();
                p1.x = res.x;
                p1.y = res.y;
                joined_points[0][0].x = res.x;
                joined_points[0][0].y = res.y;
                return false;
            } else if (is_composite > 0 && points.getLast() == selectedPoint) {
                Point2D res = proec(joined_points[1][1], points.get(points.size() - 2), ePoint);
                if (res == null) return true;
                Point2D p1 = points.getLast();
                p1.x = res.x;
                p1.y = res.y;
                joined_points[1][0].x = res.x;
                joined_points[1][0].y = res.y;
                return false;
            }

        } else {
            Point2D[] pp = null;
            if ((is_composite == -1 || is_composite == 2) && points.getFirst() == selectedPoint)
                pp = new Point2D[]{joined_points[0][0], joined_points[0][1], points.get(1)};
            else if (is_composite > 0 && points.getLast() == selectedPoint)
                pp = new Point2D[]{joined_points[1][0], joined_points[1][1], points.get(points.size() - 2)};

            if (pp != null) {
                for (Point2D p : pp) {
                    p.x += ePoint.x - selectedPoint.x;
                    p.y += ePoint.y - selectedPoint.y;
                }
            }
        }
        return true;
    }

    public void move_not_end_compose(Point2D selectedPoint, Point2D ePoint, int type_m) {
        if (type_m == 0) {
            if ((is_composite == -1 || is_composite == 2) && points.get(1) == selectedPoint)
                setIf_composite(selectedPoint, ePoint, 0);
            if (is_composite > 0 && points.get(points.size() - 2) == selectedPoint)
                setIf_composite(selectedPoint, ePoint, 1);
        } else {
            if ((is_composite == -1 || is_composite == 2) && points.get(1) == selectedPoint) {
                if (rotate_triangle(selectedPoint, ePoint, type_m, 0))
                    move_compose(points.getFirst(), selectedPoint, joined_points[0][1], ePoint);
            }
            if (is_composite > 0 && points.get(points.size() - 2) == selectedPoint) {
                if (rotate_triangle(selectedPoint, ePoint, type_m, 1))
                    move_compose(points.getLast(), selectedPoint, joined_points[1][1], ePoint);
            }
        }
    }

    private void setIf_composite(Point2D selectedPoint, Point2D ePoint, int lr) {
        try {
            selectedPoint.x = ePoint.x;
            selectedPoint.y = ePoint.y;
            Point2D next = new Point2D(joined_points[lr][1]);
            Point2D np = Point2D.add(Point2D.multiply(Point2D.div(new Point2D(selectedPoint), next).normalize(), Point2D.div(new Point2D(joined_points[lr][0]), next).length()), next);
            Point2D first = lr == 0 ? points.getFirst() : points.getLast();
            joined_points[lr][0].x = np.x;
            joined_points[lr][0].y = np.y;
            first.x = np.x;
            first.y = np.y;
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
    }

    private void move_compose(Point2D sp, Point2D next, Point2D prev, Point2D ePoint) {
        try {
            next.x = ePoint.x;
            next.y = ePoint.y;

            Point2D sp2D = new Point2D(sp),
                    next2D = new Point2D(next),
                    prev2D = new Point2D(prev);
            double t = Point2D.div(sp2D, prev2D).length();
            Point2D t2 = Point2D.add(sp2D, Point2D.multiply(Point2D.div(sp2D, next2D).normalize(), t));
            prev.x = t2.x;
            prev.y = t2.y;
        } catch (Exception exception) {
            // теоретически должно быть, но тогда нужно сделать то, как оттаскивать эти точки
            /*selected.joined_points[0][1].x = ePoint.x;
            selected.joined_points[0][1].y = ePoint.y;*/
            System.out.println(exception.getMessage());
        }
    }

    public boolean rotate_triangle(Point2D selectedPoint, Point2D ePoint, int type_m, int lr) {
        try {
            BezierLine s2 = joinedLines[lr];
            if (s2 != null && s2.points.size() == 3) {
                if (type_m == 0) {
                    selectedPoint.x = ePoint.x;
                    selectedPoint.y = ePoint.y;
                    Point2D next = new Point2D(joined_points[lr][1]);
                    Point2D np = Point2D.add(Point2D.multiply(Point2D.div(new Point2D(selectedPoint), next).normalize(), Point2D.div(new Point2D(joined_points[lr][0]), next).length()), next);
                    Point2D first = points.getFirst();
                    joined_points[lr][0].x = np.x;
                    joined_points[lr][0].y = np.y;
                    first.x = np.x;
                    first.y = np.y;
                    return false;
                } else {
                    if (s2.joinedLines[1] != null && s2.joinedLines[1] != this) {
                        move_compose(points.getLast(), selectedPoint, joined_points[lr][1], ePoint);
                        move_compose(s2.points.getLast(), joined_points[lr][1], s2.joined_points[1][1], ePoint);
                        return false;
                    } else if (s2.joinedLines[0] != null && s2.joinedLines[0] != this) {
                        move_compose(points.getFirst(), selectedPoint, joined_points[lr][1], ePoint);
                        move_compose(s2.points.getFirst(), joined_points[lr][1], s2.joined_points[0][1], ePoint);
                        return false;
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        return true;
    }

    public void redrawLines(Point2D half_window, double scale, Point2D pos, Graphics g) {
        redrawWithColors(half_window, scale, pos, pointColor, lineColor, g);
    }

    public Point2D getNearPoint(Point2D p0, int dist) {
        double d2 = dist * dist;

        for (Point2D point : points) {
            if ((point.x - p0.x) * (point.x - p0.x) + (point.y - p0.y) * (point.y - p0.y) <= d2)
                return point;
        }
        return null;
    }

    public Point2D findNearestInRadius(int mouseX, int mouseY, int radius, Point2D half_window, double scale, Point2D position) {
        double r2 = (double) radius * radius;
        double minDistance2 = Double.MAX_VALUE;
        Point2D bestPoint = null;

        for (Point2D p : points) {
            double sx = half_window.x + (p.x + position.x) / scale;
            double sy = half_window.y + (p.y + position.y) / scale;

            double dx = mouseX - sx;
            double dy = mouseY - sy;
            double d2 = dx * dx + dy * dy;

            if (d2 <= r2 && d2 < minDistance2) {
                minDistance2 = d2;
                bestPoint = p;
            }
        }
        return bestPoint;
    }

    public Point2D getNearPoint_transpose(Point2D hw, double scale, Point2D position, Point2D p0, double dist) {
        double d2 = dist * dist;

        for (Point2D point2 : points) {
            Point2D point = point2.transpose(hw, scale, position);
            if ((point.x - p0.x) * (point.x - p0.x) + (point.y - p0.y) * (point.y - p0.y) <= d2)
                return point;
        }
        return null;
    }
}
