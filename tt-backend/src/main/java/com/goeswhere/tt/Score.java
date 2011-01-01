package com.goeswhere.tt;

class Score {
	final String name;
	final double time;
	final boolean hard;
	private final boolean online;
	private final int pos;

	public Score(int pos, String name, double time, boolean hard, boolean online) {
		this.pos = pos;
		this.name = name;
		this.time = time;
		this.hard = hard;
		this.online = online;
	}

	@Override
	public String toString() {
		return "Score [name=" + name + ", time=" + time + ", pos=" + pos + ","
		+ (hard ? " (hard)" : "")
		+ (online ? " (online)" : "") + "]";
	}
}