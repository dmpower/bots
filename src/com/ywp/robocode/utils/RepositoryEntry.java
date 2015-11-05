/**
 *
 */
package com.ywp.robocode.utils;

/**
 * @author dpower
 *
 */
public interface RepositoryEntry <E> {

	String getGroupId();

	String getUniqueId();

	E getData();

}
