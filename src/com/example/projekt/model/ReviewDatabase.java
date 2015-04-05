package com.example.projekt.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.TreeMap;

/**
 *  ReviewDatabase.java
 *  
 *  A database containing reviews.
 */
public class ReviewDatabase implements Serializable {
	private static final long serialVersionUID = 0L;
	private TreeMap<Integer, Review> reviews;
	private String lastUpdated;

	public ReviewDatabase() {
		reviews = new TreeMap<Integer, Review>();
		lastUpdated = "Never";
	}

	public void addReview(int id, String game, String author, Date date,
			int visitors) {
		reviews.put(id, new Review(id, game, author, date, visitors));
	}

	public void updateVisits(Integer id, Date date, int visitors) {
		getReview(id).addVisits(date, visitors);
	}

	public boolean containsReview(Integer id) {
		if (reviews.containsKey(id))
			return true;
		else
			return false;
	}

	public Review getReview(Integer id) {
		return reviews.get(id);
	}

	public Collection<Review> getAllReviews() {
		return reviews.values();
	}

	public String getLastUpdated() {
		return lastUpdated;
	}
	
	public int getNumberOfReviews() {
		return reviews.size();
	}
	
	public void removeOldestReview() {
		reviews.remove(reviews.lastKey());
	}

	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}