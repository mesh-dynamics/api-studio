package com.cube.interceptor.jaxrs.egress;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class ClientLoggingStream extends FilterOutputStream {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	public ClientLoggingStream(OutputStream entityStream) {
		super(entityStream);
	}

	public String getString(final Charset charset)
	{
		final byte[] entity = baos.toByteArray();
		return new String(entity, charset);
	}

	public byte[] getbytes() {
		return baos.toByteArray();
	}

	@Override
	public void write(int b) throws IOException {
		baos.write(b);
		out.write(b);
	}
}
