package jbuildfs.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import jbuildfs.core.Build;
import jbuildfs.core.Build.Transaction;
import jcontent.core.Content;
import jcontent.core.Content.Registry;
import jcontent.util.Pair;
import jcontent.util.Trie;

public class ByteRepository implements Build.Repository {
	private final Content.Registry registry;
	private final ArrayList<SnapShot> states;

	public ByteRepository(Content.Registry registry, Iterable<Build.Artifact> entries) {
		this(registry, entries.iterator());
	}

	public ByteRepository(Content.Registry registry, Iterator<Build.Artifact> entries) {
		this.registry = registry;
		this.states = new ArrayList<>();
		states.add(new SnapShot(entries));
	}

	public ByteRepository(Content.Registry registry, Build.Artifact... entries) {
		this.registry = registry;
		this.states = new ArrayList<>();
		states.add(new SnapShot(entries));
	}

	@Override
	public int size() {
		return states.size();
	}

	@Override
	public Registry getContentRegistry() {
		return registry;
	}

	@Override
	public SnapShot get(int i) {
		return states.get(i);
	}

	@Override
	public <T extends Content> T get(Content.Type<T> kind, Trie path) {
		return last().get(kind, path);
	}

	@Override
	public <T extends Content> List<T> getAll(Content.Filter<T> filter) {
		return last().getAll(filter);
	}

	@Override
	public List<Trie> match(Content.Filter<? extends Content> filter) {
		return last().match(filter);
	}

	@Override
	public <S extends Content> List<Trie> match(Content.Filter<S> kind, Predicate<S> p) {
		return last().match(kind,p);
	}

	@Override
	public SnapShot last() {
		return states.get(states.size() - 1);
	}

	@Override
	public boolean apply(Transaction transaction) {
		SnapShot s = last();
		try {
			for (Build.Task t : transaction) {
				Pair<Build.SnapShot, Boolean> p = t.apply(s);
				if (!p.second()) {
					// Lots of questions as to how rollback should be managed.
					states.add((SnapShot) p.first());
					return false;
				} else {
					s = (SnapShot) p.first();
				}
			}
			states.add(s);
			return true;
		} catch (Throwable e) {
			// NOTES: Unclear to me whether this makes sense. However, we need to get
			// information out of the system when something goes wrong, and this is the only
			// way.
			states.add((SnapShot) s);
			throw e;
		}
	}

	@Override
	public String toString() {
		String r = "";
		for(SnapShot s : states) {
			r += s.toString();
		}
		return r;
	}

	public class SnapShot implements Build.SnapShot, Iterable<Build.Artifact> {
		private final ArrayList<Build.Artifact> items = new ArrayList<>();

		public SnapShot(Iterator<Build.Artifact> entries) {
			while(entries.hasNext()) {
				items.add(entries.next());
			}
		}

		public SnapShot(Build.Artifact... entries) {
			for (int i = 0; i != entries.length; ++i) {
				items.add(entries[i]);
			}
		}

		private SnapShot(SnapShot s) {
			this.items.addAll(s.items);
		}

		@Override
		public Registry getContentRegistry() {
			return ByteRepository.this.registry;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Content> List<Trie> match(Content.Filter<T> cf, Predicate<T> query) {
			ArrayList<Trie> es = new ArrayList<>();
			for (Build.Artifact e : items) {
				if (cf.includes(e.getContentType(), e.getPath()) && query.test((T) e)) {
					es.add(e.getPath());
				}
			}
			return es;
		}

		@Override
		public List<Trie> match(Content.Filter<? extends Content> filter) {
			ArrayList<Trie> es = new ArrayList<>();
			for (Build.Artifact e : items) {
				if (filter.includes(e.getContentType(),e.getPath())) {
					es.add(e.getPath());
				}
			}
			return es;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Content> T get(Content.Type<T> ct, Trie path) {
			for (Build.Artifact e : items) {
				if (e.getContentType() == ct && e.getPath().equals(path)) {
					return (T) e;
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Content> List<T> getAll(Content.Filter<T> filter) {
			ArrayList<T> es = new ArrayList<>();
			for (Build.Artifact e : items) {
				if (filter.includes(e.getContentType(),e.getPath())) {
					es.add((T) e);
				}
			}
			return es;
		}

		@Override
		public SnapShot put(Build.Artifact entry) {
			// Clone state
			SnapShot s = new SnapShot(this);
			// Update state
			for (int i = 0; i != items.size(); ++i) {
				Build.Artifact e = items.get(i);
				if (e.getClass() == entry.getClass() && e.getPath().equals(entry.getPath())) {
					// Overwrite existing entry
					s.items.set(i, entry);
					return s;
				}
			}
			// Create new entry
			s.items.add(entry);
			// Done
			return s;
		}

		@Override
		public Iterator<Build.Artifact> iterator() {
			return items.iterator();
		}

		@Override
		public String toString() {
			String r = "{";
			boolean firstTime=true;
			for(Build.Artifact f : items) {
				r = firstTime ? r : r + ",";
				r += f.getPath() + ":" + f.getContentType().getSuffix();
				firstTime=false;
			}
			return r + "}";
		}
	}
}