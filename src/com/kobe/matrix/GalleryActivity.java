package com.kobe.matrix;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class GalleryActivity extends Activity {

    private GalleryView galleryView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_gallery);
        galleryView = (GalleryView) findViewById(R.id.galleryView);
        galleryView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(ViewGroup parent, View view, int position) {
                Toast.makeText(getApplicationContext(), "image at "+position+" clicked.",Toast.LENGTH_LONG).show();
            }
        });
	}
	
}
