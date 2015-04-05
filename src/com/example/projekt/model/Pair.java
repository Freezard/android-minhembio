package com.example.projekt.model;

import java.io.Serializable;

public class Pair<F, S> implements Serializable {
	private static final long serialVersionUID = 0L;
	private final F first;
	private final S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}
}