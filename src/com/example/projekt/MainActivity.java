package com.example.projekt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.projekt.model.Review;
import com.example.projekt.model.ReviewDatabase;
import com.example.projekt.utils.Utils;
import com.example.projekt.webview.WebViewActivity;

/**
 * MainActivity.java
 * 
 * Activity that fetches a number of reviews from Minhembio.com and keeps the
 * information in a database saved to a file. The review information is
 * presented in a list view including game name, author and the number of visits
 * on a certain date. Visits and dates history are stored in the database.
 * 
 * The user can click on an add button to manually add reviews by clicking on
 * reviews on the web site, an add latest button to automatically add the latest
 * reviews until max reviews has been reached, an update button to update all
 * the existing reviews with the new visits data, and a clear button which
 * clears the database.
 * 
 * To see the visits history, the user can click on a row in the list view. The
 * user can also perform a long click on a row to browse to the review and read
 * it in a web view.
 */
public class MainActivity extends Activity {
	private ReviewDatabase database;
	private List<Integer> addCustomList;
	private final String DEFAULT_URL = "http://www.minhembio.com/spelrec/";
	private final String DEFAULT_LOCATION = "/MHBStats/reviews.dat";
	private final int MAX_REVIEWS = 10; // Not too large or will result in
										// outofmemory exception

	private enum ReviewFunction {
		ADD_CUSTOM, ADD_LATEST, UPDATE
	};

	private final int REQUST_CODE_ADD_CUSTOM = 0;
	private ProgressDialog progressDialog;
	private Builder mostVisitedDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initializeComponents();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	@Deprecated
	protected void onPrepareDialog(int id, Dialog dialog) {
		progressDialog.setProgress(0);
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			Intent i = new Intent(getApplicationContext(),
					WebViewActivity.class);
			i.putExtra("URL", DEFAULT_URL);
			startActivityForResult(i, REQUST_CODE_ADD_CUSTOM);
			return true;
		case R.id.action_update:
			new AddUpdateReviewsTask(ReviewFunction.UPDATE)
					.execute(ReviewFunction.UPDATE);
			return true;
		case R.id.action_add_latest:
			new AddUpdateReviewsTask(ReviewFunction.ADD_LATEST)
					.execute(ReviewFunction.ADD_LATEST);
			return true;
		case R.id.action_clear:
			clearDatabase();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		updateList();

		super.onResume();
	}

	// OnItemClickListener for the list view
	final OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@SuppressWarnings("unchecked")
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {

			Map<String, String> clickedRow = (HashMap<String, String>) parent
					.getAdapter().getItem(position);

			// Pass the clicked review to ReviewInfoActivity
			if (clickedRow.containsKey("id")) {
				Review review = database.getReview(Integer.parseInt(clickedRow
						.get("id")));

				Intent i = new Intent(getApplicationContext(),
						ReviewInfoActivity.class);
				i.putExtra("review", review);
				startActivity(i);
			}
		}
	};

	// OnItemLongClickListener for the list view
	final OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
		@SuppressWarnings("unchecked")
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {

			Map<String, String> clickedRow = (HashMap<String, String>) parent
					.getAdapter().getItem(position);

			// Pass the review URL to WebViewActivity which displays the web
			// page
			if (clickedRow.containsKey("id")) {
				Intent i = new Intent(getApplicationContext(),
						WebViewActivity.class);
				i.putExtra("URL", DEFAULT_URL + clickedRow.get("id"));
				startActivity(i);
			}

			return true;
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case (REQUST_CODE_ADD_CUSTOM):
			// Add all the reviews the user clicked
			if (resultCode == Activity.RESULT_OK) {
				addCustomList = data.getIntegerArrayListExtra("addCustomList");
				new AddUpdateReviewsTask(ReviewFunction.ADD_CUSTOM)
						.execute(ReviewFunction.ADD_CUSTOM);
			}
			break;
		}
	}

	private void initializeComponents() {
		ListView listView = (ListView) findViewById(R.id.listView_reviews);

		listView.setOnItemClickListener(onItemClickListener);
		listView.setOnItemLongClickListener(onItemLongClickListener);

		if (loadReviews()) {
			TextView textView = (TextView) findViewById(R.id.textView_last_updated);

			textView.setText(getString(R.string.last_updated)
					+ database.getLastUpdated());
		} else
			database = new ReviewDatabase();

		progressDialog = new ProgressDialog(this);
		progressDialog.setIndeterminate(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		
		mostVisitedDialog = new AlertDialog.Builder(this);
		mostVisitedDialog.setTitle(R.string.most_visited);
		mostVisitedDialog.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                    	dialog.dismiss();
                    }
                });
	}

	// Saves the database to file on external storage
	private boolean saveReviews() {
		if (Utils.isExternalStorageWritable()) {
			String fileLocation = Environment.getExternalStorageDirectory()
					+ DEFAULT_LOCATION;

			if (!Utils.isDirectory(fileLocation))
				Utils.makeDirs(Environment.getExternalStorageDirectory()
						+ "/MHBStats/");

			try {
				FileOutputStream fos = new FileOutputStream(fileLocation);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(database);
				oos.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		unableToAccessFile();

		return false;
	}

	// Loads the database from file on external storage
	private boolean loadReviews() {
		if (Utils.isExternalStorageReadable()) {
			String fileLocation = Environment.getExternalStorageDirectory()
					+ DEFAULT_LOCATION;

			if (Utils.fileExists(fileLocation))
				try {
					FileInputStream fis = new FileInputStream(fileLocation);
					ObjectInputStream ois = new ObjectInputStream(fis);
					database = (ReviewDatabase) ois.readObject();
					ois.close();
					return true;
				} catch (Exception e) {
					unableToAccessFile();
					e.printStackTrace();
				}
		} else
			unableToAccessFile();

		return false;
	}

	// Show toast message when save/load fails
	private void unableToAccessFile() {
		Toast.makeText(getApplicationContext(), R.string.access_file_failed,
				Toast.LENGTH_SHORT).show();
	}

	private void clearDatabase() {
		database = new ReviewDatabase();

		ListView listView = (ListView) findViewById(R.id.listView_reviews);
		listView.setAdapter(null);

		TextView textViewLastUpdated = (TextView) findViewById(R.id.textView_last_updated);
		textViewLastUpdated.setText("");

		saveReviews();
	}

	// Displays review name, author and highest visits for every review
	private void updateList() {
		ListView listView = (ListView) findViewById(R.id.listView_reviews);

		// Each row is a map that maps text to columns
		List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

		// Header row
		Map<String, String> columns = new HashMap<String, String>();
		columns.put("game", "Game");
		columns.put("reviewer", "Reviewer");
		columns.put("visits", "Visits");
		rows.add(columns);

		// Get all reviews and add them in reverse order, from newest to oldest
		List<Review> reviews = new ArrayList<Review>(database.getAllReviews());

		for (int i = reviews.size() - 1; i >= 0; i--) {
			columns = new HashMap<String, String>();
			columns.put("game", reviews.get(i).getName());
			columns.put("reviewer", reviews.get(i).getReviewer());
			// Hidden column to easily access the review when clicked
			columns.put("id", Integer.toString(reviews.get(i).getId()));
			columns.put("visits",
					Integer.toString(reviews.get(i).getLatestVisits()));
			rows.add(columns);
		}

		SimpleAdapter adapter = new SimpleAdapter(this, rows,
				R.layout.list_view_columns, new String[] { "game", "reviewer",
						"visits" }, new int[] { R.id.columnGame,
						R.id.columnReviewer, R.id.columnVisits });
		listView.setAdapter(adapter);
	}

	// ********* AddUpdateReviewsTask *********

	private class AddUpdateReviewsTask extends
			AsyncTask<ReviewFunction, Integer, Integer[]> {
		private int mostVisits;
		private String mostVisitsGame;

		public AddUpdateReviewsTask(ReviewFunction type) {
			switch (type) {
			case ADD_CUSTOM:
				progressDialog.setMessage("Adding");
				progressDialog.setMax(addCustomList.size());
				break;
			case ADD_LATEST:
				progressDialog.setMessage("Adding");
				progressDialog.setMax(MAX_REVIEWS);
				break;
			case UPDATE:
				progressDialog.setMax(database.getNumberOfReviews());
				progressDialog.setMessage("Updating");
				break;
			}
		}

		@Override
		protected void onPreExecute() {
			progressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected Integer[] doInBackground(ReviewFunction... type) {
			int reviewsAdded = 0;
			int reviewsUpdated = 0;

			switch (type[0]) {
			case ADD_CUSTOM:
				reviewsAdded = addCustomReviews();
				break;
			case ADD_LATEST:
				reviewsAdded = addLatestReviews();
				break;
			case UPDATE:
				mostVisits = 0;
				mostVisitsGame = "";

				reviewsUpdated = updateAllReviews();
				break;
			}

			return new Integer[] { reviewsAdded, reviewsUpdated };
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
			super.onProgressUpdate(progress);
		}

		// Displays the number of added/updated reviews, the most
		// visited review as well as updates the list view and
		// saves the database to file if any changes.
		protected void onPostExecute(Integer[] result) {
			progressDialog.dismiss();

			Toast.makeText(getApplicationContext(),
					"Added: " + result[0] + " | Updated: " + result[1],
					Toast.LENGTH_LONG).show();

			// If reviews updated
			if (result[1] > 0) {
				// Set last updated to today
				database.setLastUpdated(new SimpleDateFormat("yyyy-MM-dd")
						.format(new Date()));

				// Display most visited review dialog
				if (mostVisits > 0) {
					mostVisitedDialog.setMessage(mostVisitsGame
							+ " (" + mostVisits + ")");
					mostVisitedDialog.show();
				}
			}

			// If reviews added or updated
			if (result[0] > 0 || result[1] > 0) {
				updateList();
				saveReviews();
			}
		}

		// ********* ADD/UPDATE METHODS *********

		// Adds a review to the database.
		// Does not add if it already exists.
		public boolean addReview(int id) {
			if (database.containsReview(id))
				return false;

			String webContents = Utils.getPageSource(DEFAULT_URL + id);
			String expr;

			if (id >= 2140)
				expr = "h1>([^<]+)";
			else if (id >= 1953)
				expr = ">Recension: ([^<]+)";
			else
				expr = ">Spelrecension: ([^<]+)";

			String name = Utils.matchExpr(expr, webContents);
			expr = "(\\d+) bes";
			int visits = Integer.parseInt(Utils.matchExpr(expr, webContents));
			expr = "id=\"user_([^\"]+)";
			String author = Utils.matchExpr(expr, webContents);

			database.addReview(id, name, author, new Date(), visits);

			if (database.getNumberOfReviews() > MAX_REVIEWS)
				database.removeOldestReview();

			return true;
		}

		// Add manually chosen reviews
		private int addCustomReviews() {
			int reviewsAdded = 0;

			for (int i = 0; i <= addCustomList.size() - 1; i++) {
				if (addReview(addCustomList.get(i)))
					reviewsAdded++;
				publishProgress(i);
			}

			return reviewsAdded;
		}

		// Searches for new reviews and adds them.
		// Returns the number of reviews added.
		private int addLatestReviews() {
			String webContents = Utils.getPageSource(DEFAULT_URL);
			String expr = "pagenavarea\">(\\d+)";

			// Total pages with reviews
			int totalPages = Integer.parseInt(Utils
					.matchExpr(expr, webContents));

			expr = "spelrec/(\\d+)\" class=\"litenrubrik\">";
			Pattern pattern = Pattern.compile(expr);

			int reviewsAdded = 0;
			int progress = 0;

			// Search through all pages
			for (int i = 1; i <= totalPages; i++) {
				webContents = Utils.getPageSource(DEFAULT_URL + "sida/" + i);

				Matcher m = pattern.matcher(webContents);

				// Add reviews until max added reviews have been reached
				while (m.find()) {
					if (database.getNumberOfReviews() >= MAX_REVIEWS) {
						publishProgress(database.getNumberOfReviews());

						return reviewsAdded;
					} else if (addReview(Integer.parseInt((m.group(1)))))
						reviewsAdded++;

					publishProgress(progress++);
				}
			}

			return reviewsAdded;
		}

		// Updates all reviews in the database.
		// Returns the number of reviews updated.
		private int updateAllReviews() {
			int reviewsUpdated = 0;
			int progress = 0;

			for (Review review : database.getAllReviews()) {
				if (updateReview(review.getId())) {
					reviewsUpdated++;

					// Get the number of new visits since last update
					int newVisits = review.getNewVisits();

					if (newVisits > mostVisits) {
						mostVisits = newVisits;
						mostVisitsGame = review.getName();
					}
				}

				publishProgress(progress++);
			}

			return reviewsUpdated;
		}

		// Updates the visits information of a review.
		// Does not update if already updated within the same day.
		@SuppressWarnings("deprecation")
		private boolean updateReview(int id) {
			Date lastUpdated = database.getReview(id).getLastUpdated();
			Date today = new Date();

			if (lastUpdated.getDay() == today.getDay()
					&& lastUpdated.getMonth() == today.getMonth()
					&& lastUpdated.getYear() == today.getYear())
				return false;

			String webContents = Utils.getPageSource(DEFAULT_URL + id);
			String expr = "(\\d+) bes";
			int visits = Integer.parseInt(Utils.matchExpr(expr, webContents));

			database.updateVisits(id, today, visits);

			return true;
		}
	}
}
