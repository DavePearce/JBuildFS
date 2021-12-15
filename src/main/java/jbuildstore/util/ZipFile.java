// Copyright 2021 David James Pearce
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package jbuildstore.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jbuildstore.core.Content;

/**
 * A shim for handling ZipFiles in a uniform fashion within the Whiley File
 * System (WyFS).
 *
 * @author David J. Pearce
 *
 */
public class ZipFile<K extends Content.Key<?>> implements Content, Content.Source<K> {

	public static <K extends Content.Key<?>> Content.Type<ZipFile<K>> ContentType(
			jbuildstore.core.Key.Mapping<K, String> encdec) {
		return new Content.Type<>() {
			@Override
			public ZipFile<K> read(InputStream input) throws IOException {
				return new ZipFile<>(this, encdec, input);
			}

			@Override
			public void write(OutputStream output, ZipFile<K> zf) throws IOException {
				ZipOutputStream zout = new ZipOutputStream(output);
				for (int i = 0; i != zf.size(); ++i) {
					ZipFile.Entry<K> e = zf.get(i);
					// Create filename
					String filename = encdec.encode(e.key);
					zout.putNextEntry(new ZipEntry(filename));
					zout.write(e.bytes);
					zout.closeEntry();
				}
				zout.finish();
			}

			@Override
			public String suffix() {
				return "zip";
			}
		};
	};

	/**
	 * The actual content type used for this ZipFile.
	 */
	private final Content.Type<?> contentType;

	/**
	 * Contains the list of entries in the zip file.
	 */
	private final List<Entry<K>> entries;

	/**
	 * Construct an empty ZipFile
	 */
	public ZipFile(Content.Type<?> contentType) {
		this.contentType = contentType;
		this.entries = new ArrayList<>();
	}

	/**
	 * Construct a ZipFile from a given input stream representing a zip file.
	 *
	 * @param input
	 */
	public ZipFile(Content.Type<?> contentType, jbuildstore.core.Key.Mapping<K, String> encdec,
			InputStream input) throws IOException {
		this.contentType = contentType;
		this.entries = new ArrayList<>();
		// Read all entries from the input stream
		ZipInputStream zin = new ZipInputStream(input);
		ZipEntry e;
		while ((e = zin.getNextEntry()) != null) {
			byte[] contents = readEntryContents(zin);
			// Decode filename into path and content type.
			K key = encdec.decode(e.getName());
			entries.add(new Entry<>(key, contents));
			zin.closeEntry();
		}
		zin.close();
	}

	public int size() {
		return entries.size();
	}

	@Override
	public Content.Type<?> contentType() {
		return contentType;
	}

	public void add(K key, byte[] bytes) {
		this.entries.add(new Entry<>(key, bytes));
	}

	/**
	 * Get the ith entry in this ZipFile.
	 *
	 * @param i
	 * @return
	 */
	public Entry<K> get(int i) {
		return entries.get(i);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Content, S extends Key<T>> T get(S p) {
		for (int i = 0; i != entries.size(); ++i) {
			Entry<K> ith = entries.get(i);
			if (ith.key.equals(p)) {
				return (T) ith.get();
			}
		}
		// Didn't find anything.
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Content, S extends Key<T>> List<T> getAll(Function<K, S> query) {
		ArrayList<T> rs = new ArrayList<>();
		for (int i = 0; i != entries.size(); ++i) {
			Entry<K> ith = entries.get(i);
			S k = query.apply(ith.key);
			if (k != null) {
				rs.add((T) ith.get());
			}
		}
		return rs;
	}

	@Override
	public <T extends Content, S extends Key<T>> List<S> match(Function<K, S> query) {
		throw new UnsupportedOperationException();
	}

	private byte[] readEntryContents(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		// Read bytes in max 1024 chunks
		byte[] data = new byte[1024];
		// Read all bytes from the input stream
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		// Done
		buffer.flush();
		return buffer.toByteArray();
	}

	private final static class Entry<K extends Content.Key<?>> {
		public final K key;
		public final byte[] bytes;
		public Content value;

		public Entry(K key, byte[] bytes) {
			this.key = key;
			this.bytes = bytes;
		}

		public Content get() {
			try {
				if (value == null) {
					value = key.contentType().read(getInputStream());
				}
				return value;
			} catch (IOException e) {
				return null;
			}
		}

		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}
	}
}
