package test.ccn.library;

import org.junit.Test;

public class BasePutGetTest extends BaseLibrary {
	
	@Test
	public void testGetPut() throws Throwable {
		System.out.println("TEST: PutThread/GetThread");
		int id = rand.nextInt(1000);
		Thread putter = new Thread(new PutThread(count, id));
		Thread getter = new Thread(new GetThread(count, id));
		genericGetPut(putter, getter);
	}
	
	@Test
	public void testGetServPut() throws Throwable {
		System.out.println("TEST: PutThread/GetServer");
		int id = rand.nextInt(1000);

		//Library.logger().setLevel(Level.FINEST);
		Thread putter = new Thread(new PutThread(count, id));
		Thread getter = new Thread(new GetServer(count, id));
		genericGetPut(putter, getter);
	}

	@Test
	public void testGetPutServ() throws Throwable {
		System.out.println("TEST: PutServer/GetThread");
		int id = rand.nextInt(1000);
		Thread putter = new Thread(new PutServer(count, id));
		Thread getter = new Thread(new GetThread(count, id));
		genericGetPut(putter, getter);
	}
}
