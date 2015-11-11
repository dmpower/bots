/**
 *
 */
package com.ywp.robocode.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

/**
 * This is a data manager that keeps entries together by groups. Each group will have a
 * max number of entries determined by the threshold. When threshold+1 is added, the oldest
 * entry is removed.
 *
 * @author dpower
 *
 */
public class RepositoryManager <E> {

	private final int threshold;
	private Map<String, Queue<RepositoryEntry<E>>> repository = new HashMap<String, Queue<RepositoryEntry <E>>>();

	/**
	 * Default constructor
	 */
	public RepositoryManager() {
		this(5);
	}

	/**
	 * The constructor that allow one to control the number of entries per group
	 * @param threshold - max entries per group
	 */
	public RepositoryManager(int threshold){
		this.threshold = threshold;
	}

	public void add(RepositoryEntry <E> newEntry){
		boolean isPresent = false;

		if (this.repository.containsKey(newEntry.getGroupId())){
			for ( RepositoryEntry<E> currentEntry: this.repository.get(newEntry.getGroupId())) {
				if(currentEntry.getUniqueId().equals(newEntry.getUniqueId())){
					isPresent = true;
					break;
				}
			}
		} else{
			this.repository.put(newEntry.getGroupId(), new LinkedList<RepositoryEntry<E>>());
		}

		if (!isPresent){
			Queue<RepositoryEntry<E>> tempQueue = this.repository.get(newEntry.getGroupId());
			boolean results = tempQueue.offer(newEntry);
			System.out.println(this.getClass().getName() + " : adding " + (results?"Suceeded":"Failed") + ".");
			if ( this.threshold < tempQueue.size()){
				tempQueue.remove();
			}
			this.repository.put(newEntry.getGroupId(), tempQueue); // I guess you have to remember to update

		}
	}

	public void clear() {
		this.repository.clear();
	}

	public void removeGroup(RepositoryEntry<E> sample){
		removeGroup(sample.getGroupId());
	}

	public void removeGroup(String groupId){
		if (this.repository.containsKey(groupId)){
			this.repository.get(groupId).clear();
			this.repository.remove(groupId);
		}
	}

	public void remove(RepositoryEntry<E> entry){
		if (this.repository.containsKey(entry.getGroupId())){
			Queue<RepositoryEntry<E>> tempQueue = this.repository.get(entry.getGroupId());
			for (RepositoryEntry<E> currentEntry : tempQueue) {
				if(currentEntry.getUniqueId().equals(entry.getUniqueId())){
					tempQueue.remove(currentEntry);
					break; // there should only ever be one
				}
			}
		}
	}

	public void remove(Vector<RepositoryEntry<E>> entries){
		for (RepositoryEntry<E> entry : entries) {
			if (this.repository.containsKey(entry.getGroupId())){
				Queue<RepositoryEntry<E>> tempQueue = this.repository.get(entry.getGroupId());
				for (RepositoryEntry<E> currentEntry : tempQueue) {
					if(currentEntry.getUniqueId().equals(entry.getUniqueId())){
						tempQueue.remove(currentEntry);
						break; // there should only ever be once
					}
				}
			}
		}
	}

	public Vector<RepositoryEntry<E>> getAll(RepositoryEntry<E> sample){
		return getAll(sample.getGroupId());
	}

	public Vector<RepositoryEntry<E>> getAll(String groupId){
		Vector<RepositoryEntry<E>> results = new Vector<>();
		if (this.repository.containsKey(groupId)){
			for ( RepositoryEntry<E> currentEntry: this.repository.get(groupId)) {
				results.addElement(currentEntry);
			}

		}
		return results;
	}

	public Vector<E> getAllData (RepositoryEntry<E> sample){
		return getAllData(sample.getGroupId());
	}

	public Vector<E> getAllData (String groupId) {
		Vector<E> results = new Vector<E>();
		if (this.repository.containsKey(groupId)){
			for ( RepositoryEntry<E> currentEntry: this.repository.get(groupId)) {
				results.addElement(currentEntry.getData());
			}
		}

		return results;
	}

	public E get (RepositoryEntry<E> sample){
		E results = null;
		if (this.repository.containsKey(sample.getGroupId())){
			for ( RepositoryEntry<E> currentEntry: this.repository.get(sample.getGroupId())) {
				if (sample.getUniqueId().equals(currentEntry.getUniqueId())){
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

	public Map<String, Integer> stats(){
		Map<String, Integer> results = new HashMap<>();
		for (Entry<String, Queue<RepositoryEntry<E>>> mapEntry: this.repository.entrySet()){
			results.put(mapEntry.getKey(), mapEntry.getValue().size());
		}

		return results;
	}
}
