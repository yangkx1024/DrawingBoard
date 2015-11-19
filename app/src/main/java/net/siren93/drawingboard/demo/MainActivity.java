package net.siren93.drawingboard.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import net.siren93.drawingboard.DrawingBoard;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    DrawingBoard drawingBoard;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawingBoard = (DrawingBoard) findViewById(R.id.mDrawingBoard);
        image = (ImageView) findViewById(R.id.iv_pic);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void adPaint(View v) {
        drawingBoard.setPaintWidth(drawingBoard.getPaintWidth() + 1);
    }

    public void changePaint(View v) {
        if(drawingBoard.isPainting()) {
            drawingBoard.setPainting(false);
            ((Button) v).setText("切换画笔");
        } else {
            drawingBoard.setPainting(true);
            ((Button) v).setText("开启橡皮");
        }
    }

    public void adEraser(View v) {
        drawingBoard.setEraserWidth(drawingBoard.getEraserWidth() + 1);
    }

    public void reset(View v) {
        drawingBoard.reset();
    }

    public void clear(View v) {
        drawingBoard.clear();
    }

    public void undo(View v) {
        drawingBoard.undo();
    }

    public void redo(View v) {
        drawingBoard.redo();
    }

    public void showImg(View v) {
        if (image.getVisibility() == View.GONE) {
            image.setImageBitmap(drawingBoard.getPaintedBitmap());
            image.setVisibility(View.VISIBLE);
            drawingBoard.setVisibility(View.GONE);
        } else {
            image.setVisibility(View.GONE);
            drawingBoard.setVisibility(View.VISIBLE);
        }
    }

    public void play(View v) {
        List<DrawingBoard.Node> nodes = drawingBoard.getPathNodes();
        drawingBoard.play(nodes, false);
    }
}
