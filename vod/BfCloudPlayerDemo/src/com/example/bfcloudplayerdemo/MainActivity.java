package com.example.bfcloudplayerdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onVodButtonClick(View v) {
		Intent intent = new Intent();
		intent.setClass(this, VodActivity.class);
		startActivity(intent);
	}

	public void onLiveButtonClick(View v) {
		Intent intent = new Intent();
		intent.setClass(this, LiveActivity.class);
		startActivity(intent);
	}
}
