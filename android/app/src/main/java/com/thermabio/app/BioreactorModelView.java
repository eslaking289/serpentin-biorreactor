package com.thermabio.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Modelo isométrico nativo del biorreactor; no depende de WebView ni de archivos HTML. */
public final class BioreactorModelView extends View {
    public interface PartListener { void onPartSelected(String part); }

    private static final int INK = Color.rgb(17, 17, 17);
    private static final int TEAL = Color.rgb(0, 170, 176);
    private static final int MAGENTA = Color.rgb(230, 0, 169);
    private static final int PAPER = Color.rgb(255, 254, 247);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float yaw = -0.42f;
    private float downX, downY, lastX;
    private boolean dragging, labels = true, spinning;
    private int viewMode;
    private float temperature = 37f;
    private double tankDiameter = 1.1, tankHeight = 1.4, liquidHeight = 1.26;
    private double coilDiameter = 0.55, tubeDiameter = 0.0254, turns = 3.2;
    private double pitch = 0.05, brothTemperature = 37, coolantIn = 10, coolantOut = 15;
    private PartListener partListener;
    private long lastFrame;

    public BioreactorModelView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Modelo isométrico interactivo del biorreactor. Arrastra para girar; toca una pieza para seleccionarla.");
    }

    public void setPartListener(PartListener listener) { partListener = listener; }
    public void setViewMode(int mode) { viewMode = Math.max(0, Math.min(2, mode)); invalidate(); }
    public void setLabels(boolean enabled) { labels = enabled; invalidate(); }
    public void setSpinning(boolean enabled) { spinning = enabled; lastFrame = 0; invalidate(); }
    public void setDisplayTemperature(float value) { temperature = value; invalidate(); }
    public void selectPart(String part) { if (partListener != null) partListener.onPartSelected(part); invalidate(); }

    public void setDesign(double tankD, double tankH, double liquidH, double coilD, double tubeD,
                          double count, double coilPitch, double brothT, double inletT, double outletT) {
        if (positive(tankD)) tankDiameter = tankD;
        if (positive(tankH)) tankHeight = tankH;
        if (positive(liquidH)) liquidHeight = liquidH;
        if (positive(coilD)) coilDiameter = coilD;
        if (positive(tubeD)) tubeDiameter = tubeD;
        if (positive(count)) turns = Math.min(24, count);
        if (positive(coilPitch)) pitch = coilPitch;
        brothTemperature = brothT;
        coolantIn = inletT;
        coolantOut = outletT;
        if (!dragging) temperature = (float) brothT;
        invalidate();
    }

    private boolean positive(double value) { return Double.isFinite(value) && value > 0; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.currentTimeMillis();
        if (spinning) {
            if (lastFrame != 0) yaw += Math.min(50, now - lastFrame) * 0.00022f;
            lastFrame = now;
            postInvalidateOnAnimation();
        } else lastFrame = now;

        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        canvas.drawColor(PAPER);
        float modelH = Math.min(h * 0.66f, w * 0.92f);
        modelH = Math.max(150, modelH);
        float radius = modelH * (float) (tankDiameter / tankHeight) * 0.48f;
        radius = Math.min(radius, w * 0.29f);
        radius = Math.max(42, radius);
        float cx = w * 0.5f, baseY = h * 0.77f;
        float upperY = baseY - modelH;
        float ovalH = Math.max(15, radius * 0.28f);

        drawShell(canvas, cx, upperY, baseY, radius, ovalH);
        drawCoil(canvas, cx, upperY, baseY, radius, ovalH);
        drawAgitator(canvas, cx, upperY, baseY, radius, ovalH);
        drawShellFront(canvas, cx, upperY, baseY, radius, ovalH);
        if (labels) drawLabels(canvas, cx, upperY, baseY, radius);
        drawCaption(canvas, cx, h);
    }

    private void drawShell(Canvas c, float cx, float top, float bottom, float r, float eh) {
        int alpha = viewMode == 1 ? 42 : viewMode == 2 ? 238 : 105;
        paint.setColor(Color.argb(alpha, 132, 147, 151));
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(cx - r, top, cx + r, bottom, paint);
        paint.setColor(Color.argb(viewMode == 1 ? 150 : 235, 17, 17, 17));
        paint.setStrokeWidth(2.2f);
        paint.setStyle(Paint.Style.STROKE);
        c.drawOval(new RectF(cx - r, top - eh / 2, cx + r, top + eh / 2), paint);
        c.drawOval(new RectF(cx - r, bottom - eh / 2, cx + r, bottom + eh / 2), paint);
        c.drawLine(cx - r, top, cx - r, bottom, paint);
        c.drawLine(cx + r, top, cx + r, bottom, paint);
        if (viewMode != 0) {
            paint.setColor(Color.argb(190, 17, 17, 17));
            c.drawLine(cx, top, cx, bottom, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TEAL);
        c.drawRect(cx - r * .42f, top - 13, cx - r * .25f, top + 2, paint);
        paint.setColor(MAGENTA);
        c.drawRect(cx + r * .25f, top - 13, cx + r * .42f, top + 2, paint);
    }

    private void drawCoil(Canvas c, float cx, float top, float bottom, float tankR, float eh) {
        float coilR = tankR * (float) Math.min(.78, coilDiameter / tankDiameter);
        float coilBottom = bottom - Math.max(22, eh * .9f);
        float count = (float) Math.max(1.5, Math.min(24, turns));
        float liquidTop = top + (float) (tankHeight - liquidHeight) / (float) tankHeight * (bottom - top);
        float available = Math.max(20, coilBottom - liquidTop - 12);
        float physicalHeight = (float) (turns * pitch / tankHeight) * (bottom - top);
        float usable = Math.max(20, Math.min(available, physicalHeight));
        float coilTop = coilBottom - usable;
        int segments = Math.max(96, (int) (count * 48));
        Path path = new Path();
        for (int i = 0; i <= segments; i++) {
            float f = i / (float) segments;
            float a = f * count * (float) (Math.PI * 2) + yaw;
            float px = cx + (float) Math.cos(a) * coilR;
            float py = coilBottom - f * usable + (float) Math.sin(a) * coilR * .22f;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(Math.max(3, tankR * (float) (tubeDiameter / tankDiameter) * 2.2f));
        int thermal = temperature <= (coolantIn + coolantOut) * .5 ? TEAL : MAGENTA;
        paint.setColor(thermal);
        c.drawPath(path, paint);
        paint.setStrokeWidth(Math.max(2, paint.getStrokeWidth() * .5f));
        paint.setColor(Color.WHITE);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawAgitator(Canvas c, float cx, float top, float bottom, float r, float eh) {
        paint.setColor(INK);
        paint.setStrokeWidth(3);
        c.drawLine(cx, top - 8, cx, bottom - eh * 1.3f, paint);
        float y = bottom - (bottom - top) * .39f;
        c.drawRoundRect(cx - r * .42f, y - 4, cx + r * .42f, y + 4, 4, 4, paint);
        c.drawRoundRect(cx - 4, y - r * .19f, cx + 4, y + r * .19f, 4, 4, paint);
        c.drawCircle(cx, top - 9, Math.max(8, r * .09f), paint);
    }

    private void drawShellFront(Canvas c, float cx, float top, float bottom, float r, float eh) {
        if (viewMode == 0) return;
        paint.setColor(Color.argb(viewMode == 1 ? 20 : 225, 132, 147, 151));
        paint.setStyle(Paint.Style.FILL);
        c.drawRect(cx - r + 2, top + 2, cx + r - 2, bottom - 2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(INK);
        c.drawLine(cx - r, top, cx - r, bottom, paint);
        c.drawLine(cx + r, top, cx + r, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLabels(Canvas c, float cx, float top, float bottom, float r) {
        paint.setColor(INK);
        paint.setTextSize(Math.max(10, Math.min(12, r * .12f)));
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        c.drawText("TANQUE", cx - r - paint.getTextSize() * 3.8f, top + (bottom - top) * .48f, paint);
        c.drawText("SERPENTÍN", cx + r * .52f, top + (bottom - top) * .69f, paint);
        c.drawText("AGITADOR", cx - r * .9f, bottom + paint.getTextSize() * 1.7f, paint);
    }

    private void drawCaption(Canvas c, float cx, float h) {
        paint.setColor(INK);
        paint.setTextSize(12);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        String title = "MODELO 3D  ·  " + String.format(java.util.Locale.getDefault(), "%.1f °C", temperature);
        float width = paint.measureText(title);
        c.drawText(title, Math.max(8, cx - width / 2), h - 12, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = event.getX(); downY = event.getY(); dragging = false; return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) + Math.abs(event.getY() - downY) > 8) dragging = true;
                if (dragging) { yaw += (event.getX() - lastX) * .012f; lastX = event.getX(); invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging && partListener != null) {
                    float ratio = event.getY() / Math.max(1, getHeight());
                    partListener.onPartSelected(ratio < .3f ? "Tapa y motor" : ratio > .7f ? "Impulsores" : event.getX() > getWidth() * .57f ? "Serpentín" : "Tanque y deflectores");
                }
                performClick(); return true;
            default: return true;
        }
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
