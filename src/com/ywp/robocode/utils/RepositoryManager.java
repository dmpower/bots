/**
 *
 */
package com.ywp.robocode.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Vector;

/**
 * @author dpower
 *
 */
public class RepositoryManager <E> {

	private int threshHold;
	private Map<String, Queue<RepositoryEntry<E>>> repository = new HashMap<String, Queue<RepositoryEntry <E>>>();

	public RepositoryManager() {
		this(5);
	}

	public RepositoryManager(int threshHold){
		this.threshHold = threshHold;
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
			tempQueue.offer(newEntry);
			if ( this.threshHold < tempQueue.size()){
				tempQueue.remove();
			}

		}
	}

	public void removeAll(RepositoryEntry<E> sample){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.remove(sample.getGroupId());
		if (null != tempQueue){
			tempQueue.clear();
		}
	}

	public void remove(RepositoryEntry<E> target){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.get(target.getGroupId());
		if (null != tempQueue){
			for ( RepositoryEntry<E> currentEntry: tempQueue) {
				if(currentEntry.getUniqueId().equals(target.getUniqueId())){
					tempQueue.remove(currentEntry);
					break; // there should only ever be one
				}
			}
		}
	}

	public Vector<E> getAll (RepositoryEntry<E> sample){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.get(sample.getGroupId());
		Vector<E> results = new Vector<E>();
		if (null != tempQueue){
			for ( RepositoryEntry<E> currentEntry: tempQueue) {
				results.addElement(currentEntry.getData());
			}
		}

		return results;
	}

	public E get (RepositoryEntry<E> sample){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.get(sample.getGroupId());
		E results = null;
		if (null != tempQueue){
			for ( RepositoryEntry<E> currentEntry: tempQueue) {
				if (sample.getUniqueId().equals(currentEntry.getUniqueId())){
					results = currentEntry.getData();
					break;
				}
			}
		}

		return results;

	}

	public Map<String, Integer> stats(){
		Map<String, Integer> results = new HashMap<>();
		for (Entry<String, Queue<RepositoryEntry<E>>> mapEntry: this.repository.entrySet()){
			results.put(mapEntry.getKey(), mapEntry.getValue().size());
		}

		return results;
	}
}
