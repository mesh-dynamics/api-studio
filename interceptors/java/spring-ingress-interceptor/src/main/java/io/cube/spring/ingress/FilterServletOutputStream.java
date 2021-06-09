package io.cube.spring.ingress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;


/**
 * Reference: https://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging
 */
public class FilterServletOutputStream extends ServletOutputStream {

	private WriteListener writeListener = null;
	private OutputStream originalStream = null;
	private ByteArrayOutputStream baos = null;

	public FilterServletOutputStream(OutputStream originalStream) {
		this.originalStream = originalStream;
		this.baos = new ByteArrayOutputStream();
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		this.writeListener = writeListener;
	}

	@Override
	public void write(int b) throws IOException {
		originalStream.write(b);
		baos.write(b);
		if (writeListener != null) {
			writeListener.notify();
		}
	}

	public byte[] getBodyAsByteArray() {
		return baos.toByteArray();
	}
}
