/**
 *
 */
package com.ywp.robocode.utils;

/**
 * @author dpower
 *
 */
public interface RepositoryEntry <E> {
	String getSortId();

	String getUniqueId();

	E getData();

}
