package com.goeswhere.tt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

public class TT {

	private static class ListElement {
		private final int no;
		private final boolean costs;

		public ListElement(int no, boolean costs) {
			this.no = no;
			this.costs = costs;
		}

		@Override
		public String toString() {
			return "ListElement [no=" + no + (costs ? ", NON FREE" : "") + "]";
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final DAO dao = TTLevelCache.dao();
		final Socket s = new Socket(InetAddress.getByName("truck.gravitysensation.com"), 23000);
		final PrintStream failed = new PrintStream(new FileOutputStream("wrong.rej", true));

		try {
			final OutputStream os = s.getOutputStream();
			final InputStream is = s.getInputStream();
			setup(os, is);

			final int pages = 1;
			final List<Integer> tracks = Lists.newArrayListWithExpectedSize(pages * 25);
			write(os, SortOrder.DEFAULT.forPage(0));
			readFreeTracks(is, tracks);
			for (int i = 0; i < pages; ++i) {
				Thread.sleep(100);
				write(os, SortOrder.NEWEST.forPage(i));
				readFreeTracks(is, tracks);
				System.out.printf("Stage 1 / 2: %3d%% done\n", (int)(100 * (i+1) / (float)pages));
			}

			for (int i = 0; i < tracks.size(); ++i) {
				Thread.sleep(1000);
				int id = tracks.get(i);
				final List<Score> scores = getScores(failed, os, is, id, 0);
				final List<Object[]> times = Lists.newArrayListWithCapacity(scores.size());
				int pos = 0;
				for (Score sc : scores) {
					times.add(new Object[] { id, ++pos, sc.name, sc.time, sc.hard } );
				}

				dao.saveTrackTimes(id, times);
				System.out.printf("Stage 2 / 2: %3d%% done\n", (int)(100 * (i+1) / (float)tracks.size()));
			}

		} finally {
			s.close();
			failed.close();
		}
	}

	private static void readFreeTracks(final InputStream is, final List<Integer> tracks) throws IOException {
		for (ListElement l : parseListPacket(decode(readPacket(is, 3))))
			if (!l.costs)
				tracks.add(l.no);
	}

	private static List<ListElement> parseListPacket(char[] c) {
		int ptr = 0x18;
		List<ListElement> l = Lists.newArrayListWithCapacity(25);
		while (ptr < c.length - 1) {
			int no = readTwo(c, ptr);
			boolean costs = c[0x1a0 + ptr] != 0;
			l.add(new ListElement(no, costs));
			ptr += 0x1b4;
		}
		return l;
	}

	private static void readNamesPacket(char[] q, Map<Integer, String> names) {
		int ptr = 0;
		while (ptr != q.length) {
			int length = readTwo(q, ptr);
			ptr += 2;
			if (0 == length)
				break;
			names.put(readTwo(q, ptr+8), cstring(q, ptr+0x28, 16));
			ptr += length;
		}
	}

	private static int readTwo(char[] q, int ptr) {
		return (q[ptr + 1] * 0x100) + q[ptr];
	}

	private static void format(char[] c, int length) {
		byte[] b = new byte[length];
		for (int i = 0; i < length; ++i)
			b[i] = (byte) c[i];
		debugOutput(b, length);
	}

	private static List<Score> getFilteredScores(final InputStream is, final OutputStream os, final PrintStream failed,
			final Set<String> trackedUsers, int track) throws IOException {
		final List<Score> s = getScores(failed, os, is, track, 0);
		final List<Score> filtered = Lists.newArrayListWithCapacity(trackedUsers.size());
		for (Score m : s) {
			if (trackedUsers.contains(m.name)) {
				filtered.add(m);
			}
		}
		return filtered;
	}

	private static void format(final StringBuilder sb, Score m, int ourpos) {
		sb.append("  ").append(ourpos).append(") ")
		.append(m.name).append(", ")
		.append(new DecimalFormat("#.00").format(m.time)).append("s")
		.append(" (").append(m.pos).append(postfix(m.pos)).append(").");
	}

	private static String postfix(final int n)
	{
		if (n % 100 >= 11 && n % 100 <= 13)
			return "th";

		switch (n % 10)
		{
			case 1: return "st";
			case 2: return "nd";
			case 3: return "rd";
		}
		return "th";
	}

	/** @param track As displayed; apart from home screen.
	 * @param truck 0 for free truck, upwards. */
	private static List<Score> getScores(final PrintStream failed, final OutputStream os,
			final InputStream is, int track, int truck) throws IOException {
		requestScores(os, track, truck);
		char[] nc = readPacket(is, 63);

		try {
			return parse(decode(nc));
		} catch (Exception e) {
			e.printStackTrace(failed);
			for (char c : nc)
				failed.print((int)c + ", ");
			failed.flush();
			throw new RuntimeException(e);
		}
	}

	private static void requestScores(final OutputStream os, int track, int truck) throws IOException {
		byte[] req = new byte[] { 0x09, 0, 0x19, (byte) (track & 0xff), (byte) ((track >> 8) & 0xff),
				(byte) ((track >> 16) & 0xff), (byte) ((track >> 24) & 0xff), (byte) truck, 0, 0, 0 };
		write(os, req);
	}

	private static enum SortOrder {
		DEFAULT(0),
		NEWEST(2),
		USERS(1),
		RATED(3),
		ALPHABETIC(5);

		private final int number;

		SortOrder(int number) {
			this.number = number;
		}

		byte[] forPage(int page) {
			return new byte[] { 0x04, 00, 0x09, 00, (byte) number, (byte) page };
		}
	}

	private static void setup(final OutputStream os, final InputStream is) throws IOException {

		final byte[] initial = new byte[] {
				0x0d, 0x00, 0x04, 0x7e, (byte) 0xc1, 0x36, 0x65, 0x10,
				0x00, 0x00, 0x00, (byte) 0xfa, 0x0d, 0x08, 0x0a };
		final byte[] login = new byte[] { 0x09, 00, 0x1a, 00, 00, 00, 00, 00, 00, 00, 00, };
		final byte[] privacies = new byte[] { 0x54, 00, 0x05, 0x65, 00, 00, 00, 00, 00,
				00,
				00, // username
				00, 00, 00, 00, 00, 00, 00, 00, 65, 84, 73, 32, 82, 97, 100, 101, 111, 110,
				32,
				72, // system config
				68, 32, 53, 56, 48, 48, 32, 83, 101, 114, 105, 101, 115, 32, 32, 59, 97, 116, 105, 99, 102, 120, 51,
				50, 46, 100, 108, 108, 59, 99, 111, 114, 101, 115, 58, 52, 59, 49, 51, 50, 56, 120, 56, 52, 48, 59,
				100, 101, 116, 97, 105, 108, 58, 50, 59, };

		write(os, initial);
		consumeAvailable(is);
		write(os, login);
		// consumeAvailable(is); // sometimes?
		write(os, privacies);
	}

	private static void write(final OutputStream os, final byte[] bytes) throws IOException {
		os.write(bytes);
		os.flush();
	}

	private static void consumeAvailable(final InputStream is) throws IOException {
		byte[] first = new byte[90000];
		int len = is.read(first);
		debugOutput(first, len);
	}

	private static void debugOutput(byte[] first, int len) {
		final int WIDTH = 16;
		System.out.print("       ");
		for (int i = 0; i < WIDTH; ++i) {
			addBreak(WIDTH, i);
			System.out.printf("%2x ", i);
		}
		System.out.print("  ");
		for (int i = 0; i < WIDTH; ++i) {
			addBreak(WIDTH, i);
			System.out.printf("%x", i);
		}
		System.out.print("  ");
		for (int i = 0; i < WIDTH; ++i)
			System.out.printf("%x", i);

		System.out.println();
		final StringBuilder array = new StringBuilder(len * 10).append("char[] c = new char[] { ");
		for (int l = 0; l <= len / WIDTH; ++l) {
			System.out.printf("%5x  ", l*WIDTH);
			for (int i = 0; i < WIDTH; ++i) {
				addBreak(WIDTH, i);
				int ind = l*WIDTH+i;
				if (ind < len) {
					System.out.printf("%2x ", first[ind]);
					array.append("(char)0x")
						.append(String.format("%02x", first[ind]))
						.append(", ");
				}
				else
					System.out.print("   ");
			}

			System.out.print("  ");
			printAscii(first, len, WIDTH, l, true);
			System.out.print("  ");
			printAscii(first, len, WIDTH, l, false);
			System.out.println();
			array.append("\n                        ");
		}
		array.append("};");
		System.out.println(array);
	}

	private static void printAscii(byte[] first, int len, final int WIDTH, int l, boolean doBreak) {
		for (int i = 0; i < WIDTH; ++i) {
			if (doBreak)
				addBreak(WIDTH, i);
			int ind = l*WIDTH+i;
			if (ind < len) {
				byte chr = first[ind];
				System.out.printf("%c", isPrint((char)chr) ?
						(char)chr : chr == 0 ? '_' : '.');
			} else
				System.out.print(" ");
		}
	}

	private static boolean isPrint(char c) {
		return c >= 32 && c <= 128;
	}

	private static void addBreak(final int WIDTH, int i) {
		if (i % (WIDTH/4) == 0)
			System.out.print(" ");
	}

	private static char[] readPacket(final InputStream is, int offset) throws IOException {
		byte[] n = new byte[900000];
		final int found = is.read(n);
		char[] nc = new char[found];
		for (int i = offset; i < found; ++i)
			nc[i - offset] = (char) (n[i] < 0 ? 256 + n[i] : n[i]);
		return nc;
	}

	private static class Score {
        final String name;
        private final double time;
        private final boolean hard;
        final boolean online;
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
            return "Score [name=" + name + ", time=" + time + ","
            + (hard ? " (hard)" : "")
            + (online ? " (online)" : "") + "]";
        }
	}

	private static List<Score> parse(char[] b) {
		final List<Score> ret = new ArrayList<Score>(b.length / 32);
		int ptr = 0;
		int pos = 0;
		while (ptr < b.length - 30) {
			final String name = cstring(b, ptr, 16);
			ptr += 16;
			final double time = dword(b, ptr) / 120.;
			ptr += 4;
			final boolean hard = b[ptr + 6] != 0;
			final boolean online = b[ptr + 7] != 0;
			ptr += 12;
			ret.add(new Score(++pos, name, time, hard, online));
		}
		return ret;
	}

	private static long dword(char[] b, int ptr) {
		return (b[ptr] + (b[ptr + 1] << 8));
	}

	private static String cstring(char[] b, int ptr, int i) {
		final String s = new String(b, ptr, i);
		final int ind = s.indexOf(0);
		if (-1 == ind)
			return s;
		return s.substring(0, ind);
	}

	private static char[] decode(char[] in) {
		char[] out = new char[in.length * 20];

		int outptr = 0;
		int inptr = 0;
		char flag = in[inptr++];
		flag &= 0x1f;
		char esp10 = flag;

		while (true) {
			if (flag >= 0x20) {

				char highflag = (char) (flag >> 5);
				int lowflag = -((flag & 0x1f) << 8);

				--highflag;

				if (6 == highflag) {
					highflag = (char) (in[inptr++] + 6);
				}

				lowflag -= in[inptr++];

				int sourceptr = outptr + lowflag;

				if (inptr < in.length)
					esp10 = flag = in[inptr++];
				else
					throw new AssertionError();

				if (outptr == sourceptr) {

					char thing = out[outptr - 1];

					out[outptr++] = thing;
					out[outptr++] = thing;
					out[outptr++] = thing;

					if (highflag != 0) {

						flag = esp10;

						for (int i = 0; i < highflag; ++i)
							out[outptr++] = thing;
					}
				} else {

					--sourceptr;

					out[outptr++] = out[sourceptr++];
					out[outptr++] = out[sourceptr++];
					out[outptr++] = out[sourceptr++];

					if ((highflag & 1) == 1) {

						out[outptr++] = out[sourceptr++];

						--highflag;
					}

					int tooutptr = outptr;
					outptr += highflag;
					highflag >>= 1;

					while (highflag != 0) {
						out[tooutptr++] = out[sourceptr++];
						out[tooutptr++] = out[sourceptr++];

						--highflag;
					}
				}
			} else {
				++flag;
				int inend = inptr + flag;
				if (inend >= in.length)
					return Arrays.copyOfRange(out, 0, outptr);

				for (int i = 0; i < flag; ++i)
					out[outptr++] = in[inptr++];

				flag = in[inptr++];
				esp10 = flag;
			}
		}

	}
}