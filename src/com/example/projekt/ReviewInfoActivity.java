package com.example.projekt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.projekt.model.Pair;
import com.example.projekt.model.Review;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * ReviewInfoActivity.java
 * 
 * Activity that takes a bundled review and displays the visits history.
 */
public class ReviewInfoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_review_info);

		Intent i = getIntent();
		Review review = (Review) i.getSerializableExtra("review");
		
		updateList(review);
	}
	
	// Displays the visits history of a review
	private void updateList(Review review) {
		ListView listView = (ListView) findViewById(R.id.listView_review_info);

		List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

		Map<String, String> columns = new HashMap<String, String>();
		columns.put("game", "Date");
		columns.put("reviewer", "Visits");
		rows.add(columns);
		
		// Get the visit history and add them in reverse order, from newest to oldest
		List<Pair<Date, Integer>> reviews = review.getVisits();
		
		for (int i = reviews.size() - 1; i >= 0; i--) {
			columns = new HashMap<String, String>();
			columns.put("game", new SimpleDateFormat("yyyy-MM-dd").format(reviews.get(i).getFirst()));
			columns.put("reviewer", Integer.toString(reviews.get(i).getSecond()));
			rows.add(columns);
		}

		// Uses same layout as the main list view
		SimpleAdapter adapter = new SimpleAdapter(this, rows,
				R.layout.list_view_columns, new String[] { "game", "reviewer" },
				new int[] { R.id.columnGame, R.id.columnReviewer });
		listView.setAdapter(adapter);

	}
}
