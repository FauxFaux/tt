import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class TT {
	public static void main(String[] args) throws IOException {

//		final byte[] initial = new byte[] { 0x0d, 00, 0x04, (byte) 0xd4, 0x2b, 0x68, 0x34, 0x0f, 00, 00, 00, 0x4a,
//				(byte) 0x99, 0x7d, 0x09 };
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

		int a = 423; // level number, as displayed
		int tn = 0; // truck number, 0 -> 5

		byte[] req = new byte[] { 0x09, 0, 0x19, (byte) (a & 0xff), (byte) ((a >> 8) & 0xff),
				(byte) ((a >> 16) & 0xff), (byte) ((a >> 24) & 0xff), (byte) tn, 0, 0, 0 };
		final Socket s = new Socket(InetAddress.getByName("truck.gravitysensation.com"), 23000);
		try {
			final OutputStream os = s.getOutputStream();
			final InputStream is = s.getInputStream();
			os.write(initial);
			os.flush();
			is.read(new byte[9000]);

			os.write(login);
			os.flush();
			os.write(privacies);
			os.flush();
			os.write(login2);
			os.flush();
			final byte[] first = new byte[90000];
			int q = is.read(first);


			os.write(req);
			os.flush();
			byte[] n = new byte[9000];
			final int found = is.read(n);
			char[] nc = new char[found];
			for (int i = 63; i < found; ++i)
				nc[i - 63] = (char) (n[i] < 0 ? 256 + n[i] : n[i]);

			for (TT3.Score m : TT3.parse(TT3.decode(nc)))
				System.out.println(m);
		} finally {
			s.close();
		}
	}
}