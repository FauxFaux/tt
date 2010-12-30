package com.goeswhere.tt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.google.common.collect.Lists;

public class TTLevelCache {

	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
		final DAO lc = dao();
		final Connection conn = DriverManager.getConnection("jdbc:sqlite:sample2.db");
		Statement stat = conn.createStatement();
		stat.execute("drop table track_names");
		stat.execute("create table if not exists track_names(track integer primary key, name varchar(16));");
		stat.execute("begin");

		final FileInputStream fis = new FileInputStream(args[0]);
		final List<Object[]> l = Lists.newArrayListWithExpectedSize(500);
		try {
			while (true) {
				if (discardHeader(fis))
					break;
				int len = readTwo(fis);
				fis.skip(9);
				int num = readTwo(fis);
				fis.skip(30);
				String name = cstring(fis);
				l.add(new Object[] { num, name });
				fis.skip(len - 39 - 16);
			}
			System.out.println(l.size());
			final long start = System.nanoTime();
			lc.saveTrackNumber(l);
			System.out.println((System.nanoTime() - start) / 1e9);
		} finally {
			fis.close();
		}
	}

	static DAO dao() throws SQLException, ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		return new DAO(DriverManager.getConnection("jdbc:sqlite:sample.db"));
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
