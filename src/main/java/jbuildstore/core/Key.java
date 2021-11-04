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

import jbuildstore.core.Content.Type;

public interface Key {

	public interface Encoder<K, V, T> {
		/**
		 * Encode a given content type and key into a low-level type (e.g. a filename).
		 *
		 * @param s
		 * @return
		 */
		public T encode(Type<? extends V> type, K key);
	}

	public interface Decoder<K, V, T> {
		/**
		 * Decode a low-level type (e.g. a filename) into a key. In the context of a
		 * filename, for example, this might return its path.
		 *
		 * @param t
		 * @return
		 */
		public K decodeKey(T t);

		/**
		 * Decode a low-level type (e.g. a filename) into a
		 * <code>Content.Type</code>content type. In the context of a filename, for
		 * example, the <code>Content.Type</code> returned might depend upon the suffix.
		 * Observe this may fail and return <code>null</code> for content types which
		 * are not supported.
		 *
		 * @param t
		 * @return
		 */
		public Content.Type<V> decodeType(T t);
	}

	public interface EncoderDecoder<K, V, T> extends Encoder<K, V, T>, Decoder<K, V, T> {

	}
}
