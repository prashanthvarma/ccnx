package org.ccnx.ccn.test.profiles.access;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;

import javax.xml.stream.XMLStreamException;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.config.UserConfiguration;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.content.Link;
import org.ccnx.ccn.profiles.access.AccessControlManager;
import org.ccnx.ccn.profiles.access.AccessControlProfile;
import org.ccnx.ccn.profiles.access.AccessDeniedException;
import org.ccnx.ccn.profiles.access.Group;
import org.ccnx.ccn.profiles.access.GroupManager;
import org.ccnx.ccn.profiles.nameenum.EnumeratedNameList;
import org.ccnx.ccn.protocol.ContentName;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;



public class GroupTestRepo {
	
	static boolean USE_REPO = true;
	static int NUM_USERS = 20;
	static char [] USER_PASSWORD = new String("password").toCharArray();
	
	static ContentName testStorePrefix = null;
	static ContentName userStore = null;
	static ContentName groupStore = null;
	
	static EnumeratedNameList _userList = null;
	static CCNHandle _library = null;
	
	static String myUserName = null;
	
	/**
	 * Have to make a bunch of users.
	 * @throws Exception
	 */
	static TestUserData users = null;
	static CCNHandle userLibrary = null;
	static AccessControlManager _acm = null;
	static GroupManager _gm = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception { 
		try {
			myUserName = UserConfiguration.userName();
			System.out.println("Username = " + myUserName);
			testStorePrefix = UserConfiguration.defaultNamespace();
			userStore = ContentName.fromNative(testStorePrefix, "home");
			groupStore = AccessControlProfile.groupNamespaceName(testStorePrefix);
			
			_library = CCNHandle.open();
			System.out.println("group store: " + groupStore);
			_acm = new AccessControlManager(testStorePrefix, groupStore, userStore);
			_acm.publishMyIdentity(myUserName, KeyManager.getDefaultKeyManager().getDefaultPublicKey());
			_userList = _acm.userList();
			_gm = _acm.groupManager();

	//		users = new TestUserData(userStore, NUM_USERS, USE_REPO, USER_PASSWORD, userLibrary);
		} catch (Exception e) {
			Log.warning("Exception in setupBeforeClass: " + e);
			Log.warningStackTrace(e);
			throw e;
		}
	}

	private void testRemoveUsers(ArrayList<Link> removeMembers, Group grp) throws InvalidKeyException, CryptoException, XMLStreamException, IOException, ConfigurationException{
		boolean succeed = false;
		int retries = 0;
		while(!succeed){
			retries ++;
			System.out.print("................trying to remove user...........");
			try{
					grp.removeUsers(removeMembers);
					succeed = true;
			}catch(AccessDeniedException e){
				succeed = false;
			}
		}
		System.out.println(".....................removed user, number of retries:..............." + retries);
	}
	
	@Test
	public void testCreateGroup() {
		try {
			Assert.assertNotNull(_userList);
			ContentName prefixTest = _userList.getName();
			Assert.assertNotNull(prefixTest);
			Log.info("***************** Prefix is "+ prefixTest.toString());
			Assert.assertEquals(prefixTest, userStore);
			
			_userList.waitForData();
			Assert.assertTrue(_userList.hasNewData());
			SortedSet<ContentName> returnedBytes = _userList.getNewData();
			Assert.assertNotNull(returnedBytes);
			Assert.assertFalse(returnedBytes.isEmpty());
			
			System.out.println("Got " + returnedBytes.size() + " children: " + returnedBytes);
			System.out.print("names in list:");
			for(ContentName n: returnedBytes)
				System.out.print(" "+n);
			System.out.println();
			
			assertTrue(returnedBytes.size() >= 3);
			Iterator<ContentName> it = returnedBytes.iterator();
			
			ArrayList<Link> newMembers = new ArrayList<Link>();
			System.out.println("member to add:" + UserConfiguration.defaultUserNamespace());
			newMembers.add(new Link(UserConfiguration.defaultUserNamespace()));
			
			for(int i = 0; i <2; i++){
				ContentName name = it.next();
				String fullname = _userList.getName().toString() + name.toString();
				if (!fullname.equals(UserConfiguration.defaultUserNamespace())){
					System.out.println("member to add:" + fullname);
					newMembers.add(new Link(ContentName.fromNative(fullname)));
				}
			}
			System.out.println("creating a group...");
			Random random = new Random();
			String randomGroupName = "testGroup" + random.nextInt();
			Group newGroup = _gm.createGroup(randomGroupName, newMembers);
			
			Assert.assertTrue(_gm.haveKnownGroupMemberships());
			Assert.assertTrue(_gm.amCurrentGroupMember(newGroup));
			Assert.assertTrue(_gm.amCurrentGroupMember(randomGroupName));

			ContentName name = it.next();
			String fullname = _userList.getName().toString() + name.toString();
			System.out.println("adding member to group:"+ fullname);
			ArrayList<Link> addMembers = new ArrayList<Link>();
			addMembers.add(new Link(ContentName.fromNative(fullname)));
			
			boolean succeed = false;
			int retries = 0;
			while(!succeed){
					retries ++;
					System.out.print("................trying to add user...........");
					try{
						newGroup.addUsers(addMembers);
						succeed = true;
					}catch(AccessDeniedException e){
						succeed = false;
					}
			}
			System.out.println(".....................added user, number of retries:..............." + retries);
			
			
			ArrayList<Link> removeMembers = new ArrayList<Link>();
			System.out.println("removing user:.................." + newMembers.get(2).targetName());
			removeMembers.add(newMembers.get(2));
			testRemoveUsers(removeMembers, newGroup);
			
			Assert.assertTrue(_gm.haveKnownGroupMemberships());
			Assert.assertTrue(_gm.amCurrentGroupMember(newGroup));
			Assert.assertTrue(_gm.amCurrentGroupMember(randomGroupName));
			
			removeMembers.clear();
			//try removing myself
			removeMembers.add(new Link(UserConfiguration.defaultUserNamespace()));
			System.out.println("removing myself:.................." );
			testRemoveUsers(removeMembers, newGroup);
			
			//Assert.assertFalse(_gm.haveKnownGroupMemberships());
			
			//isGroup sometimes fails, there's a timing issue. 
			Assert.assertTrue(_gm.isGroup(randomGroupName));
			ContentName pkName = newGroup.publicKeyName();
			System.out.println("new group's pk name is " + pkName);
			Assert.assertTrue(_gm.isGroup(pkName));

			//test groups of group
			String randomParentGroupName = "parentGroup" + random.nextInt();
			newMembers.clear();
			ContentName parentGroupNamespace = AccessControlProfile.groupName(testStorePrefix, randomGroupName);
			System.out.println("parent group namespace = " + parentGroupNamespace);
			newMembers.add(new Link(parentGroupNamespace));
			Group newParentGroup = _gm.createGroup(randomParentGroupName, newMembers);
			
//			System.out.println("adding users to parent group.........");
//			newParentGroup.addUsers(addMembers);
			
			//test deletion of group
			_gm.deleteGroup(randomGroupName);			
//			Assert.assertFalse(_gm.isGroup(randomGroupName));
			
			
		} catch (Exception e) {
			System.out.println("Exception : " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}
	
}