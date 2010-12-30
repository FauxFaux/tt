package com.goeswhere.tt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class TTLevelCache {
	public static void main(String[] args) throws IOException {
		final FileInputStream fis = new FileInputStream(args[0]);
		try {
			while (true) {
				if (discardHeader(fis))
					break;
				int len = readTwo(fis);
				fis.skip(9);
				int num = readTwo(fis);
				fis.skip(30);
				String name = cstring(fis);

				System.out.println(num + ": " + name);
				fis.skip(len - 39 - 16);
			}
		} finally {
			fis.close();
		}

	}

	private static String cstring(InputStream fis) throws IOException {
		byte by[] = new byte[16];
		assertEqual(16, fis.read(by));
		for (int i = 0; i < by.length; ++i)
			if (0 == by[i])
				return new String(by, 0, i);
		return new String(by);
	}

	private static int readTwo(InputStream fis) throws IOException {
		return fis.read() + 0x100 * fis.read();
	}

	/** @return eof */
	private static boolean discardHeader(InputStream fis) throws IOException {
		byte[] by = new byte[4];
		int readed = fis.read(by);
		if (readed == -1)
			return true;
		assertEqual(4, readed);
		assertEqual(0x10, by[0]);
		assertEqual(0, by[1]);
		assertEqual(0, by[2]);
		assertEqual(0, by[3]);
		return false;
	}

	private static void assertEqual(int expected, int actual) {
		if (expected != actual)
			throw new AssertionError("got " + actual + ", expecting " + expected);

	}
}
