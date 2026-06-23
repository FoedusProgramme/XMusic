package com.xapps.media.xmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ModularScrimView extends View {

    public enum Direction {
        TOP_TO_BOTTOM, BOTTOM_TO_TOP, LEFT_TO_RIGHT, RIGHT_TO_LEFT
    }

    public enum ShapeMode {
        LINEAR, ARC
    }

    private final Paint paint;
    private Direction direction = Direction.BOTTOM_TO_TOP;
    private ShapeMode shapeMode = ShapeMode.LINEAR;
    private int solidColor = Color.BLACK;
    private float solidPercentage = 0.5f;
    
    private int customWidth = Integer.MIN_VALUE;
    private int customHeight = Integer.MIN_VALUE;

    public ModularScrimView(@NonNull Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public ModularScrimView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            invalidate();
        }
    }

    public void setShapeMode(ShapeMode mode) {
        if (this.shapeMode != mode) {
            this.shapeMode = mode;
            invalidate();
        }
    }

    public void setSolidColor(int color) {
        if (this.solidColor != color) {
            this.solidColor = color;
            invalidate();
        }
    }

    public void setSolidPercentage(float percentage) {
        float clamped = Math.max(0f, Math.min(1f, percentage));
        if (this.solidPercentage != clamped) {
            this.solidPercentage = clamped;
            invalidate();
        }
    }

    public void setCustomSize(int width, int height) {
        if (this.customWidth != width || this.customHeight != height) {
            this.customWidth = width;
            this.customHeight = height;
            invalidate();
        }
    }

    public void resetCustomSize() {
        if (this.customWidth != Integer.MIN_VALUE || this.customHeight != Integer.MIN_VALUE) {
            this.customWidth = Integer.MIN_VALUE;
            this.customHeight = Integer.MIN_VALUE;
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();

        if (customWidth != Integer.MIN_VALUE) {
            if (customWidth == ViewGroup.LayoutParams.MATCH_PARENT && getParent() instanceof View) {
                width = ((View) getParent()).getWidth();
            } else if (customWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                width = getWidth();
            } else {
                width = customWidth;
            }
        }

        if (customHeight != Integer.MIN_VALUE) {
            if (customHeight == ViewGroup.LayoutParams.MATCH_PARENT && getParent() instanceof View) {
                height = ((View) getParent()).getHeight();
            } else if (customHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = getHeight();
            } else {
                height = customHeight;
            }
        }

        if (width <= 0 || height <= 0) return;

        int startColor = solidColor;
        int endColor = Color.argb(0, Color.red(startColor), Color.green(startColor), Color.blue(startColor));

        Shader shader = null;

        if (shapeMode == ShapeMode.LINEAR) {
            float x0 = 0, y0 = 0, x1 = 0, y1 = 0;

            if (direction == Direction.TOP_TO_BOTTOM) {
                y1 = height;
            } else if (direction == Direction.BOTTOM_TO_TOP) {
                y0 = height;
                y1 = 0;
            } else if (direction == Direction.LEFT_TO_RIGHT) {
                x1 = width;
            } else if (direction == Direction.RIGHT_TO_LEFT) {
                x0 = width;
                x1 = 0;
            }

            shader = new LinearGradient(
                x0, y0, x1, y1,
                new int[]{startColor, startColor, endColor},
                new float[]{0f, solidPercentage, 1f},
                Shader.TileMode.CLAMP
            );
        } else if (shapeMode == ShapeMode.ARC) {
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.max(width, height);

            if (direction == Direction.TOP_TO_BOTTOM) {
                cy = 0;
                radius = height;
            } else if (direction == Direction.BOTTOM_TO_TOP) {
                cy = height;
                radius = height;
            } else if (direction == Direction.LEFT_TO_RIGHT) {
                cx = 0;
                radius = width;
            } else if (direction == Direction.RIGHT_TO_LEFT) {
                cx = width;
                radius = width;
            }

            shader = new RadialGradient(
                cx, cy, radius,
                new int[]{startColor, startColor, endColor},
                new float[]{0f, solidPercentage, 1f},
                Shader.TileMode.CLAMP
            );

            Matrix matrix = new Matrix();
            if (direction == Direction.BOTTOM_TO_TOP || direction == Direction.TOP_TO_BOTTOM) {
                matrix.setScale(width / (radius * 1.5f), 1f, cx, cy);
            } else {
                matrix.setScale(1f, height / (radius * 1.5f), cx, cy);
            }
            shader.setLocalMatrix(matrix);
        }

        if (shader != null) {
            paint.setShader(shader);
            canvas.drawRect(0, 0, width, height, paint);
        }
    }
}
