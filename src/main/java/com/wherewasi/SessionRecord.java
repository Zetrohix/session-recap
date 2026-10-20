package com.wherewasi;

import java.util.LinkedHashMap;
import java.util.Map;

// everything remembered about one world or server
public class SessionRecord {
	public double x, y, z;
	public String dimension;
	public long lastSeen;

	public long sessionMillis;
	public double distanceWalked;
	public Map<String, Integer> mined = new LinkedHashMap<>();
	public Map<String, Integer> placed = new LinkedHashMap<>();

	public boolean hasDeath;
	public double deathX, deathY, deathZ;
	public String deathDimension;
	public long deathTime;
	public String note;

	public boolean hasPosition() {
		return dimension != null && lastSeen > 0;
	}
}
