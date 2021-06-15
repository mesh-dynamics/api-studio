/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
