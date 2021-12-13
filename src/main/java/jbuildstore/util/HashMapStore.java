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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jbuildstore.core.Content;
import jbuildstore.core.Content.Filter;
import jbuildstore.core.Content.Type;

public class HashMapStore<K,V extends Content> implements Content.Store<K,V>, Iterable<Content.Entry<K, V>>{
	private final HashMap<Entry<K,V>,V> map;

	public HashMapStore() {
		this.map = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends V> T get(Type<T> kind, K key) throws IOException {
		return (T) map.get(new Entry<K,V>(kind, key));
	}

	@Override
	public <T extends V> List<T> getAll(Filter<K, T> filter) throws IOException {
		ArrayList<T> items = new ArrayList<>();
		for(Map.Entry<Entry<K,V>,V> e : map.entrySet()) {
			Content.Type<?> ct = e.getKey().contentType;
			K key = e.getKey().key;
			if (filter.includes(ct, key)) {
				items.add((T) e.getValue());
			}
		}
		return items;
	}

	@Override
	public List<K> match(Filter<K, ?> filter) {
		throw new UnsupportedOperationException("implement me");
	}

	@Override
	public <T> List<K> match(Filter<K, T> ct, Predicate<T> p) {
		throw new UnsupportedOperationException("implement me");
	}

	@Override
	public void put(K key, V value) {
		System.out.println("WRITING: " + key + ":" + value);
		Content.Type ct = value.getContentType();
		map.put(new Entry<K,V>(ct, key), value);
	}

	@Override
	public void remove(K key, Type<?> type) {
		throw new UnsupportedOperationException("implement me");
	}

	@Override
	public Iterator<Content.Entry<K, V>> iterator() {
		final Iterator<Map.Entry<Entry<K,V>,V>> iter = map.entrySet().iterator();
		//
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public jbuildstore.core.Content.Entry<K, V> next() {
				Map.Entry<Entry<K, V>, V> e = iter.next();
				//
				return new Content.Entry<K, V>() {

					@Override
					public K getKey() {
						return e.getKey().key;
					}

					@Override
					public Type<? extends V> getContentType() {
						return e.getKey().contentType;
					}

					@Override
					public <S extends V> S get(Class<S> kind) {
						V v = e.getValue();
						if (kind.isInstance(v)) {
							return (S) v;
						} else {
							throw new IllegalArgumentException("invalid kind");
						}
					}
				};
			}
		};
	}

	@Override
	public void synchronise() throws IOException {
		throw new UnsupportedOperationException("implement me");
	}

	private static final class Entry<K,V> {
		private final Content.Type<? extends V> contentType;
		private final K key;

		public Entry(Content.Type<? extends V> contentType, K key) {
			if(contentType == null || key == null) {
				throw new IllegalArgumentException("invalid content type or key");
			}
			this.contentType = contentType;
			this.key = key;
		}

		@Override
		public boolean equals(Object o) {
			if(o instanceof Entry) {
				Entry<?,?> e = (Entry<?,?>) o;
				return contentType.equals(e.contentType) && key.equals(e.key);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return contentType.hashCode() ^ key.hashCode();
		}

		@Override
		public String toString() {
			return key + ":" + contentType.suffix();
		}
	}
}
