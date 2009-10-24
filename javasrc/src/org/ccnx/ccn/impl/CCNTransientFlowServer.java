package org.ccnx.ccn.impl;

import java.io.IOException;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;

/**
 * A simple server that takes a set of blocks and makes them available
 * to readers. Unlike standard flow controllers, this class doesn't care if
 * anyone ever reads its blocks -- it doesn't call waitForPutDrain. If no one
 * is interested, it simply persists until deleted/cleared. 
 * This version of flow server holds blocks until they have been
 * read once, manually removed or cleared, or the 
 * server is deleted by canceling all its registered prefixes. It does not
 * signal an error if the blocks are never read.
 */
public class CCNTransientFlowServer extends CCNFlowControl {

	boolean _persistent = true;
	
	public CCNTransientFlowServer(ContentName name, Integer capacity, CCNHandle handle) throws IOException {
		super(name, handle);
		if (null != capacity)
			setCapacity(capacity);
	}

	public CCNTransientFlowServer(String name, Integer capacity, CCNHandle handle)
			throws MalformedContentNameStringException, IOException {
		super(name, handle);
		if (null != capacity)
			setCapacity(capacity);
	}

	public CCNTransientFlowServer(Integer capacity, CCNHandle handle) throws IOException {
		super(handle);
		if (null != capacity)
			setCapacity(capacity);
	}
	
	/**
	 * Remove content objects after they have been read once.
	 * 
	 * @param co ContentObject to remove from flow controller.
	 * @throws IOException may be thrown by overriding subclasses
	 */
	@Override
	public void afterPutAction(ContentObject co) throws IOException {
		remove(co);
	}

	/**
	 * Do not force client to wait for content to drain -- if
	 * no one wants it, that's just fine.
	 */
	public void afterClose() {
		// do nothing
	}
}