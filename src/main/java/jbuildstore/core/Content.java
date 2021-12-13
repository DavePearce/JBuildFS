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
package jbuildstore.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;

/**
 * Provides various interfaces for mapping the structured content held in memory
 * to unstructured content held on disk. For example, we can define sources of
 * structured content from directories or compressed archives.
 *
 * @author David J. Pearce
 *
 */
public interface Content {

	/**
	 * Get the content type associated with this piece of content.
	 *
	 * @return
	 */
	public Content.Type<?> getContentType();

	/**
	 * Minimal requirements for a content key.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public interface Key<T> {
		/**
		 * Get the content type identified by this key.
		 *
		 * @return
		 */
		public Content.Type<T> getContentType();
	}

	/**
	 * Identifies a given piece of content in a store of some kind, and provides an
	 * API for reading / writing it.
	 *
	 * @author David J. Pearce
	 *
	 * @param <K>
	 */
	public interface Entry<K, T> {
		/**
		 * Get the identifing key for this particular piece of content.
		 *
		 * @return
		 */
		public K getKey();

		/**
		 * Read this particular piece of content.
		 *
		 * @param <T>
		 * @param kind
		 * @return
		 */
		public T get();
	}

	/**
	 * Provides an abstract mechanism for reading and writing file in
	 * a given format. Whiley source files (*.whiley) are one example, whilst JVM
	 * class files (*.class) are another.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public interface Type<T> {
		/**
		 * Physically read the raw bytes from a given input stream and convert into the
		 * format described by this content type.
		 *
		 * @param input    Input stream representing in the format described by this
		 *                 content type.
		 * @param registry Content registry to be used for creating content within the
		 *                 given type.
		 * @return
		 */
		public T read(InputStream input) throws IOException;

		/**
		 * Convert an object in the format described by this content type into
		 * an appropriate byte stream and write it to an output stream
		 *
		 * @param output
		 *            --- stream which this value is to be written to.
		 * @param value
		 *            --- value to be converted into bytes.
		 */
		public void write(OutputStream output, T value) throws IOException;

		/**
		 * Return an appropriate suffix for this content type. This is used to identify
		 * instances stored on disk (for example).
		 *
		 * @return
		 */
		public String suffix();
	}

	/**
	 * Provides a general mechanism for reading content from a given source.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Source<K, V extends Content> {
		/**
		 * Get a given piece of content from this source.
		 *
		 * @param <T>
		 * @param kind
		 * @param key
		 * @return
		 */
		public <T extends V, S extends Key<T>> T get(S key) throws IOException;

		/**
		 * Get a given piece of content from this source.
		 *
		 * @param <T>
		 * @param kind
		 * @param p
		 * @return
		 */
		public <T extends V, S extends Key<T>> List<T> getAll(Function<K, S> query) throws IOException;

		/**
		 * Find all content matching a given filter.
		 *
		 * @param <S>
		 * @param kind
		 * @param f
		 * @return
		 */
		public <T extends V, S extends Key<T>> List<S> match(Function<K, S> query);
	}

	/**
	 * A ledger is content source which is additionally journaled. That means we can
	 * see <i>differences</i> between files, rather than just the state of the files
	 * are they are now. In particular, the ledger provides sequence of
	 * <i>snapshots</i> which determine the state at different points in time.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Ledger<K,V extends Content> extends Source<K,V> {
		/**
		 * Get the number of snapshots within the ledger.
		 *
		 * @return
		 */
		public int size();

		/**
		 * Get a given snapshot from within this ledger. The snapshot handle must be
		 * between <code>0</code> and <code>size()-1</code>.
		 */
		public Source<K,V> get(int snapshot);
	}

	/**
	 * Provides a general mechanism for writing content into a given source.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Sink<K,V extends Content> {
		/**
		 * Write a given piece of content into this sink.
		 *
		 * @param <T>
		 * @param kind
		 * @param key
		 * @param value
		 */
		public void put(K key, V value);

		/**
		 * Remove a given piece of content from this sink.
		 * @param key
		 */
		public void remove(K key);
	}

	/**
	 * A content store represents an interface to an underlying medium (e.g. the file
	 * system). As such it provides both read and write access, along with the
	 * ability for synchronisation.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Store<K, V extends Content> extends Source<K, V>, Sink<K, V> {
		/**
		 * Synchronise this root against the underlying medium. This does two things. It
		 * flushes writes and invalidates items which have changed on disk. Invalidate
		 * items will then be reloaded on demand when next requested.
		 */
		public void synchronise() throws IOException;
	}
}
