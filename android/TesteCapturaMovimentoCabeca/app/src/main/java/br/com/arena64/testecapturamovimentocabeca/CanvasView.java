package br.com.arena64.testecapturamovimentocabeca;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class CanvasView extends View {
    public interface Listener {
        void onNewPosition(final int x, final int y);
    }
    private Paint paint;
    private Point point;

    public CanvasView(Context context) {
        super(context);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        paint = new Paint();
        point = new Point(getWidth() / 2, getHeight() / 2);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        point = new Point(getWidth() / 2, getHeight() / 2);
    }

    public void setPoint(Point p) {
        point = p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBG(canvas);
        drawGrid(canvas);
        drawPoint(canvas);
    }

    private void drawBG(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawPaint(paint);
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawLine(0, height / 2, width, height / 2, paint);
        canvas.drawLine(width / 2, 0, width / 2, height, paint);
    }

    private void drawPoint(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(point.x, point.y, 50,paint);
    }
}
