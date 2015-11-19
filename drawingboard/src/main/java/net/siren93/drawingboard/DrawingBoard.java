package net.siren93.drawingboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by siren93 on 15/11/17.
 */
public class DrawingBoard extends View {

    private final int INVALIDATE = 0x0001;
    private final int CLEAR = 0x0002;

    /**
     * 轨迹采点间隔
     */
    private final int TOUCH_TOLERANCE = 4;
    /**
     * 默认画笔颜色
     */
    private final int DEFAULT_PAINT_COLOR =  0xFF000000;
    /**
     * 默认画笔与橡皮宽度
     */
    private final int DEFAULT_PAINT_WIDTH = 6; //in dp

    /**
     * 绘制与记录触摸轨迹的画笔
     */
    private Paint mPaint = new Paint();
    /**
     * 将已记录触摸轨迹的bitmap绘制到DrawingBoard中的画笔
     */
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    /**
     * 记录触摸轨迹的{@link Bitmap}
     */
    private Bitmap mBitmap;
    /**
     * 用来向{@link  #mBitmap}上绘制触摸轨迹的{@link Canvas}
     */
    private Canvas mCanvas;
    /**
     * 用来绘制橡皮效果的画笔
     */
    private Paint mEraserPaint = new Paint();
    /**
     * 用来记录一次完整的触摸事件(从按下到离开)轨迹的{@link Path}
     */
    private Path mPath = new Path();
    /**
     * 画笔颜色，宽度，橡皮宽度
     */
    private int mPaintColor, mPaintWidth, mEraserWidth;
    /**
     * 记录触摸点的坐标
     */
    private float mX, mY;
    /**
     * 画笔状态（true为画笔，false位橡    */
    private boolean isPainting = true;

    /**
     * 记录轨迹点的列表
     */
    private List<Node> pathNodes = new ArrayList<>();
    /**
     * 记录已撤销轨迹点的列表
     */
    private List<Node> undoNodes = new ArrayList<>();

    /**
     * 播放动画的{@link Executor}
     */
    Executor executor = Executors.newSingleThreadExecutor();
    /**
     * 是否正在播放
     */
    private boolean isPlaying = false;

    public DrawingBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
        initEraser();
    }

    public DrawingBoard(Context context) {
        super(context);
        initPaint();
        initEraser();
    }

    public DrawingBoard(Context context, List<Node> nodes) {
        super(context);
        pathNodes = nodes;
        initPaint();
        initEraser();
    }

    public DrawingBoard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaint();
        initEraser();
    }

    public DrawingBoard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        initPaint();
        initEraser();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initBackground();
    }

    /**
     * 初始化画笔
     */
    public void initPaint() {
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        setPaintColor(DEFAULT_PAINT_COLOR);
        setPaintWidth(DEFAULT_PAINT_WIDTH);
    }

    /**
     * 初始化橡皮
     */
    private void initEraser(){
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setDither(true);
        mEraserPaint.setColor(Color.WHITE);
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);
        //mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        setEraserWidth(DEFAULT_PAINT_WIDTH);
    }

    /**
     * 设置画笔宽度
     * @param width
     */
    public void setPaintWidth(int width) {
        if(width <= 0) width = 1;
        mPaintWidth = width;
        mPaint.setStrokeWidth(dip2px(width));
    }

    /**
     * 设置画笔颜色
     * @param color
     */
    public void setPaintColor(int color) {
        mPaintColor = color;
        mPaint.setColor(color);
    }

    /**
     * 设置橡皮宽度
     * @param width
     */
    public void setEraserWidth(int width) {
        if(width <= 0) width = 1;
        mEraserWidth = width;
        mEraserPaint.setStrokeWidth(dip2px(width));
    }

    /**
     * 设置画笔状态
     * @param isPainting true位画笔， false为橡皮
     */
    public void setPainting(boolean isPainting) {
        this.isPainting = isPainting;
    }

    /**
     * 画笔状态
     * @return
     */
    public boolean isPainting() {
        return isPainting;
    }

    public int getPaintColor() {
        return mPaintColor;
    }

    public int getPaintWidth() {
        return mPaintWidth;
    }

    public int getEraserWidth() {
        return mEraserWidth;
    }

    /**
     * 初始化白色画板背景
     */
    private void initBackground(){
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        int width = getWidth();
        int height = getHeight();
        if(width == 0 || height == 0) return;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if(mCanvas == null) {
            mCanvas = new Canvas(mBitmap);
        } else {
            mCanvas.setBitmap(mBitmap);
        }
        mCanvas.drawColor(Color.WHITE);
    }

    /**
     * 返回绘制号的位图
     * @return
     */
    public Bitmap getPaintedBitmap() {
        return mBitmap;
    }

    /**
     * 返回触摸路径列表
     * @return
     */
    public List<Node> getPathNodes() {
        Node[] nodeArray = new Node[pathNodes.size()];
        List<Node> newList = Arrays.asList(nodeArray);
        Collections.copy(newList, pathNodes);
        return newList;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //先将已保存在位图中的轨迹绘制到背景
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        //载绘制新的轨迹
        if(isPainting) {
            canvas.drawPath(mPath, mPaint);
        } else {
            canvas.drawPath(mPath, mEraserPaint);
        }
    }

    /**
     * 重置画笔橡皮位默认状态
     */
    public void reset(){
        setPaintColor(DEFAULT_PAINT_COLOR);
        setPaintWidth(DEFAULT_PAINT_WIDTH);
        setEraserWidth(DEFAULT_PAINT_WIDTH);
        isPainting = true;
    }

    /**
     * 清除画板内容
     */
    public void clear() {
        //将清除也记录为一次轨迹操作，使其可以撤销与重做
        recordNode(-1, -1, -1);
        initBackground();
        mPath.reset();
        invalidate();
    }

    /**
     * 撤销
     * @return
     */
    public boolean undo() {
        int originSize = pathNodes.size();
        if (originSize == 0) return false;
        //如果是clear操作,则直接撤销一个点
        if(pathNodes.get(originSize - 1).touchEvent == -1) {
            undoNodes.add(pathNodes.remove(originSize - 1));
            drawNodes(pathNodes);
            return true;
        }
        //将上一次完整的触摸操作从已记录的轨迹列表中取出放到撤销列表
        for (int i = originSize - 1; i >= 0; i--) {
            Node node = pathNodes.remove(i);
            undoNodes.add(node);
            if(node.touchEvent == MotionEvent.ACTION_DOWN) {
                break;
            }
        }
        drawNodes(pathNodes);
        return true;
    }

    /**
     * 重做
     * @return
     */
    public boolean redo() {
        int originSize = undoNodes.size();
        if (originSize == 0) return false;
        if(undoNodes.get(originSize - 1).touchEvent == -1) {
            pathNodes.add(undoNodes.remove(originSize - 1));
            drawNodes(pathNodes);
            return true;
        }
        for (int i = originSize - 1; i >= 0; i--) {
            Node node = undoNodes.remove(i);
            pathNodes.add(node);
            if (node.touchEvent == MotionEvent.ACTION_UP) {
                break;
            }
        }
        drawNodes(pathNodes);
        return true;
    }

    /**
     * 触摸事件按下
     * @param x
     * @param y
     */
    private void touchDown(float x, float y) {
        undoNodes.clear();
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        recordNode(x, y, MotionEvent.ACTION_DOWN);
    }

    /**
     * 触摸移动
     * @param x
     * @param y
     */
    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
            recordNode(x, y, MotionEvent.ACTION_MOVE);
        }
    }

    /**
     * 触摸事件结束
     * @param x
     * @param y
     */
    private void touchUp(float x, float y) {
        mPath.lineTo(mX, mY);
        if (isPainting) {
            mCanvas.drawPath(mPath, mPaint);
        } else {
            mCanvas.drawPath(mPath, mEraserPaint);
        }
        mPath.reset();
        recordNode(x, y, MotionEvent.ACTION_UP);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isPlaying) return true;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                touchDown(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE :
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP :
                touchUp(x, y);
                invalidate();
                break;
        }
        return true;
    }

    /**
     * 从dp转换位pix的方法
     * @param dpValue
     * @return
     */
    public int dip2px(float dpValue) {
        final float scale = this.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //记得释放Bitmap
        if(mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
    }

    /**
     * 用来记录触摸轨迹点的模型类
     */
    public class Node {
        public float x, y;
        public int paintColor;
        public int paintWidth;
        /**
         * 画笔还是橡皮
         */
        public boolean isPainting;
        public int touchEvent;
        public long timeStamp;

        @Override
        public String toString() {
            return "{ 'x' : " + x + ", 'y' : " + y + ", 'paintColor : '" + paintColor +
                    ", 'paintWidth:' " + paintWidth + ", 'isPainting:' " + isPainting +
                    ", 'touchEvent': " + touchEvent + ", 'timeStamp:' " + timeStamp + "}";
        }
    }

    /**
     * 记录轨迹点
     * @param x
     * @param y
     * @param touchEvent
     */
    private void recordNode(float x, float y, int touchEvent) {
        Node node = new Node();
        node.x = x;
        node.y = y;
        node.touchEvent = touchEvent;
        node.timeStamp = System.currentTimeMillis();
        if(touchEvent == MotionEvent.ACTION_MOVE) {
            pathNodes.add(node);
            //为移动事件时不用记录画笔和橡皮状态
            return;
        }
        if(isPainting()) {
            node.paintColor = getPaintColor();
            node.paintWidth = getPaintWidth();
        } else {
            node.paintWidth = getEraserWidth();
        }
        node.isPainting = isPainting();
        pathNodes.add(node);
    }

    /**
     * 根据触摸轨迹列表绘制图形
     * @param nodes
     */
    public void drawNodes(List<Node> nodes){
        initBackground();
        invalidate();
        Path path = new Path();
        for(int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if(node.touchEvent == -1) {
                initBackground();
                invalidate();
            }
            if(node.touchEvent == MotionEvent.ACTION_DOWN) {
                path.reset();
                path.moveTo(node.x, node.y);
            }
            if(node.touchEvent == MotionEvent.ACTION_MOVE) {
                Node preNode = nodes.get(i - 1);
                path.quadTo(preNode.x, preNode.y, (preNode.x + node.x) / 2, (preNode.y + node.y) / 2);
            }
            if(node.touchEvent == MotionEvent.ACTION_UP) {
                path.lineTo(node.x, node.y);
                if(node.isPainting) {
                    mPaint.setColor(node.paintColor);
                    mPaint.setStrokeWidth(dip2px(node.paintWidth));
                    mCanvas.drawPath(path, mPaint);
                    invalidate();
                    mPaint.setColor(mPaintColor);
                    mPaint.setStrokeWidth(dip2px(mPaintWidth));
                } else {
                    mEraserPaint.setStrokeWidth(dip2px(node.paintWidth));
                    mCanvas.drawPath(path, mEraserPaint);
                    invalidate();
                    mEraserPaint.setStrokeWidth(dip2px(mEraserWidth));
                }
            }
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INVALIDATE) {
                invalidate();
            }
            if (msg.what == CLEAR) {
                clear();
            }
        }
    };

    /**
     * 实现播放动画
     */
    class PlayerRunnable implements Runnable  {

        private List<Node> nodes;

        public PlayerRunnable(List<Node> nodes) {
            this.nodes = nodes;
        }

        @Override
        public void run() {
            isPlaying = true;
            //保存画板状态
            int originPaintColor = getPaintColor();
            int originPaintWidth = getPaintWidth();
            int originEraserWidth = getEraserWidth();
            boolean originPaintStatus = isPainting();
            long time = 0;
            for(int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if(i < nodes.size() - 1) {
                    time = nodes.get(i + 1).timeStamp - node.timeStamp;
                    //当两次操作时间间隔太长时将其缩短为一秒
                    if(time > 1000) time = 1000;
                }
                if(node.touchEvent == -1) {
                    mHandler.sendEmptyMessage(CLEAR);
                }
                if(node.touchEvent == MotionEvent.ACTION_DOWN) {
                    setPainting(node.isPainting);
                    if(node.isPainting) {
                        setPaintColor(node.paintColor);
                        setPaintWidth(node.paintWidth);
                    } else {
                        setEraserWidth(node.paintWidth);
                    }
                    touchDown(node.x, node.y);
                    mHandler.sendEmptyMessage(INVALIDATE);
                }
                if(node.touchEvent == MotionEvent.ACTION_MOVE) {
                    touchMove(node.x, node.y);
                    mHandler.sendEmptyMessage(INVALIDATE);
                }
                if(node.touchEvent == MotionEvent.ACTION_UP) {
                    touchUp(node.x, node.y);
                    mHandler.sendEmptyMessage(INVALIDATE);
                }
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //还原画板状态
            setPaintColor(originPaintColor);
            setPaintWidth(originPaintWidth);
            setEraserWidth(originEraserWidth);
            setPainting(originPaintStatus);
            isPlaying = false;
        }
    }

    /**
     * 播放动画
     * @param nodes 轨迹记录
     * @param isAppending 动画内容是否添加在已有的轨迹后面
     */
    public void play(List<Node> nodes, boolean isAppending) {
        initBackground();
        if (mBitmap == null) return;
        if(!isAppending) {
            pathNodes.clear();
        }
        executor.execute(new PlayerRunnable(nodes));
    }

}
