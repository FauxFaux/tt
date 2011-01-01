package com.goeswhere.tt;

import static com.goeswhere.tt.TT.readName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class ParseTest {
	private static interface RunWithStream {
		void run(InputStream is) throws IOException;
	}

	void runWithStreams(String resource, RunWithStream rws) throws IOException {
		rws.run(getResourceAsStream(resource));
		rws.run(new BufferedInputStream(getResourceAsStream(resource)));
		rws.run(new RageInputStream(getResourceAsStream(resource)));
	}

	private InputStream getResourceAsStream(String resource) {
		final InputStream stream = ParseTest.class.getResourceAsStream(resource);
		assertNotNull("test resource must exist", stream);
		return stream;
	}

	@Test
	public void testReadName() throws IOException {
		runWithStreams("details533.tst", new RunWithStream() {
			@Override
			public void run(InputStream is) throws IOException {
				assertEquals("Kreta Droga", readName(is));
				assertEquals(-1, is.read());
			}
		});
	}
}
