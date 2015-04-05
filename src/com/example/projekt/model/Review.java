package com.example.projekt.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Review.java
 * 
 * A review with an id, game name, reviewer, and a list of visits at a certain date.
 */
public class Review implements Serializable {
	private static final long serialVersionUID = 0L;
	private int id;
	private String name;
	private String reviewer;
	private List<Pair<Date, Integer>> visits;

	public Review(int id, String name, String reviewer, Date date, int visits) {
		this.id = id;
		this.name = name;
		this.reviewer = reviewer;
		this.visits = new ArrayList<Pair<Date, Integer>>();
		this.visits.add(new Pair<Date, Integer>(date, visits));
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getReviewer() {
		return reviewer;
	}

	public int getLatestVisits() {
		return visits.get(visits.size() - 1).getSecond();
	}

	public Date getLastUpdated() {
		return visits.get(visits.size() - 1).getFirst();
	}

	public int getNewVisits() {
		return visits.get(visits.size() - 1).getSecond()
				- visits.get(visits.size() - 2).getSecond();
	}

	public void addVisits(Date date, int visits) {
		this.visits.add(new Pair<Date, Integer>(date, visits));
	}

	public List<Pair<Date, Integer>> getVisits() {
		return visits;
	}
}
