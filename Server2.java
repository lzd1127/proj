package proj;


import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.MalformedURLException;
import java.rmi.Naming;

	
public class Server2 implements Hello {
	Map<String, Integer> map;
	public static final int siteLabel = 2;
	public static final int[] sites ={1079, 1576, 1666}; 
	Map<Integer, Deque<Request>> transactions;


    public Server2() { 
    	map = new HashMap<String, Integer>();
    	map.put("X", 1);
    	map.put("Y", 2);
    	map.put("Z", 3);
    	transactions = new HashMap<Integer, Deque<Request>>();
    }

   public void addRequest(String item, int siteNumber, String op, int val, int id) throws Exception{

    	if (op.equals("S")) {
    		transactions.put(id, new LinkedList<Request>());
    	}
    	Deque<Request> queue = transactions.get(id);
    	queue.offer(new Request(item, siteNumber,  op,  val,  id));

    	if (queue.size() == 1) {

    		if (siteNumber != 1) {
    			Registry registry = LocateRegistry.getRegistry(sites[0]);
    			Hello stub = (Hello) registry.lookup("Server1");
    			stub.addOperation(item, siteNumber, op, val, id);
    		} else {
    			addOperation(item, siteNumber, op, val, id);
    		}
    	}
    }


    public  void addNextOperation(int id) throws Exception{
    	Deque<Request> queue = transactions.get(id);
    	if (queue.isEmpty()) return;

    	queue.poll();
    	if (queue.size() != 0) {
    		Request r = queue.peek();

    		if (r.siteNumber != 1) {
    			Registry registry = LocateRegistry.getRegistry(sites[0]);
    			Hello stub = (Hello) registry.lookup("Server1");
    			stub.addOperation(r.item, r.siteNumber, r.op, r.val, r.id);
    		} else {
    			addOperation(r.item, r.siteNumber, r.op, r.val, r.id);
    		}
    		
    	}
    }

    public  void addOperation(String item, int siteNumber, String op, int val, int id) throws Exception{
    	
    }

    public void addFirst(String item, int siteNumber, String op, int val, int id) throws Exception {
    	Deque<Request> queue = transactions.get(id);
    	Request r = new Request(item, siteNumber, op, val, id);
    	queue.offerFirst(r);
    }

     public void undo(String a, int b) throws RemoteException {
    	map.put(a, b);
    	System.out.println("rollBack " + a +" to " + b);
    }

    public int readObject(String a) throws RemoteException {
    	int val = map.get(a);
    	System.out.println("read " + a +" = " + val);
    	return val;
    }

    public int writeObject(String a, int b) throws RemoteException {
    	int old = map.get(a);
    	System.out.println("Write " + a + "from " + old + " to " + b);
    	map.put(a, b);
    	return b;
    }
	

	public int issueWrite(String a, int b, int siteNumber) throws Exception {
		return -1;
	}
    public static void main(String args[]) {
	
	try {
	    Server2 obj = new Server2();
	    Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 1576);

	    // Bind the remote object's stub in the registry
	   	Registry registry = LocateRegistry.createRegistry(1576);
	    registry.bind("Server2", stub);


	    System.err.println("Server2 ready");
	} catch (Exception e) {
	    System.err.println("Server exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}