/**
 *
 */
package com.ywp.robocode.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

/**
 * This is a data manager that keeps entries together by groups. Each group will
 * have a max number of entries determined by the threshold. When threshold+1 is
 * added, the oldest entry is removed.
 *
 * @author dpower
 *
 */
public class RepositoryManager<E> {

	private final int							  threshold;
	private Map<String, List<RepositoryEntry<E>>> repository = new HashMap<String, List<RepositoryEntry<E>>>();

	/**
	 * Default constructor
	 */
	public RepositoryManager() {
		this(5);
	}

	/**
	 * The constructor that allow one to control the number of entries per group
	 *
	 * @param threshold
	 *            - max entries per group
	 */
	public RepositoryManager(int threshold) {
		this.threshold = threshold;
	}

	public void add(RepositoryEntry<E> newEntry) {
		boolean isPresent = false;

		if (this.repository.containsKey(newEntry.getGroupId())) {
			for (RepositoryEntry<E> currentEntry : this.repository.get(newEntry.getGroupId())) {
				if (currentEntry.getUniqueId().equals(newEntry.getUniqueId())) {
					isPresent = true;
					break;
				}
			}
		} else {
			this.repository.put(newEntry.getGroupId(), new ArrayList<RepositoryEntry<E>>());
		}

		if (!isPresent) {
			List<RepositoryEntry<E>> tempList = this.repository.get(newEntry.getGroupId());
			tempList.add(0, newEntry);
			if (this.threshold < tempList.size()) {
				// System.out.println(this.getClass().getName() + " : removing
				// an extra.");
				tempList.remove(tempList.size() - 1);
			}
			// System.out.println(this.getClass().getName() + " : updating
			// entry. queue size: " + tempList.size());

		} else {
			System.out.println(
					this.getClass().getName() + " - Dropping: " + newEntry.getUniqueId() + " as it already exists.");
		}
	}

	public void clear() {
		this.repository.clear();
	}

	public void removeGroup(RepositoryEntry<E> sample) {
		removeGroup(sample.getGroupId());
	}

	public void removeGroup(String groupId) {
		if (this.repository.containsKey(groupId)) {
			this.repository.get(groupId).clear();
			this.repository.remove(groupId);
		}
	}

	public void remove(RepositoryEntry<E> entry) {
		if (this.repository.containsKey(entry.getGroupId())) {
			List<RepositoryEntry<E>> tempList = this.repository.get(entry.getGroupId());
			for (RepositoryEntry<E> currentEntry : tempList) {
				if (currentEntry.getUniqueId().equals(entry.getUniqueId())) {
					tempList.remove(currentEntry);
					break; // there should only ever be one
				}
			}
		}
	}

	public void remove(Vector<RepositoryEntry<E>> entries) {
		for (RepositoryEntry<E> entry : entries) {
			if (this.repository.containsKey(entry.getGroupId())) {
				List<RepositoryEntry<E>> tempList = this.repository.get(entry.getGroupId());
				for (RepositoryEntry<E> currentEntry : tempList) {
					if (currentEntry.getUniqueId().equals(entry.getUniqueId())) {
						tempList.remove(currentEntry);
						break; // there should only ever be once
					}
				}
			}
		}
	}

	public Vector<RepositoryEntry<E>> getAll(RepositoryEntry<E> sample) {
		return getAll(sample.getGroupId());
	}

	public Vector<RepositoryEntry<E>> getAll(String groupId) {
		Vector<RepositoryEntry<E>> results = new Vector<>();
		if (this.repository.containsKey(groupId)) {
			// for ( RepositoryEntry<E> currentEntry:
			// this.repository.get(groupId)) {
			// results.addElement(currentEntry);
			// }
			results.addAll(this.repository.get(groupId));
		}
		return results;
	}

	public Vector<E> getAllData(RepositoryEntry<E> sample) {
		return getAllData(sample.getGroupId());
	}

	public Vector<E> getAllData(String groupId) {
		Vector<E> results = new Vector<E>();
		if (this.repository.containsKey(groupId)) {
			for (RepositoryEntry<E> currentEntry : this.repository.get(groupId)) {
				results.addElement(currentEntry.getData());
			}
		}

		return results;
	}

	public E get(RepositoryEntry<E> sample) {
		E results = null;
		if (this.repository.containsKey(sample.getGroupId())) {
			for (RepositoryEntry<E> currentEntry : this.repository.get(sample.getGroupId())) {
				if (sample.getUniqueId().equals(currentEntry.getUniqueId())) {
					results = currentEntry.getData();
					break;
				}
			}
		}

		return results;

	}

	/**
	 * Get all the current group id being tracked
	 *
	 * @return - a set containing the group IDs
	 */
	public Set<String> getAllGroupIds() {
		return this.repository.keySet();
	}

	public Map<String, Integer> stats() {
		Map<String, Integer> results = new HashMap<>();
		for (Entry<String, List<RepositoryEntry<E>>> mapEntry : this.repository.entrySet()) {
			results.put(mapEntry.getKey(), mapEntry.getValue().size());
		}

		return results;
	}
}
