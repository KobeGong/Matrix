package com.kobe.matrix;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnSeekBarChangeListener{

	private Camera camera;
    // views
    private SeekBar seekbarXRotate;
    private SeekBar seekbarYRotate;
    private SeekBar seekbarZRotate;
    private TextView txtXRotate;
    private TextView txtYRotate;
    private TextView txtZRotate;
    private SeekBar seekbarXSkew;
    private SeekBar seekbarYSkew;
    private SeekBar seekbarZTranslate;
    private TextView txtXTranslate;
    private TextView txtYTranslate;
    private TextView txtZTranslate;
    private ImageView imgResult;
    // integer params
    private int rotateX, rotateY, rotateZ;
    private float skewX, skewY;
    private int translateZ;
    BitmapDrawable tmpBitDra;
    Bitmap tmpBit;
  
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // camera
        camera = new Camera();
        // initViews
        // rotate
        seekbarXRotate = (SeekBar) findViewById(R.id.seekbarXRotate);
        seekbarXRotate.setOnSeekBarChangeListener(this);
        seekbarYRotate = (SeekBar) findViewById(R.id.seekbarYRotate);
        seekbarYRotate.setOnSeekBarChangeListener(this);
        seekbarZRotate = (SeekBar) findViewById(R.id.seekbarZRotate);
        seekbarZRotate.setOnSeekBarChangeListener(this);
        txtXRotate = (TextView) findViewById(R.id.txtXRotate);
        txtYRotate = (TextView) findViewById(R.id.txtYRotate);
        txtZRotate = (TextView) findViewById(R.id.txtZRotate);
        // translate
        seekbarXSkew = (SeekBar) findViewById(R.id.seekbarXSkew);
        seekbarXSkew.setOnSeekBarChangeListener(this);
        seekbarYSkew = (SeekBar) findViewById(R.id.seekbarYSkew);
        seekbarYSkew.setOnSeekBarChangeListener(this);
        seekbarZTranslate = (SeekBar) findViewById(R.id.seekbarZTranslate);
        seekbarZTranslate.setOnSeekBarChangeListener(this);
        txtXTranslate = (TextView) findViewById(R.id.txtXSkew);
        txtYTranslate = (TextView) findViewById(R.id.txtYSkew);
        txtZTranslate = (TextView) findViewById(R.id.txtZTranslate);
        imgResult = (ImageView) findViewById(R.id.imgResult);
        
        tmpBitDra = (BitmapDrawable) getResources().getDrawable(R.drawable.aa);
        tmpBit = tmpBitDra.getBitmap();
        // refresh
//        refreshImage();
    }
  
    private void refreshImage() {

        camera.save();
        Matrix matrix = new Matrix();
        // rotate
        camera.rotateX(rotateX);
        camera.rotateY(rotateY);
        camera.rotateZ(rotateZ);

        int translateY = 0;
        camera.translate(-translateY, -translateY, translateZ);
        camera.getMatrix(matrix);

        camera.restore();
       
        matrix.preTranslate(-tmpBit.getWidth() >> 1, -tmpBit.getHeight() >> 1);
        matrix.postTranslate(tmpBit.getWidth() >> 1, tmpBit.getHeight() >> 1);
        matrix.postSkew(skewX, skewY);

        Bitmap newBit = null;
        try {
            newBit = Bitmap.createBitmap(tmpBit, 0, 0, tmpBit.getWidth(), tmpBit.getHeight(), matrix, true);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        if (newBit != null) {
            imgResult.setImageBitmap(newBit);
        }
    }
  
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == seekbarXRotate) {
            txtXRotate.setText(progress + ".");
            rotateX = progress;
        } else if (seekBar == seekbarYRotate) {
            txtYRotate.setText(progress + ".");
            rotateY = progress;
        } else if (seekBar == seekbarZRotate) {
            txtZRotate.setText(progress + ".");
            rotateZ = progress;
        } else if (seekBar == seekbarXSkew) {
            skewX = (progress - 100) * 1.0f / 100;
            txtXTranslate.setText(String.valueOf(skewX));
        } else if (seekBar == seekbarYSkew) {
            skewY = (progress - 100) * 1.0f / 100;
            txtYTranslate.setText(String.valueOf(skewY));
        } else if (seekBar == seekbarZTranslate) {
            translateZ = progress - 100;
            txtZTranslate.setText(String.valueOf(translateZ));
        }
        refreshImage();
    }
  
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
  
    }
  
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
  
    }
}
