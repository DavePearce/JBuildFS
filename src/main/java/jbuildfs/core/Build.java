// Copyright 2011 The Whiley Project Developers
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
package jbuildfs.core;

import java.util.List;
import java.util.function.Function;

import jbuildfs.util.Pair;
import jbuildfs.util.Trie;

public interface Build {

	/**
	 * Represents a versioned view of the "build", including all generated
	 * artifacts.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Repository extends Content.Ledger {
		/**
		 * Apply a given transaction to this repository. This will be given the latest
		 * snapshot when it is executed. The resulting snapshot well then become the
		 * head (if no other snapshots have been written inbetween) or will be merged
		 * (if possible).
		 *
		 * @param transformer
		 */
		public boolean apply(Transaction transaction);

		/**
		 * Get the ith state within this repository
		 *
		 * @param i
		 * @return
		 */
		@Override
		public SnapShot get(int i);

		/**
		 * Get current state of the build system
		 *
		 * @return
		 */
		public SnapShot last();

		@Override
		public <T extends Content> T get(Content.Type<T> kind, Trie p);

		@Override
		public <T extends Content> List<T> getAll(Content.Filter<T> filter);
	}

	/**
	 * Represents a snapshot of the repository at a given point in time.
	 *
	 * @param <S>
	 */
	public interface SnapShot extends Content.Source, Iterable<Build.Artifact> {
		@Override
		public <T extends Content> T get(Content.Type<T> kind, Trie p);

		@Override
		public <T extends Content> List<T> getAll(Content.Filter<T> filter);

		/**
		 * Write a specific artifact to this snapshot, thereby producing a new snapshot.
		 *
		 * @param entry
		 * @param <T>
		 * @return
		 */
		public <T extends Artifact> SnapShot put(T entry);
	}

	public interface Transaction extends Iterable<Task> {
		/**
		 * Returns the number of tasks in this transaction.
		 *
		 * @return
		 */
		public int size();

		/**
		 * Get the ith task in this transaction.
		 *
		 * @param ith
		 * @return
		 */
		public Task get(int ith);
	}

	/**
	 * Represents a given "build artifact" within a repository. This could a
	 * SourceFile, or some kind of structured syntax tree or intermediate
	 * representation. It could also be a binary target.
	 */
	public interface Artifact extends Content {
		/**
		 * Get the location within the build of this artifact.
		 *
		 * @return
		 */
		public Trie getPath();

		/**
		 * Get the content type of this artifact.
		 *
		 * @return
		 */
		@Override
		public Content.Type<? extends Artifact> getContentType();

		/**
		 * Get all the source artifacts that contributed to this artifact. Observe that,
		 * if this is a source file, then this list is always empty!
		 *
		 * @return
		 */
		public List<? extends Build.Artifact> getSourceArtifacts();
	}

	/**
	 * A shortlived unit of work responsible for generating a given build artifact
	 * (e.g. converting one or more files of a given type into a given target file).
	 * Tasks which are not dependent on each other may be scheduled in parallel.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Task extends Artifact, Function<SnapShot, Pair<SnapShot, Boolean>> {
	}
}
