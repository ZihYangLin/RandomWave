package com.yangpingapps;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.yangpingapps.yp.randomwave.R;

import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * Created by TzYang on 2018/5/20.
 */

public class RandomWave extends View {
    /*類型常數*/
    public enum Gravity {
        Top(1), Bottom(2);
        int value;

        Gravity(int value) {
            this.value = value;
        }

        static Gravity fromValue(int value) {
            for (Gravity gravity : values()) {
                if (gravity.value == value) return gravity;
            }
            return Bottom;
        }
    }

    public enum Direction {
        Left(1), Right(2);
        int value;

        Direction(int value) {
            this.value = value;
        }

        static Direction fromValue(int value) {
            for (Direction direction : values()) {
                if (direction.value == value) return direction;
            }
            return Right;
        }
    }


    /*Default Value*/
    private static final boolean DEFAULT_IS_ANIMATION = false;
    private static final int DEFAULT_WAVE_COLOR = Color.parseColor("#40FF0000");
    private static final float DEFAULT_NUMBER_OF_WAVES = 3.5f;
    private static final Gravity DEFAULT_WAVE_GRAVITY = Gravity.Bottom;
    private static final Direction DEFAULT_WAVE_DIRECTION = Direction.Right;


    /*參數*/
    private boolean isAnimation = DEFAULT_IS_ANIMATION;
    private float waveCount = DEFAULT_NUMBER_OF_WAVES;
    private Gravity waveGravity = DEFAULT_WAVE_GRAVITY;
    private Direction waveDirection = DEFAULT_WAVE_DIRECTION;


    private HandlerThread thread = new HandlerThread("YPWaveView_" + hashCode());
    private Handler animHandler, uiHandler;
    private int[] randomArray1 = new int[100];
    private int frequency = 1;
    private float shift = 0;
    private Paint mPaint3;
    private Path path1;
    private boolean isRefresh = false;
    private int lastWaveX = 0;

    public RandomWave(Context context) {
        this(context, null, 0);
    }

    public RandomWave(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RandomWave(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        /*取得XML參數*/
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RandomWave, defStyleAttr, 0);
        isAnimation = attributes.getBoolean(R.styleable.RandomWave_animatorEnable, DEFAULT_IS_ANIMATION);
        waveCount = attributes.getFloat(R.styleable.RandomWave_numberOfWaves, DEFAULT_NUMBER_OF_WAVES);
        int waveColor = attributes.getColor(R.styleable.RandomWave_waveColor, DEFAULT_WAVE_COLOR);
        waveGravity = Gravity.fromValue(attributes.getInt(R.styleable.RandomWave_waveGravity, DEFAULT_WAVE_GRAVITY.value));
        waveDirection = Direction.fromValue(attributes.getInt(R.styleable.RandomWave_direction, DEFAULT_WAVE_DIRECTION.value));

        /*設定行徑方向*/
        switch (waveDirection) {
            case Left:
                setScaleX(1f);
                break;
            case Right:
                setScaleX(-1f);
                break;
        }

        /*設定畫筆*/
        mPaint3 = new Paint();
        mPaint3.setColor(waveColor);
        mPaint3.setStyle(Paint.Style.FILL);
        mPaint3.setStrokeWidth(8f);
        mPaint3.setAntiAlias(true);
        refresh();
        path1 = createWave();
        if (path1 != null) {
            path1.addPath(createWave());
        }

        /*開啟動畫執行緒*/
        thread.start();
        animHandler = new Handler(thread.getLooper());
        uiHandler = new UIHandler(new WeakReference<View>(this));


    }

    @Override
    protected void onDetachedFromWindow() {
        if (animHandler != null) {
            animHandler.removeCallbacksAndMessages(null);
        }
        if (thread != null) {
            thread.quit();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (path1 == null) {
            refresh();
            path1 = createWave();
            if (path1 != null) {
                path1.addPath(createWave());
            }
        }
        if (isAnimation) {
            runAnimation();
        }
        invalidate();
    }

    public void setAnimation(boolean isAnimation) {
        this.isAnimation = isAnimation;
        if (this.isAnimation) {
            runAnimation();
        }
    }

    /**
     * 開始動畫
     */
    private void runAnimation() {
        animHandler.post(new Runnable() {
            @Override
            public void run() {
                shift -= 2; //位移量
                if (shift <= -lastWaveX) {
                    shift = 0;
                    lastWaveX = 0;
                }
                path1 = createWave();
                if (Math.abs(shift) % getWidth() > getWidth() - 50) {
                    if (!isRefresh) {

                    }
                } else {
                    isRefresh = false;
                }
                if (isAnimation) {
                    Message message = Message.obtain(uiHandler);
                    message.sendToTarget();
                    animHandler.postDelayed(this, 20);
                }
            }
        });
    }

    /**
     * 產生20個亂數振幅
     */
    private void refresh() {
        for (int i = 0; i < 100; i++) {
            int random = 0;
            while (random < getHeight() / 16) {
                random = new Random().nextInt((getHeight() / 2));
            }
            randomArray1[i] = random;
        }
    }

    /**
     * 二階貝塞爾曲線(Bézier curve)
     * B(t) = X(1-t)^2 + 2t(1-t)Y + Zt^2 , 0 <= t <= n
     *
     * @return 路徑
     */
    private Path createWave() {
        if (getWidth() != 0 && getHeight() != 0) {
            Path path = new Path();
            /*第一個波的寬度*/
            float widthUnit = getWidth() / waveCount;
            /*移至起始點*/
            int count = 0;
            path.moveTo(0, getHeight() / 2);
            /*建立第一個波*/
            float x1 = shift;
            float y1 = getHeight() / 2;
            float x2 = widthUnit / 2f + shift;
            float y2 = (getHeight() / 32) * (frequency / 4f);
            float x3 = widthUnit + shift;
            float y3 = getHeight() / 2f;
            path.cubicTo(x1, y1, x2, y2, x3, y3);
            count++;
            /*建立後面隨機波*/
            for (int i = 0; i < 19; i++) {
                /*計算上一個波的斜率*/
                float m = (y3 - y2) / (x3 - x2);
                /*起始點為上一個波的結束點*/
                x1 = x3;
                y1 = y3;
                /*隨機振幅*/
                if (count % 2 == 0) {
                    y2 = (getHeight() / 2) - randomArray1[i % 100];
                } else {
                    y2 = (getHeight() / 2) + randomArray1[i % 100];
                }
                /*依照隨機振幅需得相同斜率*/
                x2 = (y2 - y1 + (m * x1)) / m;
                /*結束點x為控制點x的兩倍距離*/
                x3 = x1 + (2 * (x2 - x1));
                /*結束點與起始點的y相同*/
                y3 = y1;
                /*畫出貝塞爾曲線*/
                path.cubicTo(x1, y1, x2, y2, x3, y3);
                count++;
            }
            if (lastWaveX == 0) {
                lastWaveX = (int) x3;
            }
            /*建立第一個波*/
            x1 = x3;
            y1 = getHeight() / 2;
            x2 = x3 + widthUnit / 2;
            y2 = (getHeight() / 32) * (frequency / 4f);
            x3 = x3 + widthUnit;
            y3 = getHeight() / 2;
            path.cubicTo(x1, y1, x2, y2, x3, y3);
            count++;
            /*建立後面隨機波*/
            for (int i = 0; i < 19; i++) {
                /*計算上一個波的斜率*/
                float m = (y3 - y2) / (x3 - x2);
                /*起始點為上一個波的結束點*/
                x1 = x3;
                y1 = y3;
                /*隨機振幅*/
                if (count % 2 == 0) {
                    y2 = (getHeight() / 2) - randomArray1[i % 10];
                } else {
                    y2 = (getHeight() / 2) + randomArray1[i % 10];
                }
                /*依照隨機振幅需得相同斜率*/
                x2 = (y2 - y1 + (m * x1)) / m;
                /*結束點x為控制點x的兩倍距離*/
                x3 = x1 + (2 * (x2 - x1));
                if (i == 9) {
                    if (lastWaveX == 0) {
                        lastWaveX = (int) x1;
                    }
                }
                /*結束點與起始點的y相同*/
                y3 = y1;
                /*畫出貝塞爾曲線*/
                path.cubicTo(x1, y1, x2, y2, x3, y3);
                count++;
            }
            switch (waveGravity) {
                case Top:
                    path.lineTo(x3, 0);
                    path.lineTo(0, 0);
                    path.lineTo(0, getHeight() / 2);
                    break;
                case Bottom:
                    path.lineTo(x3, getHeight());
                    path.lineTo(0, getHeight());
                    path.lineTo(0, getHeight() / 2);
                    break;
            }

            return path;
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null && getWidth() != 0 && getHeight() != 0) {
            if (path1 != null) {
                canvas.drawPath(path1, mPaint3);
            }
        }
    }

    private static class UIHandler extends Handler {
        private final View mView;

        UIHandler(WeakReference<View> view) {
            super(Looper.getMainLooper());
            mView = view.get();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mView != null) {
                mView.invalidate();
            }
        }
    }
}