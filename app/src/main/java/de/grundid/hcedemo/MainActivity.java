package de.grundid.hcedemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.content.Intent;

public class MainActivity extends Activity {

	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//		listView = (ListView)findViewById(R.id.listView);
//		listView.setAdapter(isoDepAdapter);
		startService(new Intent(this, MyHostApduService.class));
	}
}
