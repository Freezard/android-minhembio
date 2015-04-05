package com.example.projekt.webview;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.projekt.R;

/**
 * WebViewActivity.java
 * 
 * Activity with a web view to let users browse reviews and
 * add selected ones to the database.
 */
public class WebViewActivity extends Activity {
	private WebView webView;
	private ArrayList<Integer> addCustomList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_form);

		addCustomList = new ArrayList<Integer>();
		
		Intent i = getIntent();
		String url = i.getStringExtra("URL");
	
		webView = (WebView) findViewById(R.id.webView);
		webView.setWebViewClient(new MyWebViewClient());
		webView.loadUrl(url);
	}
	
	@Override
	public void finish() {
		// Return the add custom list through an intent on activity exit
	    Intent resultIntent = new Intent();
	    resultIntent.putIntegerArrayListExtra("addCustomList", addCustomList);
	    setResult(Activity.RESULT_OK, resultIntent);

	    super.finish();
	}
	
	private class MyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// User clicked a review, add it to the add custom list
			if (url.matches("(.*)spelrec/\\d+")) {
					String id = url.substring(url.lastIndexOf('/') + 1);				
					addCustomList.add(Integer.parseInt(id));
										
					return true;
			}
			
			return false;
		}
	}
}
