package com.goeswhere.tt;

class Score {
	final String name;
	final double time;
	final boolean hard;
	private final boolean online;
	private final int pos;
	final long when;

	public Score(int pos, String name, double time, long when, boolean hard, boolean online) {
		this.pos = pos;
		this.name = name;
		this.time = time;
		this.when = when;
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