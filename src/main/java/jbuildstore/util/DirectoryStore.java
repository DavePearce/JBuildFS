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
public class DirectoryStore<K, V extends Content> implements Content.Store<K, V>, Iterable<Content.Entry<K, V>> {
	public final static FileFilter NULL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return true;
		}
	};
	private final File dir;
	private final FileFilter filter;
	private final Key.EncoderDecoder<K, V, String> encdec;
	private final ArrayList<Entry> items;

	public DirectoryStore(Key.EncoderDecoder<K, V, String> encdec, File dir) throws IOException {
		this(encdec, dir, NULL_FILTER);
	}

	public DirectoryStore(Key.EncoderDecoder<K, V, String> encdec, File dir, FileFilter filter) throws IOException {
		if(encdec == null) {
			throw new IllegalArgumentException("Content encoder/decoder is required");
		}
		if(dir == null) {
			throw new IllegalArgumentException("Directory root is required");
		}
		if(filter == null) {
			throw new IllegalArgumentException("File filter is required");
		}
		this.encdec = encdec;
		this.dir = dir;
		this.filter = filter;
		this.items = initialise(dir, filter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends V> T get(Content.Type<T> kind, K key) {
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			Content.Type<?> ct = ith.getContentType();
			if (ith.getKey().equals(key) && ct == kind) {
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
			Entry ith = items.get(i);
			if (filter.includes(ith.getContentType(), ith.getKey())) {
				rs.add((T) ith.get());
			}
		}
		return rs;
	}

	@Override
	public List<K> match(Content.Filter<K, ?> filter) {
		ArrayList<K> rs = new ArrayList<>();
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			if (filter.includes(ith.getContentType(), ith.getKey())) {
				rs.add(ith.getKey());
			}
		}
		return rs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> List<K> match(Content.Filter<K, S> filter, Predicate<S> f) {
		ArrayList<K> rs = new ArrayList<>();
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			Content.Type<?> ct = ith.getContentType();
			if (filter.includes(ct, ith.getKey())) {
				S item = (S) ith.get();
				if (f.test(item)) {
					rs.add(ith.getKey());
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
					Entry ith = items.get(i);
					if (ith.getKey().equals(key) && ith.getContentType() == ct) {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Iterator<Content.Entry<K, V>> iterator() {
		// Add wrapping iterator which forces loading of artifacts.
		return (Iterator) items.iterator();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void put(K key, V value) {
		if(key == null) {
			throw new IllegalArgumentException("key required");
		}
		// NOTE: yes, there is unsafe stuff going on here because we cannot easily type
		// this in Java.
		Content.Type ct = value.getContentType();
		// Update state
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			if (ith.getContentType() == ct && ith.getKey().equals(key)) {
				// Yes, overwrite existing entry
				ith.set(value);
				return;
			}
		}
		Entry e = new Entry(key, ct);
		e.set(value);
		// Create new entry
		items.add(e);
	}

	@Override
	public void remove(K key, Content.Type<?> ct) {
		// Update state
		for (int i = 0; i != items.size(); ++i) {
			Entry ith = items.get(i);
			if (ith.getContentType() == ct && ith.getKey().equals(key)) {
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
		for (Entry f : items) {
			if (!firstTime) {
				r += ",";
			}
			r += f.getKey();
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
	private ArrayList<Entry> initialise(File dir, FileFilter filter) throws IOException {
		java.nio.file.Path root = dir.toPath();
		// First extract all files rooted in this directory
		List<File> files = findAll(64, dir, filter, new ArrayList<>());
		// Second convert them all into entries as appropriate
		ArrayList<Entry> entries = new ArrayList<>();
		//
		for (int i = 0; i != files.size(); ++i) {
			File ith = files.get(i);
			String filename = root.relativize(ith.toPath()).toString().replace(File.separatorChar, '/');
			// Decode filename into path and content type.
			// Decode filename into path and content type.
			K key = encdec.decodeKey(filename);
			Content.Type<V> ct = encdec.decodeType(filename);
			if (ct != null && key != null) {
				// Create lazy artifact
				entries.add(new Entry(key, ct));
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
	private class Entry implements Content.Entry<K,V> {
		/**
		 * The repository path to which this entry corresponds.
		 */
		private final K key;
		/**
		 * The content type of this entry
		 */
		private final Content.Type<? extends V> contentType;
		/**
		 * Indicates whether this entry has been modified or not.
		 */
		private boolean dirty;
		/**
		 * The cached value of this entry. This may be <code>null</code> if the entry
		 * has been read from disk yet.
		 */
		private V value;

		public Entry(K key, Content.Type<? extends V> contentType) {
			this.key = key;
			this.contentType = contentType;
			this.dirty = false;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public Content.Type<? extends V> getContentType() {
			return contentType;
		}

		public V get() {
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

		@Override
		public <S extends V> S get(Class<S> kind) {
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
			if (kind.isInstance(value)) {
				return (S) value;
			} else {
				throw new IllegalArgumentException("invalid content kind");
			}
		}

		public void set(V value) {
			if (this.value != value) {
				this.dirty = true;
				this.value = value;
			}
		}

		@SuppressWarnings("unchecked")
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
				@SuppressWarnings("rawtypes")
				Content.Type ct = contentType;
				ct.write(fout, value);
				fout.close();
			}
		}

		private File getFile() {
			String filename = encdec.encode((Content.Type) contentType, key);
			// Done.
			return new File(dir, filename);
		}

		@Override
		public String toString() {
			return key + ":" + contentType.suffix();
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