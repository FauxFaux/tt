import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TT {

	private static final int FIRST_LEVEL = 1;

	public static void main(String[] args) throws IOException, InterruptedException {

		final Set<String> trackedUsers = new HashSet<String>();
		trackedUsers.add("Faux");
		trackedUsers.add("Silver");
		trackedUsers.add("ajmiles");

		final Socket s = new Socket(InetAddress.getByName("truck.gravitysensation.com"), 23000);
		final PrintStream failed = new PrintStream(new FileOutputStream("wrong.rej"));
		final Map<Integer, String> pwner = new HashMap<Integer, String>();
		pwner.put(375, null);
		pwner.put(377, null);
		pwner.put(FIRST_LEVEL, null);

		try {
			final OutputStream os = s.getOutputStream();
			final InputStream is = s.getInputStream();
			setup(os, is);

			while (true) {
				if (isAnyoneAlive(os, is, failed, trackedUsers)) {
					for (Entry<Integer, String> q : pwner.entrySet()) {
						System.out.println(q.getKey());
						for (TT3.Score m : getScores(failed, os, is, q.getKey(), 0)) {
							if (trackedUsers.contains(m.name)) {
								if (null == q.getValue())
									q.setValue(m.name);
								else if (m.name.equals(q.getValue()))
									q.setValue(m.name + " overtook " + q.getValue() + " on " + q.getKey());
								break;
							}
						}
					}
					Thread.sleep(1000);
				}
				Thread.sleep(60000);
			}
		} finally {
			s.close();
			failed.close();
		}
	}

	private static boolean isAnyoneAlive(final OutputStream os, final InputStream is,
			final PrintStream failed,
			final Set<String> trackedUsers) throws IOException {
		for (TT3.Score m : getScores(failed, os, is, FIRST_LEVEL, 0))
			if (m.online && trackedUsers.contains(m.name))
				return true;
		return false;
	}

	/** @param track As displayed; apart from home screen.
	 * @param truck 0 for free truck, upwards. */
	private static List<TT3.Score> getScores(final PrintStream failed, final OutputStream os,
			final InputStream is, int track, int truck) throws IOException {
		request(os, track, truck);
		char[] nc = readPacket(is);

		List<TT3.Score> pared;
		try {
			pared = TT3.parse(TT3.decode(nc));
		} catch (Exception e) {
			e.printStackTrace(failed);
			for (char c : nc)
				failed.print((int)c + ", ");
			failed.flush();
			throw new RuntimeException(e);
		}
		return pared;
	}

	private static void request(final OutputStream os, int track, int truck) throws IOException {
		byte[] req = new byte[] { 0x09, 0, 0x19, (byte) (track & 0xff), (byte) ((track >> 8) & 0xff),
				(byte) ((track >> 16) & 0xff), (byte) ((track >> 24) & 0xff), (byte) truck, 0, 0, 0 };
		os.write(req);
		os.flush();
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
		final byte[] login2 = new byte[] { 0x04, 00, 0x09, 00, 00, 00, };

		os.write(initial);
		os.flush();
		is.read(new byte[90000]);

		os.write(login);
		os.flush();
		is.read(new byte[90000]);
		os.write(privacies);
		os.flush();
		os.write(login2);
		os.flush();
		is.read(new byte[90000]);
	}

	private static char[] readPacket(final InputStream is) throws IOException {
		byte[] n = new byte[900000];
		final int found = is.read(n);
		char[] nc = new char[found];
		for (int i = 63; i < found; ++i)
			nc[i - 63] = (char) (n[i] < 0 ? 256 + n[i] : n[i]);
		return nc;
	}
}