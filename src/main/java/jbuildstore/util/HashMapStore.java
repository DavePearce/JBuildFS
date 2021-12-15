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
import java.util.function.Function;
import jbuildstore.core.Content;

public class HashMapStore<K extends Content.Key<?>> implements Content.Store<K>, Iterable<Content.Entry<K, Content>> {
	private final HashMap<K,Content> map;

	public HashMapStore() {
		this.map = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Content, S extends Content.Key<T>> T get(S key) {
		return (T) map.get(key);
	}

	@Override
	public <T extends Content, S extends Content.Key<T>> List<T> getAll(Function<K,S> query) throws IOException {
		ArrayList<T> items = new ArrayList<>();
		for(Map.Entry<K,Content> e : map.entrySet()) {
			S key = query.apply(e.getKey());
			if (key != null) {
				items.add((T) e.getValue());
			}
		}
		return items;
	}

	@Override
	public <T extends Content, S extends Content.Key<T>> List<S> match(Function<K, S> query) {
		throw new UnsupportedOperationException("implement me");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void put(K key, Content value) {
		if(key.contentType() != value.contentType()) {
			throw new IllegalArgumentException("invalid key-value pair");
		}
		map.put(key, value);
	}

	@Override
	public void remove(K key) {
		map.remove(key);
	}

	@Override
	public Iterator<Content.Entry<K, Content>> iterator() {
		final Iterator<Map.Entry<K, Content>> iter = map.entrySet().iterator();
		//
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public jbuildstore.core.Content.Entry<K, Content> next() {
				Map.Entry<K, Content> e = iter.next();
				//
				return new Content.Entry<K, Content>() {

					@Override
					public K getKey() {
						return e.getKey();
					}

					@Override
					public Content get() {
						return e.getValue();
					}

					@Override
					public String toString() {
						return e.getKey() + "=" + e.getValue();
					}
				};
			}
		};
	}

	@Override
	public void synchronise() throws IOException {
		throw new UnsupportedOperationException("implement me");
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
