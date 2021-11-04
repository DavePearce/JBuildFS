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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import jbuildstore.core.Content;
import jbuildstore.core.Key;

/**
 * Provides an implementation of <code>Content.Store<K,V></code> which is backed
 * by a file system directory.
 *
 * @author David J. Pearce
 *
 */
public class DirectoryStore<K, V extends Content> implements Content.Store<K, V>, Iterable<V> {
	public final static FileFilter NULL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return true;
		}
	};
	private final File dir;
	private final FileFilter filter;
	private final Key.EncoderDecoder<K, V, String> encdec;
	private final ArrayList<Entry<V>> items;

	public DirectoryStore(Key.EncoderDecoder<K, V, String> encdec, File dir) throws IOException {
		this(encdec, dir, NULL_FILTER);
	}

	public DirectoryStore(Key.EncoderDecoder<K, V, String> encdec, File dir, FileFilter filter) throws IOException {
		this.encdec = encdec;
		this.dir = dir;
		this.filter = filter;
		this.items = initialise(dir, filter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends V> T get(Content.Type<T> kind, K key) {
		for (int i = 0; i != items.size(); ++i) {
			Entry<V> ith = items.get(i);
			Content.Type<?> ct = ith.getContentType();
			if (ith.getPath().equals(key) && ct == kind) {
				return (T) ith.get();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends V> List<T> getAll(Content.Filter<K, T> filter) {
		ArrayList<T> rs = new ArrayList<>();
		for (int i = 0; i != items.size(); ++i) {
			Entry<V> ith = items.get(i);
			if (filter.includes(ith.getContentType(), ith.getPath())) {
				rs.add((T) ith.get());
			}
		}
		return rs;
	}

	@Override
	public List<K> match(Content.Filter<K, ?> filter) {
		ArrayList<K> rs = new ArrayList<>();
		for (int i = 0; i != items.size(); ++i) {
			Entry<?> ith = items.get(i);
			if (filter.includes(ith.getContentType(), ith.getPath())) {
				rs.add(ith.getPath());
			}
		}
		return rs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> List<K> match(Content.Filter<K, S> filter, Predicate<S> f) {
		ArrayList<K> rs = new ArrayList<>();
		for (int i = 0; i != items.size(); ++i) {
			Entry<?> ith = items.get(i);
			Content.Type<?> ct = ith.getContentType();
			if (filter.includes(ct, ith.getPath())) {
				S item = (S) ith.get();
				if (f.test(item)) {
					rs.add(ith.getPath());
				}
			}
		}
		return rs;
	}

	@Override
	public void synchronise() throws IOException {
		// FIXME: this method could be made more efficient
		final java.nio.file.Path root = dir.toPath();
		// FIXME: bug here if root created with specific file filter
		List<File> files = findAll(64, dir, filter, new ArrayList<>());
		for (File f : files) {
			// Construct filename
			String filename = root.relativize(f.toPath()).toString().replace(File.separatorChar, '/');
			// Decode filename into path and content type.
			K key = encdec.decodeKey(filename);
			Content.Type<V> ct = encdec.decodeType(filename);
			// Check whether this file is recognised or not
			if (ct != null) {
				// Search for this item
				boolean matched = false;
				for (int i = 0; i != items.size(); ++i) {
					Entry<?> ith = items.get(i);
					if (ith.getPath().equals(key) && ith.getContentType() == ct) {
						matched = true;
						break;
					}
				}
				// File has been removed, so delete it :)
				if (!matched) {
					f.delete();
				}
			}
		}
		//
		for (int i = 0; i != items.size(); ++i) {
			// FIXME: this should really sanity check that we have not had a concurrent
			// modification.
			items.get(i).flush();
		}
	}

	@Override
	public Iterator<V> iterator() {
		// Add wrapping iterator which forces loading of artifacts.
		return new Iterator<>() {
			int index = 0;

			@Override
			public boolean hasNext() {
				return index < items.size();
			}

			@Override
			public V next() {
				return items.get(index++).get();
			}

		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void put(K key, V value) {
		// NOTE: yes, there is unsafe stuff going on here because we cannot easily type
		// this in Java.
		Content.Type ct = value.getContentType();
		// Update state
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			if (ith.getContentType() == ct && ith.getPath().equals(key)) {
				// Yes, overwrite existing entry
				ith.set(value);
				return;
			}
		}
		Entry<V> e = new Entry<>(key, ct);
		e.set(value);
		// Create new entry
		items.add(e);
	}

	@Override
	public void remove(K key, Content.Type<?> ct) {
		// Update state
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			if (ith.getContentType() == ct && ith.getPath().equals(key)) {
				// Yes, remove entry
				items.remove(i);
				return;
			}
		}
		// Done
	}

	/**
	 * Get the root directory where this repository starts from.
	 *
	 * @return
	 */
	public File getDirectory() {
		return dir;
	}

	@Override
	public String toString() {
		String r = "{";
		boolean firstTime = true;
		for (Entry<?> f : items) {
			if (!firstTime) {
				r += ",";
			}
			r += f.getPath();
			firstTime = false;
		}
		return r + "}";
	}

	/**
	 * Construct the initial listing of files from the contents of the build
	 * directory. Observe that this does not load the files, but rather returns a
	 * list of "place holders".
	 *
	 * @param registry
	 * @param dir
	 * @param filter
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ArrayList<Entry<V>> initialise(File dir, FileFilter filter) throws IOException {
		java.nio.file.Path root = dir.toPath();
		// First extract all files rooted in this directory
		List<File> files = findAll(64, dir, filter, new ArrayList<>());
		// Second convert them all into entries as appropriate
		ArrayList<Entry<V>> entries = new ArrayList<>();
		//
		for (int i = 0; i != files.size(); ++i) {
			File ith = files.get(i);
			String filename = root.relativize(ith.toPath()).toString().replace(File.separatorChar, '/');
			// Decode filename into path and content type.
			// Decode filename into path and content type.
			K key = encdec.decodeKey(filename);
			Content.Type<V> ct = encdec.decodeType(filename);
			if (key != null) {
				// Create lazy artifact
				entries.add(new Entry<>(key, ct));
			}
		}
		// Done
		return entries;
	}

	/**
	 * An entry within this root which corresponds (in theory) to an entry on disk.
	 * The content of an entry is loaded lazily on demand since, in general, this
	 * may involve a complex operation (e.g. decoding a structured file). An entry
	 * may not actually correspond to anything on disk if it has been created during
	 * execution, but not yet flushed to disk.
	 *
	 * @author David J. Pearce
	 *
	 * @param <S>
	 */
	private class Entry<S extends V> {
		/**
		 * The repository path to which this entry corresponds.
		 */
		private final K key;
		/**
		 * The content type of this entry
		 */
		private final Content.Type<S> contentType;
		/**
		 * Indicates whether this entry has been modified or not.
		 */
		private boolean dirty;
		/**
		 * The cached value of this entry. This may be <code>null</code> if the entry
		 * has been read from disk yet.
		 */
		private S value;

		public Entry(K key, Content.Type<S> contentType) {
			this.key = key;
			this.contentType = contentType;
			this.dirty = false;
		}

		public K getPath() {
			return key;
		}

		public Content.Type<S> getContentType() {
			return contentType;
		}

		public S get() {
			try {
				if (value == null) {
					File f = getFile();
					FileInputStream fin = new FileInputStream(f);
					value = contentType.read(fin);
					fin.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return value;
		}

		public void set(S value) {
			if (this.value != value) {
				this.dirty = true;
				this.value = value;
			}
		}

		public void flush() throws IOException {
			// Only flush if the entry is actually dirty
			if (dirty) {
				File f = getFile();
				if (!f.exists()) {
					// Create any enclosing directories as necessary.
					f.getParentFile().mkdirs();
					// Attempt to create the file.
					if (!f.createNewFile()) {
						// Error creating file occurred
						return;
					}
				}
				// File now exists, therefore we can write to it.
				FileOutputStream fout = new FileOutputStream(f);
				contentType.write(fout, value);
				fout.close();
			}
		}

		private File getFile() {
			String filename = encdec.encode(contentType, key);
			// Done.
			return new File(dir, filename);
		}
	}

	/**
	 * Extract all files starting from a given directory.
	 *
	 * @param dir
	 * @return
	 */
	private static List<File> findAll(int n, File dir, FileFilter filter, List<File> files) {
		if (n > 0 && dir.exists() && dir.isDirectory()) {
			File[] contents = dir.listFiles(filter);
			for (int i = 0; i != contents.length; ++i) {
				File ith = contents[i];
				//
				if (ith.isDirectory()) {
					findAll(n - 1, ith, filter, files);
				} else {
					files.add(ith);
				}
			}
		}
		return files;
	}
}