package com.example.projekt.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Environment;
import android.util.Log;

/**
 * Utils.java
 * 
 * Helper class with common operations.
 */
public class Utils {
	// Matches an expression with regex and returns the match if successful
	public static String matchExpr(String expr, String text) {
		Pattern pattern = Pattern.compile(expr);
		Matcher m = pattern.matcher(text);

		if (m.find())
			return m.group(1);
		else {
			Log.e("", "No match! " + expr);
			return "";
		}
	}

	// Downloads the page source of a web site.
	// Returns the string.
	public static String getPageSource(String webSiteUrl) {
		URL url = null;

		try {
			url = new URL(webSiteUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		BufferedReader reader;
		String s = null;
		StringBuilder sb = new StringBuilder();

		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream()));

			while ((s = reader.readLine()) != null)
				sb.append(s);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	// Creates all directories needed to the specified location
	public static void makeDirs(String location) {
		File dir = new File(location);
		dir.mkdirs();
	}

	// Deletes the directory including all the contained files
	public static void deleteDir(File dir) {
		File[] files = dir.listFiles();

		if (files != null)
			for (File f : files)
				if (f.isDirectory())
					deleteDir(f);
				else
					f.delete();
		dir.delete();
	}

	// Checks if the specified location is a directory
	public static boolean isDirectory(String location) {
		File dir = new File(location);

		return dir.isDirectory();
	}

	// Checks if the specified location exists
	public static boolean fileExists(String location) {
		File file = new File(location);

		return file.exists();
	}

	// Checks if external storage is available to read and write
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();

		return Environment.MEDIA_MOUNTED.equals(state);
	}

	// Checks if external storage is available to read
	public static boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();

		return Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	}
}