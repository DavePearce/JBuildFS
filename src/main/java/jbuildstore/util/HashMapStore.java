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
import jbuildstore.core.Key;

public class HashMapStore<S> implements Content.Store<S>, Iterable<Content.Entry<S>> {
	private final HashMap<Key<S, ?>, Content> map;

	public HashMapStore() {
		this.map = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Content> T get(Key<S, T> key) {
		return (T) map.get(key);
	}

	@Override
	public <T extends Content> List<T> getAll(Predicate<Key<S,?>> query) throws IOException {
		ArrayList<T> items = new ArrayList<>();
		for(Map.Entry<Key<S,?>,Content> e : map.entrySet()) {
			if(query.test(e.getKey())) {
				items.add((T) e.getValue());
			}
		}
		return items;
	}

	@Override
	public <T extends Content> List<Key<S, T>> match(Predicate<Key<S, ?>> query) {
		throw new UnsupportedOperationException("implement me");
	}

	@Override
	public <T extends Content> void put(Key<S,T> key, T value) {
		if(key.contentType() != value.contentType()) {
			throw new IllegalArgumentException("invalid key-value pair");
		}
		map.put(key, value);
	}

	@Override
	public void remove(Key<S,?> key) {
		map.remove(key);
	}

	@Override
	public Iterator<Content.Entry<S>> iterator() {
		final Iterator<Map.Entry<Key<S, ?>, Content>> iter = map.entrySet().iterator();
		//
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public jbuildstore.core.Content.Entry<S> next() {
				Map.Entry<Key<S, ?>, Content> e = iter.next();
				//
				return new Content.Entry<>() {

					@Override
					public Key<S, ?> getKey() {
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
