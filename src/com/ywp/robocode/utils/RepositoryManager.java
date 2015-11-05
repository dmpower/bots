/**
 *
 */
package com.ywp.robocode.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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

		if (this.repository.containsKey(newEntry.getSortId())){
			for ( RepositoryEntry<E> currentEntry: this.repository.get(newEntry.getSortId())) {
				if(currentEntry.getUniqueId().equals(newEntry.getUniqueId())){
					isPresent = true;
					break;
				}
			}
		} else{
			this.repository.put(newEntry.getSortId(), new LinkedList<RepositoryEntry<E>>());
		}

		if (!isPresent){
			Queue<RepositoryEntry<E>> tempQueue = this.repository.get(newEntry.getSortId());
			tempQueue.offer(newEntry);
			if ( this.threshHold < tempQueue.size()){
				tempQueue.remove();
			}

		}
	}

	public void removeAll(RepositoryEntry<E> sample){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.remove(sample.getSortId());
		if (null != tempQueue){
			tempQueue.clear();
		}
	}

	public void remove(RepositoryEntry<E> target){
		Queue<RepositoryEntry<E>> tempQueue = this.repository.get(target.getUniqueId());
		if (null != tempQueue){
			for ( RepositoryEntry<E> currentEntry: tempQueue) {
				if(currentEntry.getUniqueId().equals(target.getUniqueId())){
					tempQueue.remove(currentEntry);
					break; // there should only ever be one
				}
			}
		}
	}
}
