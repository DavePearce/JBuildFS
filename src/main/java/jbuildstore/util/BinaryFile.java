package jbuildstore.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import jbuildstore.core.Content;

public class BinaryFile implements Content {
	public static Content.Type<BinaryFile> ContentType(String suffix) {
		return new Content.Type<BinaryFile>() {

			@Override
			public BinaryFile read(InputStream input) throws IOException {
				// Read all bytes from input stream
				byte[] bytes = input.readAllBytes();
				// Construct BinaryFile wrapper
				return new BinaryFile(this,bytes);
			}

			@Override
			public void write(OutputStream output, BinaryFile value) throws IOException {
				// Extract bytes from binary file
				byte[] bytes = value.getBytes();
				// Write them to output stream
				output.write(bytes);
			}

			@Override
			public String suffix() {
				return suffix;
			}

			@Override
			public String toString() {
				return suffix;
			}
		};
	}

	private final Content.Type<BinaryFile> contentType;
	private final byte[] bytes;

	public BinaryFile(Content.Type<BinaryFile> contentType, byte[] bytes) {
		this.contentType = contentType;
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public Type<?> contentType() {
		return contentType;
	}

	@Override
	public String toString() {
		return Arrays.toString(bytes);
	}
}
