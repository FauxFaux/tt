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
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

public class TT {

	static class Track {
		private final int id;
		private final String name;

		public Track(String name, int id) {
			this.name = name;
			this.id = id;
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		final Set<String> trackedUsers = ImmutableSet.of(
				"[UWCS] Faux",
				"[UWCS] Silver",
				"[UWCS] ajmiles"
				);

		final Map<String, AtomicInteger> results = new MapMaker().makeComputingMap(new Function<String, AtomicInteger>() {
			@Override
			public AtomicInteger apply(String input) {
				return new AtomicInteger();
			}
		});

		final List<Track> tracks = ImmutableList.of(
				new Track("warming up", 1),
				new Track("first shortcuts", 5),
				new Track("crack down", 0),
				new Track("walljump", 3),
				new Track("reverse parking", 423),
				new Track("death slope", 4),
				new Track("overloaded truck", 6),
				new Track("reversing fast", 7),
				new Track("sky fly", 8),
				new Track("alpinism", 9),
				new Track("overpass", 10)
				);

		final Socket s = new Socket(InetAddress.getByName("truck.gravitysensation.com"), 23000);
		final PrintStream failed = new PrintStream(new FileOutputStream("wrong.rej", true));

		try {
			final OutputStream os = s.getOutputStream();
			final InputStream is = s.getInputStream();
			setup(os, is);

			write(os, SortOrder.NEWEST.forPage(0));
			char[] c2 = readPacket(is, 3);
			format(c2, c2.length);
			char[] c = decode(c2);
			format(c, c.length);
			write(os, new byte[] { 6, 0, 10, 1, 11, 0, 0, 0,
					               6, 0, 10, 1, 12, 0, 0, 0, });
			consumeAvailable(is);

			if (true)
				return;

			for (Track t : tracks) {
				final StringBuilder sb = new StringBuilder(t.name).append(": ");
				int ourpos = 0;
				for (Score q : getFilteredScores(is, os, failed, trackedUsers, t.id)) {
					if (ourpos < trackedUsers.size())
						results.get(q.name).addAndGet(trackedUsers.size() - ourpos);
					++ourpos;
					format(sb, q, ourpos);
				}

				System.out.println(sb);
				Thread.sleep(1000);
			}
			System.out.println(results);
		} finally {
			s.close();
			failed.close();
		}
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
		format(b, length);
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
		request(os, track, truck);
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

	private static void request(final OutputStream os, int track, int truck) throws IOException {
		byte[] req = new byte[] { 0x09, 0, 0x19, (byte) (track & 0xff), (byte) ((track >> 8) & 0xff),
				(byte) ((track >> 16) & 0xff), (byte) ((track >> 24) & 0xff), (byte) truck, 0, 0, 0 };
		write(os, req);
	}

	static enum SortOrder {
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

//		final byte[] requestStartScreen = new byte[] { 0x04, 00, 0x09, 00, 02, 00, };
////                                   First page of new:  04  00    09  00  02  00
////                                  Second page of new:  04  00    09  00  02  01
////                                  First page of best:  04  00    09  00  01  00
//		// best rated: 3
//		// alphabetic: 5
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
		format(first, len);
	}

	private static void format(byte[] first, int len) {
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
				System.out.printf("%c", chr >= 32 && chr <= 128 ?
						(char)chr : chr == 0 ? '_' : '.');
			} else
				System.out.print(" ");
		}
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

	static class Score {
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

	static List<Score> parse(char[] b) {
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

	static char[] decode(char[] in) {
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