package io.md.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MeshDGsonProvider {

	private static Gson singleInstance = (new GsonBuilder()).create();

	public static Gson getInstance() {
		return singleInstance;
	}

}
