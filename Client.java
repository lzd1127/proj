package proj;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Naming;


public class Client {

    private Client() {}

    //test cycle created by two sites
    public static void deadlockTest(Hello stub, Hello stub2, Hello stub3) throws Exception{
    	System.out.println("Test DeadLock");
    	stub.addRequest("", 1, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);

    	stub.addRequest("X", 1, "R", 0, 1);
    	stub2.addRequest("Y", 2, "R", 0, 2);


    	stub2.addRequest("X", 2, "W", 2, 2);
    	stub.addRequest("Y", 1, "W", 1, 1);


    	stub.addRequest("", 1, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		System.out.println("End");
    }

    //need rollback case
    public static void deadlockTest0(Hello stub, Hello stub2, Hello stub3) throws Exception{
    	System.out.println("Test DeadLock");
    	stub.addRequest("", 1, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);

    	stub.addRequest("X", 1, "W", 0, 1);
    	stub2.addRequest("Y", 2, "W", 0, 2);


    	stub2.addRequest("X", 2, "W", 2, 2);
    	stub.addRequest("Y", 1, "W", 1, 1);


    	stub.addRequest("", 1, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		System.out.println("End");
    }

    //test cycle created by one site
    public static void deadlockTest1(Hello stub, Hello stub2, Hello stub3) throws Exception{
    	System.out.println("Test DeadLock1");
    	stub2.addRequest("", 2, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);

    	stub2.addRequest("X", 2, "R", 0, 1);
    	stub2.addRequest("Y", 2, "R", 0, 2);


    	stub2.addRequest("X", 2, "W", 2, 2);
    	stub2.addRequest("Y", 2, "W", 1, 1);


    	stub2.addRequest("", 2, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		System.out.println("End");
    }


    //test cycle created by two sites and graph is constructed correctly
    //2->1 
    public static void deadlockTest2(Hello stub, Hello stub2, Hello stub3) throws Exception {
    	System.out.println("Test DeadLock2");
    	stub3.addRequest("", 3, "S", 0, 3);
    	stub.addRequest("", 1, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);


    	stub2.addRequest("Y", 2, "R", 0, 2);
    	stub3.addRequest("X", 3, "W", 5, 3);
    	


    	stub.addRequest("X", 1, "R", 0, 1);
    	
    	stub2.addRequest("X", 2, "W", 0, 2);
    	stub3.addRequest("", 3, "F", 0, 3);
    	stub.addRequest("Y", 1, "W", 0, 1);


    	stub.addRequest("", 1, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		System.out.println("End");

    }


    // test  1 -> 2 -> 3 -> 1 cycle case in three sites
    public static void deadlockTest3(Hello stub, Hello stub2, Hello stub3) throws Exception {
    	System.out.println("Test DeadLock2");
    	stub3.addRequest("", 3, "S", 0, 3);
    	stub.addRequest("", 1, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);




    	stub.addRequest("X", 1, "W", 6, 1);
    	stub2.addRequest("Y", 2, "W", 5, 2);
    	stub3.addRequest("Z", 3, "W", 4, 3);
    	


    	stub.addRequest("Y", 1, "R", 0, 1);
    	
    	stub2.addRequest("Z", 2, "R", 0, 2);
    	stub3.addRequest("X", 3, "R", 0, 3);
    	

    	stub.addRequest("", 1, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		stub3.addRequest("", 3, "F", 0, 3);
		System.out.println("End");

    }






    public static void test1(Hello stub, Hello stub2, Hello stub3) throws Exception {
    	System.out.println("Test1");
    	stub.addRequest("", 1, "S", 0, 3);
    	stub2.addRequest("", 2, "S", 0, 1);
    	stub2.addRequest("", 2, "S", 0, 2);

    	stub2.addRequest("X", 2, "R", 0, 1);
    	stub2.addRequest("Y", 2, "R", 0, 2);
    	stub.addRequest("Y", 1, "R", 0, 3);

    	stub2.addRequest("X", 2, "W", 5, 2);

    	stub2.addRequest("", 2, "F", 0, 1);
		stub2.addRequest("", 2, "F", 0, 2);
		stub.addRequest("", 1, "F", 0, 3);
		System.out.println("End");

    }

    public static void main(String[] args) {

	String host = (args.length < 1) ? null : args[0];
	try {
	    Registry registry = LocateRegistry.getRegistry(1079);
	    Hello stub = (Hello) registry.lookup("Server1");

	    Registry registry2 = LocateRegistry.getRegistry(1576);
	    Hello stub2 = (Hello) registry2.lookup("Server2");
	    //String item, int siteNumber, String op, int val, int id)

	    Registry registry3 = LocateRegistry.getRegistry(1666);
	    Hello stub3 = (Hello) registry3.lookup("Server3");



	    //test1(stub, stub2, stub3);
	    
	    //deadlockTest(stub, stub2, stub3);

	    //deadlockTest0(stub, stub2, stub3);
	    //deadlockTest1(stub, stub2, stub3);

	    //deadlockTest2(stub, stub2, stub3);

	    deadlockTest3(stub, stub2, stub3);

	    /*
	    stub.addRequest("", 1, "S", 0, 1);

	    stub2.addRequest("", 2, "S", 0, 2);
	    stub3.addRequest("", 3, "S", 0, 5);

	    stub.addRequest("X", 1, "R", 0, 1);
	    stub.addRequest("Y", 1, "R", 3, 1);
	    stub3.addRequest("Y", 3, "W", 3, 5);

	    stub2.addRequest("X", 2, "R", 2, 2);

	    stub2.addRequest("Y", 2, "R", 0, 2);

	    stub.addRequest("", 1, "F", 0, 1);
	
		stub2.addRequest("", 2, "F", 0, 2);
		stub3.addRequest("", 3, "F", 0, 5);
		System.out.println("End");

	    */

	} catch (Exception e) {
	    System.err.println("Client exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}