package org.ccnx.ccn.utils;

import java.io.FileOutputStream;
import java.io.IOException;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;


public class get {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			return;
		}
		
		try {
			// If we get one file name, put as the specific name given.
			// If we get more than one, put underneath the first as parent.
			// Ideally want to use newVersion to get latest version. Start
			// with random version.
			ContentName argName = ContentName.fromURI(args[0]);
			
			CCNHandle library = CCNHandle.open();
			
			if (args.length == 2) {
				// Adjust to use defragmenting interface, find latest
				// version, etc...
				ContentObject object = library.get(argName, CCNHandle.NO_TIMEOUT);
				
				System.out.println("Retrieved an object named: " + argName);
				System.out.println("Writing to file " + args[1]);
			
				FileOutputStream fos = new FileOutputStream(args[1]);
				fos.write(object.content());
				fos.flush();
				fos.close();
				
			} else {
				// put something more interesting here
				usage();
				return;
			}
			System.exit(0);
		} catch (ConfigurationException e) {
			System.out.println("Configuration exception in get: " + e.getMessage());
			e.printStackTrace();
		} catch (MalformedContentNameStringException e) {
			System.out.println("Malformed name: " + args[0] + " " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Cannot write file. " + e.getMessage());
			e.printStackTrace();
		} 

	}
	
	public static void usage() {
		System.out.println("usage: get <ccnname> <filename>");
	}

}