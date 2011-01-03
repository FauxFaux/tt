package com.goeswhere.tt;

import static com.goeswhere.tt.TT.charise;

import java.util.Set;

public class GuessDecrypt {

	static char[] guessEncrypted(byte[] in, final Set<String> nicks) {
		int firstlen = charise(in[0]);
		if (firstlen >= 4) {
			return hilarious(in, nicks);
		}
		throw new RuntimeException("Can't deal with only " + firstlen + " of padding");
	}

	/**
	 * The protection here is xoring with a 4-octet value.
	 *
	 * Recovering the key seems to be pretty challenging, so let's just guess it.
	 *
	 * We know that (at least) the first 5 octets of data (octets 1->6 of input; 0 is taken) are a nick.
	 *
	 * The 1st octet of data and the 5th octet are encrypted with the same octet of the key.
	 * -> generate key from 1st octet, apply to 5th octet, check it matches the nick.
	 *
	 * If it does, generate the full key from the nick, and apply it to the entire message.
	 *
	 * If this decompresses, then, victory. \o/  Otherwise, just try the next another nick.
	 */
	private static char[] hilarious(byte[] in, final Set<String> nicks) {
		for (String nick : nicks)
			if (nick.length() >= 4)
				if ((in[1] ^ nick.charAt(0) ^ in[5]) == (nick.length() >= 5 ? nick.charAt(4) : 0)) {
					byte[] b = new byte[4];
					for (int i = 0; i < 4; ++i)
						b[i] = (byte) (in[i + 1] ^ nick.charAt(i));
					char[] out = new char[in.length];
					out[0] = (char) in[0];
					for (int i = 1; i < out.length; ++i)
						out[i] = TT.charise((byte) (in[i] ^ b[(i - 1) % 4]));
					try {
						return TT.decode(out);
					} catch (RuntimeException e) {
						// whatever
					}
				}
		throw new RuntimeException("No keys found. /o\\");
	}
}
