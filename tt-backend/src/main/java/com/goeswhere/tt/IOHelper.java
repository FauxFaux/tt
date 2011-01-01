package com.goeswhere.tt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

abstract class IOHelper {
	static IOHelper loggingToStdErr(InputStream is) {
		return new IOHelper(is) {
			@Override
			void warn(String msg) {
				System.err.println(msg);
			}
		};
	}

	private final InputStream is;

	public IOHelper(InputStream is) {
		this.is = is;
	}

	abstract void warn(String msg);

	void assertArraysAreEqual(String situation, byte[] expected, byte[] actual) {
		if (!Arrays.equals(expected, actual))
			warn(situation + ": Expected:\n" + debugMessage(expected) + ", got:\n" + debugMessage(actual));
	}

	String debugMessage(byte[] b) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(b.length * 20);
		final PrintStream ps = new PrintStream(baos);
		TT.debugOutput(b, b.length, ps);
		ps.flush();
		ps.close();
		return new String(baos.toByteArray());
	}

	public void readOrWarn(String situation, byte[] expected) throws IOException {
		final byte[] actual = read(is, expected.length);
		assertArraysAreEqual(situation, expected, actual);
	}

	byte[] read(int len) throws IOException {
		return read(is, len);
	}

	/** reads exactly the requested number of bytes or throws */
	static byte[] read(InputStream is, final int cnt) throws IOException {
		final byte[] ret = new byte[cnt];
		int done = 0;
		while (done != ret.length) {
			final int read = is.read(ret, done, ret.length - done);
			if (-1 == read)
				throw new IOException("End of stream found trying to read "
						+ (ret.length - done) + "/" + ret.length + " bytes");
			done += read;
		}
		return ret;
	}

	public String readName() throws IOException {
		return TT.cstring(is, 16);
	}

	public int readTwo() throws IOException {
		return TT.readTwo(is);
	}

	public boolean checkEof() throws IOException {
		return -1 == is.read();
	}

	public void skip(int i) throws IOException {
		TT.skip(is, i);
	}
}
