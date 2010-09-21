/*
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.ccnx.ccn.profiles.repo;

import java.io.IOException;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.profiles.CommandMarker;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;

public class RepositoryBulkImport {
	public static boolean bulkImport(CCNHandle handle, String fileName, long timeout) throws IOException {
		// Create an Interest
		CommandMarker argMarker = CommandMarker.getMarker(CommandMarker.COMMAND_MARKER_REPO_ADD_FILE.getBytes());
		argMarker.addArgument(fileName);
		ContentObject co = handle.get(new ContentName(new byte[][]{argMarker.getBytes()}), timeout);
		return co != null;
	}
}
