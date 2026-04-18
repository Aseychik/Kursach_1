import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class Main extends JFrame {
    Point selectedPoint = null;
    JPanel canvas;
    List<Point> points = new ArrayList<>();
    List<BezierLine> bezierLines = new ArrayList<>();
    int selectedLine = -1;
    Color lineColor = Color.black, pointColor = Color.green, bgColor = Color.WHITE;
    int[] pointSize = new int[]{12, 12};
    int bezierCount = 100;
    boolean isShowPoints = true;
    boolean is_draw_dev = false;
    int count_dev = 3;
    List<Point> connectedPoints = null;
    boolean connect_points = false;

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public Main(String title) throws IOException {
        super(title);
        int w = 1200, h = 600;
        setSize(w, h);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel canvasPanel = new JPanel(true);

        canvas = new MyPanel(true);
        canvas.setPreferredSize(new Dimension(w - 20, h - 100));
        canvas.setBackground(Color.WHITE);

        JButton buttonSave = new JButton(),
                hidePointsButton = new JButton(),
                importFileButton = new JButton(),
                deletePointButton = new JButton();

        Action saveAction = new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SaveAsFile();
                System.out.println("saved");
            }
        }, hideAction = new AbstractAction("Visibility") {
            @Override
            public void actionPerformed(ActionEvent e) {
                isShowPoints = !isShowPoints;
                canvas.repaint();
            }
        }, connect_button_action = new AbstractAction("ConnectSwitch") {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect_points = !connect_points;
            }
        }, importAction = new AbstractAction("Import") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportFromFile();
                System.out.println("imported");
            }
        }, deletePointAction = new AbstractAction("DeletePoint") {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedPoint();
            }
        }, connectPointsAction = new AbstractAction("ConnectPoints") {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (bezierLines.isEmpty()) return;
                if (selectedPoint == null) return;
                if (connectedPoints != null) {
                    for (Point p : connectedPoints) {
                        p.x = selectedPoint.x;
                        p.y = selectedPoint.y;
                    }
                    return;
                }

                int r = 20;
                r = r * r;
                connectedPoints = new ArrayList<>();
                for (BezierLine line : bezierLines)
                    for (Point point : line.points)
                        if ((point.x - selectedPoint.x) * (point.x - selectedPoint.x) + (point.y - selectedPoint.y) * (point.y - selectedPoint.y) <= r && point != selectedPoint) {
                            point.x = selectedPoint.x;
                            point.y = selectedPoint.y;
                            connectedPoints.add(point);
                        }
                repaint();
            }
        };

        buttonSave.setAction(saveAction);
        //saveAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        buttonSave.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "Save");
        buttonSave.getActionMap().put("Save", saveAction);

        hidePointsButton.setAction(hideAction);
        hidePointsButton.getActionMap().put("Visibility", hideAction);
        hidePointsButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "Visibility");

        importFileButton.setAction(importAction);

        deletePointButton.setAction(deletePointAction);
        deletePointButton.getActionMap().put("DeletePoint", deletePointAction);
        deletePointButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DeletePoint");

        canvas.setLocation(0, 50);

        canvasPanel.add(canvas);
        Container content = getContentPane();
        canvasPanel.add(buttonSave);
        canvasPanel.add(hidePointsButton);
        canvasPanel.add(importFileButton);
        canvasPanel.add(deletePointButton);

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "ConnectPoints");
        canvasPanel.getActionMap().put("ConnectPoints", connectPointsAction);

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "ConnectSwitch");
        canvasPanel.getActionMap().put("ConnectSwitch", connect_button_action);


        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0, true), "ReleaseE");
        canvasPanel.getActionMap().put("ReleaseE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectedPoints = null;
                canvas.repaint();
            }
        });

        canvasPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, true), "CreateComp");
        canvasPanel.getActionMap().put("CreateComp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedPoint == null) return;
                if (selectedLine == -1) return;
                BezierLine res = bezierLines.get(selectedLine).addCompositeLine(selectedPoint);
                if (res == null) return;
                bezierLines.add(res);
                canvas.repaint();
            }
        });

        content.add(BorderLayout.CENTER, canvasPanel);

        setVisible(true);
    }


    public static void main(String[] args) throws IOException {
        Main main = new Main("Первый тест курсового проекта");

    }

    public void drawBezierPoints(int count, Graphics g) {
        if (count <= 1) return;
        Point[] resPoints = BezierPoints(points, count);
        if (resPoints.length == 0) return;

        int r = 1;
        g.setColor(Color.black);
        Point lastPoint = null;
        for (Point point : resPoints) {
            if (lastPoint != null) g.drawLine(lastPoint.x, lastPoint.y, point.x, point.y);
            lastPoint = point;
        }
    }

    public void drawDottedLine(int x1, int y1, int x2, int y2, int len, Graphics g) {
        if (len <= 0) return;
        double lineLen = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        int count = (int) (lineLen / (len * len));
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

    public Point[] BezierPoints(List<Point> ys, int count) {
        if (ys.isEmpty()) return new Point[0];

        double k = 1. / (count - 1);
        int n = ys.size(), cnt = n * (n + 1) / 2;
        Point[] res = new Point[count];
        Point2D[] arrY = new Point2D[cnt];

        for (int i = 0; i < n; i++)
            arrY[i] = new Point2D(ys.get(i));

        double x = 0, mx = 1;
        int pos = n, index = 0;
        res[0] = ys.getFirst();

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
            res[c] = new Point((int) arrY[cnt - 1].x, (int) arrY[cnt - 1].y);
            pos = n;
            index = 0;
        }
        res[count - 1] = ys.getLast();
        return res;
    }

    private void redrawWithColors(Color pointC, Color line, Graphics g, int width, int height) {
        if (points.isEmpty())
            return;

        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);


        Point lastPoint = points.getFirst();
        Point pointNow;

        g.setColor(line);
        for (int i = 1; i < points.size(); i++) {
            pointNow = points.get(i);
            drawDottedLine(lastPoint.x, lastPoint.y, pointNow.x, pointNow.y, 100, g);
            lastPoint = pointNow;
        }

        g.setColor(pointC);
        for (Point point : points)
            g.fillOval(point.x - pointSize[0] / 2, point.y - pointSize[1] / 2, pointSize[0], pointSize[1]);
        g.setColor(Color.white);
        lastPoint = points.getLast();
        g.fillOval(lastPoint.x - pointSize[0] / 2 + 2, lastPoint.y - pointSize[1] / 2 + 2, pointSize[0] - 4, pointSize[1] - 4);
    }

    public void redrawLines(Graphics g, int w, int h) {
        redrawWithColors(pointColor, lineColor, g, w, h);
    }

    public void deleteSelectedPoint() {
        if (selectedPoint == null || selectedLine == -1) return;

        // потом придумать, как лучше удалять или точнее - что делать при удалении точек участвующих в создании составных кривых
        BezierLine line = bezierLines.get(selectedLine);
        if ((line.is_composite == -1 || line.is_composite == 2) &&
                (selectedPoint == line.points.getFirst() || selectedPoint == line.points.get(1)))
            return;

        if ((line.is_composite == 1 || line.is_composite == 2) &&
                (selectedPoint == line.points.getLast() || selectedPoint == line.points.get(line.points.size() - 2)))
            return;

        if (line.points.size() == 1) {
            bezierLines.remove(selectedLine);
            selectedLine = -1;
        } else {
            line.points.remove(selectedPoint);
        }
        selectedPoint = null;
        connectedPoints = null;
        repaint();
    }

    public void SaveAsFile() {
        JFileChooser j = new JFileChooser();

        if (j.showSaveDialog(null) == 0) {
            String filePath = j.getSelectedFile().getAbsolutePath();

            try (FileWriter fw = new FileWriter(filePath)) {
                fw.write(bezierLines.size() + "\n");
                for (BezierLine bez : bezierLines) {
                    fw.write(bez.points.size() + "|");
                    for (Point point : bez.points) {
                        fw.write(point.x + " " + point.y);
                        fw.write("|");
                    }
                    fw.write('\n');
                }
            } catch (
                    IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void ImportFromFile() {
        JFileChooser j = new JFileChooser();

        if (j.showOpenDialog(null) == 0) {
            String filePath = j.getSelectedFile().getAbsolutePath();
            if (j.getSelectedFile().isFile()) {
                try (BufferedReader fw = new BufferedReader(new FileReader(filePath))) {
                    int countBeziers = Integer.parseInt(fw.readLine().strip()), pointsCount;
                    List<Point> bez;
                    String[] points;
                    String[] point;
                    for (int i = 0; i < countBeziers; i++) {
                        points = fw.readLine().strip().split("\\|");
                        pointsCount = Integer.parseInt(points[0]);
                        bez = new ArrayList<>();
                        for (int k = 1; k < pointsCount + 1; k++) {
                            point = points[k].split(" ");
                            bez.add(new Point(Integer.parseInt(point[0]), Integer.parseInt(point[1])));
                        }
                        bezierLines.add(new BezierLine(bez));
                    }
                    selectedLine = -1;
                    selectedPoint = null;
                    connectedPoints = null;
                    canvas.repaint();
                } catch (
                        IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public class MyPanel extends JPanel implements MouseListener, MouseMotionListener {

        public MyPanel(boolean isDoubleBuffered) {
            super(isDoubleBuffered);
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(bgColor);
            int w = this.getWidth(), h = this.getHeight();
            g.fillRect(0, 0, w, h);
            BezierLine line;
            for (int i = 0; i < bezierLines.size(); i++) {
                line = bezierLines.get(i);
                if (isShowPoints) {
                    if (selectedLine == i) line.pointColor = Color.GREEN;
                    else line.pointColor = Color.DARK_GRAY;
                    line.redrawLines(g, w, h);
                }
                if (selectedLine == i && is_draw_dev) {
                    double step = (double) 1 / (count_dev + 1);
                    for (int k = 1; k <= count_dev; k++)
                        line.drawTangent(k * step, Color.DARK_GRAY, g, w, h);
                }

                line.drawBezierPoints(bezierCount, g);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == 3) {
                bezierLines.add(new BezierLine(e.getPoint()));
                selectedLine = bezierLines.size() - 1;
            } else if (selectedLine >= 0 && bezierLines.get(selectedLine).is_composite < 1)
                bezierLines.get(selectedLine).points.add(e.getPoint());

            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (bezierLines.isEmpty()) return;

            //if (points.isEmpty()) return;
            Point pos = e.getPoint(), sel;

            int p = -1, d = pointSize[0] / 2 + 5;
            for (BezierLine line : bezierLines) {
                sel = line.getNearPoint(pos, d);
                if (sel != null) {
                    if (connect_points) {
                        BezierLine l = bezierLines.get(p + 1);

                    }
                    selectedPoint = sel;
                    selectedLine = p + 1;
                    repaint();
                    return;
                }
                ++p;
            }

            selectedPoint = null;
            connectedPoints = null;

            /*for (Point point : points) {
                if ((point.x - pos.x) * (point.x - pos.x) + (point.y - pos.y) * (point.y - pos.y) <= (pointSize[0] / 2 + 5) * (pointSize[0] / 2 + 5)) {
                    selectedPoint = point;
                    return;
                }
            }*/
            //points.add(e.getPoint());
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point ePoint = e.getPoint();
            if (selectedPoint != null) {
                int type_m = 0;
                BezierLine selected = bezierLines.get(selectedLine);
                boolean b = selected.shift_compose(selectedPoint, ePoint, type_m);
                selected.move_not_end_compose(selectedPoint, ePoint, type_m);
                if (b) {
                    selectedPoint.x = ePoint.x;
                    selectedPoint.y = ePoint.y;
                }
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }
}