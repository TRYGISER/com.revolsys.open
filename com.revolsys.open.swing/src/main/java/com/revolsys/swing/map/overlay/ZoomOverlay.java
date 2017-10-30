package com.revolsys.swing.map.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import com.revolsys.datatype.DataTypes;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.map.MapPanel;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.preferences.PreferencesDialog;
import com.revolsys.util.Booleans;
import com.revolsys.util.OS;

public class ZoomOverlay extends AbstractOverlay {
  private static final String PREFERENCE_WHEEL_FORWARDS_ZOOM_IN = "wheelForwardsZoomIn";

  private static final String PREFERENCE_PATH = "/com/revolsys/gis/zoom";

  public static final String ACTION_PAN = "pan";

  public static final String ACTION_ZOOM = "zoom";

  public static final String ACTION_ZOOM_BOX = "zoomBox";

  private static final Cursor CURSOR_PAN = new Cursor(Cursor.HAND_CURSOR);

  private static final Cursor CURSOR_ZOOM_BOX = Icons.getCursor("cursor_zoom_box", 9, 9);

  private static final long serialVersionUID = 1L;

  private static final Color TRANS_BG = new Color(0, 0, 0, 30);

  public static final BasicStroke ZOOM_BOX_STROKE = new BasicStroke(2, BasicStroke.CAP_SQUARE,
    BasicStroke.JOIN_MITER, 2, new float[] {
      6, 6
    }, 0f);

  static {
    PreferencesDialog.get().addPreference("Zoom", "com.revolsys.gis", PREFERENCE_PATH,
      PREFERENCE_WHEEL_FORWARDS_ZOOM_IN, DataTypes.BOOLEAN, true);
  }

  private int panButton;

  private BufferedImage panImage;

  private int panX1 = -1;

  private int panX2 = -1;

  private int panY1 = -1;

  private int panY2 = -1;

  private int zoomBoxX1 = -1;

  private int zoomBoxX2;

  private int zoomBoxY1;

  private int zoomBoxY2;

  public ZoomOverlay(final MapPanel map) {
    super(map);
    addOverlayAction(ACTION_ZOOM_BOX, CURSOR_ZOOM_BOX);
    addOverlayAction(ACTION_PAN, CURSOR_PAN);
  }

  protected void cancel() {
    panClear();
    zoomBoxClear();
    repaint();
  }

  @Override
  public void focusLost(final FocusEvent e) {
    cancel();
  }

  public boolean isWheelForwardsZoomIn() {
    final Object wheelForwardsZoomIn = OS.getPreference("com.revolsys.gis", PREFERENCE_PATH,
      PREFERENCE_WHEEL_FORWARDS_ZOOM_IN);
    return !Booleans.isFalse(wheelForwardsZoomIn);
  }

  @Override
  public void keyPressed(final KeyEvent event) {
    final int keyCode = event.getKeyCode();
    if (keyCode == KeyEvent.VK_ESCAPE) {
      cancel();
    } else if (keyCode == KeyEvent.VK_SHIFT) {
      if (isMouseInMap()) {
        setOverlayAction(ACTION_ZOOM_BOX);
      }
    }
  }

  @Override
  public void keyReleased(final KeyEvent event) {
    if (!event.isShiftDown() && this.zoomBoxX1 == -1) {
      zoomBoxClear();
    }
  }

  @Override
  public void keyTyped(final KeyEvent event) {
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    if (canOverrideOverlayAction(ACTION_ZOOM)) {
      final int button = event.getButton();
      // Double click
      if (event.getClickCount() == 2) {
        final int x = event.getX();
        final int y = event.getY();
        int numSteps = 0;

        if (button == MouseEvent.BUTTON1 && !hasOverlayAction() || button == MouseEvent.BUTTON2) {
          // Left or middle button, zoom in
          numSteps = -1;
        } else if (button == MouseEvent.BUTTON3) {
          // Right mouse button, zoom out
          numSteps = 1;
        }
        if (numSteps != 0) {
          final Viewport2D viewport = getViewport();
          final Point mapPoint = viewport.toModelPoint(x, y);
          final MapPanel map = getMap();
          map.zoom(mapPoint, numSteps);
          event.consume();
        }
      }
    }
  }

  @Override
  public void mouseDragged(final MouseEvent event) {
    if (zoomBoxDrag(event)) {
    } else if (panDrag(event)) {
    }
  }

  @Override
  public void mouseEntered(final MouseEvent e) {
  }

  @Override
  public void mouseExited(final MouseEvent e) {
    if (this.zoomBoxX1 == -1) {
      zoomBoxClear();
    }
  }

  @Override
  public void mouseMoved(final MouseEvent event) {
    if (zoomBoxMove(event)) {
    }
  }

  @Override
  public void mousePressed(final MouseEvent event) {
    if (zoomBoxStart(event)) {
    } else if (panStart(event, false)) {
    }
  }

  @Override
  public void mouseReleased(final MouseEvent event) {
    if (panFinish(event)) {
    } else if (zoomBoxFinish(event)) {
    }
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent event) {
    if (canOverrideOverlayAction(ACTION_ZOOM)) {
      int numSteps = event.getWheelRotation();
      if (SwingUtil.isScrollReversed()) {
        numSteps = -numSteps;
      }
      if (!isWheelForwardsZoomIn()) {
        numSteps = -numSteps;
      }

      numSteps = Math.max(-4, Math.min(4, numSteps));

      final int x = event.getX();
      final int y = event.getY();
      final Viewport2D viewport = getViewport();
      final Point mapPoint = viewport.toModelPoint(x, y);
      final MapPanel map = getMap();
      map.zoom(mapPoint, numSteps);
      event.consume();
    }
  }

  @Override
  protected void paintComponent(final Viewport2D viewport, final Graphics2D graphics) {
    if (this.zoomBoxX1 != -1) {
      graphics.setColor(Color.DARK_GRAY);
      graphics.setStroke(ZOOM_BOX_STROKE);
      final int boxX = Math.min(this.zoomBoxX1, this.zoomBoxX2);
      final int boxY = Math.min(this.zoomBoxY1, this.zoomBoxY2);
      final int width = Math.abs(this.zoomBoxX2 - this.zoomBoxX1);
      final int height = Math.abs(this.zoomBoxY2 - this.zoomBoxY1);
      graphics.drawRect(boxX, boxY, width, height);
      graphics.setPaint(TRANS_BG);
      graphics.fillRect(boxX, boxY, width, height);
    }
    if (this.panX1 != -1 && this.panImage != null) {
      final int dx = this.panX2 - this.panX1;
      final int dy = this.panY2 - this.panY1;
      final int width = viewport.getViewWidthPixels();
      final int height = viewport.getViewHeightPixels();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);

      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      final AffineTransform transform = AffineTransform.getTranslateInstance(dx, dy);
      graphics.drawRenderedImage(this.panImage, transform);
    }
  }

  protected void panClear() {
    this.panImage = null;
    this.panX1 = -1;
    this.panY1 = -1;
    this.panX2 = -1;
    this.panY2 = -1;
    clearOverlayAction(ACTION_PAN);
    final MapPanel map = getMap();
    map.clearVisibleOverlay(this);
  }

  public boolean panDrag(final MouseEvent event) {
    if (panStart(event, true)) {
      this.panX2 = event.getX();
      this.panY2 = event.getY();
      repaint();
      return true;
    }
    return false;
  }

  public boolean panFinish(final MouseEvent event) {
    if (event.getButton() == this.panButton) {
      if (clearOverlayAction(ACTION_PAN) && this.panX1 != -1) {
        this.panImage = null;
        if (this.panX1 != this.panX2 || this.panY1 != this.panY2) {
          final java.awt.Point point = event.getPoint();
          final Viewport2D viewport = getViewport();
          final Point fromPoint = viewport.toModelPoint(this.panX1, this.panY1);
          final Point toPoint = viewport.toModelPoint(point);

          final double deltaX = fromPoint.getX() - toPoint.getX();
          final double deltaY = fromPoint.getY() - toPoint.getY();

          final BoundingBox boundingBox = viewport.getBoundingBox();
          final BoundingBox newBoundingBox = boundingBox.move(deltaX, deltaY);

          final MapPanel map = getMap();
          map.setBoundingBox(newBoundingBox);
        }
        panClear();
        repaint();
        return true;
      }
    }
    return false;
  }

  public boolean panStart(final MouseEvent event, final boolean drag) {
    if (this.panX1 == -1) {
      boolean pan = false;
      final int button = event.getButton();
      if (button == MouseEvent.BUTTON2) {
        pan = true;
        this.panButton = MouseEvent.BUTTON2;
      } else if (!drag && button == MouseEvent.BUTTON1 && !hasOverlayAction()) {
        if (event.getModifiersEx() == InputEvent.BUTTON1_DOWN_MASK) {
          pan = true;
          this.panButton = MouseEvent.BUTTON1;
        }
      }
      if (pan) {
        if (setOverlayAction(ACTION_PAN)) {
          final Viewport2D viewport = getViewport();
          final int width = viewport.getViewWidthPixels();
          final int height = viewport.getViewHeightPixels();
          if (width > 0 && height > 0) {
            final JComponent parent = (JComponent)getParent();
            this.panImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D graphics = (Graphics2D)this.panImage.getGraphics();
            try {
              final Insets insets = parent.getInsets();
              graphics.translate(-insets.left, -insets.top);
              graphics.setColor(Color.WHITE);
              graphics.fillRect(insets.left, insets.top, width, height);
              parent.paintComponents(graphics);
            } finally {
              graphics.dispose();
            }
            this.panX1 = this.panX2 = event.getX();
            this.panY1 = this.panY2 = event.getY();
            final MapPanel map = getMap();
            map.setVisibleOverlay(this);
          }
          return true;
        }
      }
      return false;
    } else {
      return true;
    }
  }

  protected void zoomBoxClear() {
    this.zoomBoxX1 = -1;
    this.zoomBoxY1 = -1;
    this.zoomBoxX2 = -1;
    this.zoomBoxY2 = -1;
    clearOverlayAction(ACTION_ZOOM_BOX);
  }

  protected boolean zoomBoxDrag(final MouseEvent event) {
    if (isOverlayAction(ACTION_ZOOM_BOX)) {
      this.zoomBoxX2 = event.getX();
      this.zoomBoxY2 = event.getY();
      repaint();
      return true;
    } else {
      return false;
    }
  }

  protected boolean zoomBoxFinish(final MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1 && clearOverlayAction(ACTION_ZOOM_BOX)) {
      final Viewport2D viewport = getViewport();

      // Convert first point to envelope top left in map coords.
      final int minX = Math.min(this.zoomBoxX1, this.zoomBoxX2);
      final int minY = Math.min(this.zoomBoxY1, this.zoomBoxY2);
      final Point topLeft = viewport.toModelPoint(minX, minY);

      // Convert second point to envelope bottom right in map coords.
      final int maxX = Math.max(this.zoomBoxX1, this.zoomBoxX2);
      final int maxY = Math.max(this.zoomBoxY1, this.zoomBoxY2);
      final Point bottomRight = viewport.toModelPoint(maxX, maxY);

      final MapPanel map = getMap();
      final GeometryFactory geometryFactory = map.getGeometryFactory();
      final BoundingBox boundingBox = geometryFactory.newBoundingBox(topLeft.getX(), topLeft.getY(),
        bottomRight.getX(), bottomRight.getY());

      if (boundingBox.isEmpty()) {
        Toolkit.getDefaultToolkit().beep();
      } else {
        map.setBoundingBox(boundingBox);
      }
      zoomBoxClear();
      return true;
    }
    return false;
  }

  protected boolean zoomBoxMove(final MouseEvent event) {
    final int modifiers = event.getModifiersEx();
    if (modifiers == InputEvent.SHIFT_DOWN_MASK) {
      if (setOverlayAction(ACTION_ZOOM_BOX)) {
        return true;
      }
    }
    return false;
  }

  protected boolean zoomBoxStart(final MouseEvent event) {
    if (isOverlayAction(ACTION_ZOOM_BOX) && event.getButton() == MouseEvent.BUTTON1) {
      this.zoomBoxX1 = this.zoomBoxX2 = event.getX();
      this.zoomBoxY1 = this.zoomBoxY2 = event.getY();
      return true;
    }
    return false;
  }
}
