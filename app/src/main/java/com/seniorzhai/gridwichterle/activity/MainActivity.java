package com.seniorzhai.gridwichterle.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.seniorzhai.gridwichterle.R;
import com.seniorzhai.gridwichterle.services.GridOverlayService;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		Intent intent = new Intent(this, GridOverlayService.class);
	    startService(intent);

	    finish();
    }



}
