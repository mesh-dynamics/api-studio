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

package io.cube.jaxrs.egress;

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
