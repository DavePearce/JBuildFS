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

public interface Key {

	public interface Encoder<K, V> {
		/**
		 * Encode a given content type and key into a low-level type (e.g. a filename).
		 *
		 * @param s
		 * @return
		 */
		public V encode(K key);
	}

	public interface Decoder<K, V> {
		/**
		 * Decode a low-level type (e.g. a filename) into a key. In the context of a
		 * filename, for example, this might return its path.
		 *
		 * @param t
		 * @return
		 */
		public K decode(V t);

	}

	public interface EncoderDecoder<K, V> extends Encoder<K, V>, Decoder<K, V> {

	}
}
