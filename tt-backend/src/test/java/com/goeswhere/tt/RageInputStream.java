package com.goeswhere.tt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/** Attempt to make consumers angry by not doing what they say, within the spec. */
public class RageInputStream extends InputStream {
	private final Random rand = new Random();
	private final InputStream un;

	public RageInputStream(InputStream underlying) {
		this.un = underlying;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return un.read(b, off, fuzzDown(Math.min(un.available() + 1, len)));
	}

	@Override
	public long skip(long n) throws IOException {
		return un.skip(fuzzDown(n));
	}

	@Override
	public int available() throws IOException {
		return fuzzDown(un.available());
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new RuntimeException("mark not supported");
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new IOException("mark not supported");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		return un.read();
	}

	private long fuzzDown(long n) {
		return (long) (rand.nextDouble() * (n + 1));
	}

	private int fuzzDown(int n) {
		return rand.nextInt(n + 1);
	}
}
