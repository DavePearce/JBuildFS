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
import java.util.List;
import java.util.function.Predicate;

import jbuildstore.core.Content;
import jbuildstore.core.Content.Filter;
import jbuildstore.core.Content.Type;

public class HashMapStore<K,V extends Content> implements Content.Store<K,V> {

	@Override
	public <T extends V> T get(Type<T> kind, K key) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends V> List<T> getAll(Filter<K, T> filter) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<K> match(Filter<K, ?> filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<K> match(Filter<K, T> ct, Predicate<T> p) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(K key, V value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(K key, Type<?> type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void synchronise() throws IOException {
		// TODO Auto-generated method stub

	}

}
