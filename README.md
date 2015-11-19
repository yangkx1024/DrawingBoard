# Android画板

之前见到一个需求，在视频通话时，希望能将手指在屏幕上绘制的图形实时发送给对方，为了实现这个需求，在github转了一圈，发现一个名叫[JustWeTools](https://github.com/lfkdsk/JustWeTools)的开源项目，借鉴这个项目中画板的思想，经过一番思考，实现了一个功能比较完善的画板。

### 功能点：

1. 画笔粗细颜色可调，橡皮粗细可调。
2. 可无限*undo*与*rodo*
3. 能将绘制的图形保存位图片
4. 可以记录绘画过程，能够以动画形式播放，也能够以文件形式保存。

### 基本思路：

继承View类，重写`onTouch()`与`onDraw()`方法。通过监听touch事件在View上绘制轨迹。

``` java
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
```

其中每次都调用`invalidate()`方法，通知其自身重绘，将记录的轨迹绘制在View中。

接下来看`touchDown()`， `touchMove()`,  `touchUp()`三个方法：

``` java
	private void touchDown(float x, float y) {
        undoNodes.clear();
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        recordNode(x, y, MotionEvent.ACTION_DOWN);
    }
	private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= 
            TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2,
                         (y + mY) / 2);
            mX = x;
            mY = y;
            recordNode(x, y, MotionEvent.ACTION_MOVE);
        }
    }
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
```

一次完整的*touch*事件以*Action_Down*开始，经过一段*Action_Move*,以*Action_Up*结束。这里使用一个全局的*Path*实例*mPath*来记录*touch*的轨迹，手指移动时，将轨迹记录在*mPath*中，以`invalidate()`被调用后，`nDraw()`会将这段轨迹绘制在屏幕上。当*touch*事件结束时，将已经画好的一笔记录在一个缓存的Bitmap中（这里的*mCanvas*是从一个全局的Bitmap实例即*mcanvas*创建的），并清空*mPath*，为下一笔作准备。在之后的`onDraw()`中，同样会将Bitmap中缓存的图案绘制出来。

``` java
	protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //先将已保存在位图中的轨迹绘制到背景
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        //再绘制新的轨迹
        if(isPainting) {
            canvas.drawPath(mPath, mPaint);
        } else {
            canvas.drawPath(mPath, mEraserPaint);
        }
    }
```

此外，所有的轨迹点都经由`recordNode()`方法记录在了一个全局的列表*pathNodes*中，*undo*时，将*pathNodes*中最新记录的完整的一笔（即自*down*始，至*up*终的一系列点）移除，放到另一列表*undoNodes*中，再绘制*pathNode*中所有的轨迹。而*redo*则将*undoNodes*中的轨迹放回*pathNodes*。在`recordNode()`记录轨迹点时，给每一个点打上一个时间戳，此后如果在绘制*pathNodes*时，在两点绘制之间停顿两点时间戳之差的时间，就能以动画的形式完全的还原整个*pathNodes*的绘制过程了。

下面是实现播放*pathNodes*的过程

``` java
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
        time = nodes.get(i + 1).timeStamp - 
          node.timeStamp;
        //当两次操作时间间隔太长时将其缩短为一秒
        if(time > 1000) time = 1000;
      }
      //清屏的动作
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
```

