package proj;


import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.PrintWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

	
public class Server1 implements Hello {
	Map<String, Integer> map;
	public static final int[] sites ={1079, 1576, 1666}; 
	public static final int  siteLabel = 1;
	List<Request> blockList;
	Map<String, LockStatus> lockTable;
	Map<Integer, List<Request>> log;

	Map<Integer, Deque<Request>> transactions;

	Map<Integer, Set<Integer>> waitGraph;
	Map<String, List<Track>>lastTryLock;


	Map<String, Integer> lastStableValue;

	Map<Integer, Integer> abortingDependency;
	Map<Integer, AbortingStatus> aborting;

	PrintWriter printWriter;
	Object mutex;

    public Server1() throws Exception { 
    	map = new HashMap<String, Integer>();
    	map.put("X", 1);
    	map.put("Y", 2);
    	map.put("Z", 3);

    	lastStableValue = new HashMap<String, Integer>();
    	lastStableValue.put("X", 1);
    	lastStableValue.put("Y", 2);
    	lastStableValue.put("Z", 3);


    	blockList = new ArrayList<Request>();
    	lockTable = new HashMap<String, LockStatus>();
    	lockTable.put("X", new LockStatus());
    	lockTable.put("Y", new LockStatus());
    	lockTable.put("Z", new LockStatus());

    	lastTryLock = new HashMap<String, List<Track>>();
    	lastTryLock.put("X", new ArrayList<Track>());
    	lastTryLock.put("Y", new ArrayList<Track>());
    	lastTryLock.put("Z", new ArrayList<Track>());

    	log = new HashMap<Integer, List<Request>>();
    	transactions = new HashMap<Integer, Deque<Request>>();

    	waitGraph = new HashMap<Integer, Set<Integer>>();
    	abortingDependency = new HashMap<Integer, Integer>();
    	aborting = new HashMap<Integer, AbortingStatus>();

    	printWriter = new PrintWriter(new File("log.txt"));
    	mutex = new Object();

    }

    public void closeLog() throws Exception {
    	printWriter.close();
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


    public void addNextOperation(int id) throws Exception{
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

    public  void addOperation(String item, int siteNumber, String op, int val, int id)throws Exception {
    	if (op.equals("S")) {
    		List<Request> list = new ArrayList<Request>();
    		Request r = new Request(item, siteNumber, op, val, id);
    		list.add(r);
    		log.put(id, list);
    		waitGraph.put(id, new HashSet<Integer>());

    		printWriter.println("Transaction T" + id + " starts at site " + siteNumber + ", " + getCurrentTime());

    		if (siteNumber != 1) {
    				Registry registry = LocateRegistry.getRegistry(sites[siteNumber - 1]);
    				Hello stub = (Hello) registry.lookup("Server" + siteNumber);
    				stub.addNextOperation(id);
    			} else {
    				addNextOperation(id);
    		}

    	} else if (op.equals("R")) {
    		Request r = new Request(item, siteNumber, op, val, id);
    		int trial = canGrantLock(r);
    		if (trial == 0) {
    			blockList.add(r);
    		} else if (trial == -1) {


    		} else {
    			log.get(id).add(r);

    			if (siteNumber != 1) {
    				Registry registry = LocateRegistry.getRegistry(sites[siteNumber - 1]);
    				Hello stub = (Hello) registry.lookup("Server" + siteNumber);
    				int response = stub.readObject(r.item);
    				printWriter.printf("Transaction T%d Read %s = %d at site %d, %s\n", 
    						r.id, r.item, response, r.siteNumber, getCurrentTime());
    		
    				stub.addNextOperation(id);
    			} else {
    				int response = readObject(r.item);
    				printWriter.printf("Transaction T%d Read %s = %d at site %d, %s\n", 
    						r.id, r.item, response, r.siteNumber, getCurrentTime());
    				addNextOperation(id);
    			}
    			
    		}

    	} else if (op.equals("F")) {

    		System.out.println("Commit Transaction " + id);
    		printWriter.println("Start Commit Transaction T" + id + ", site " + siteNumber + ", "+ getCurrentTime());

    		waitGraph.remove(id);
    		Set<Integer> set = waitGraph.keySet();
    		for (Integer n : set) {
    			Set<Integer> l = waitGraph.get(n);
    			if (l.contains(id)) {
    				l.remove(id);
    			}
    		}


    		List<Request> list = log.get(id);
    		for (Request r : list) {
    			unlock(r);
    		}

    	
    		if (abortingDependency.containsKey(id)) {
    			int abortId = abortingDependency.get(id);
    			abortingDependency.remove(id);
    			AbortingStatus status = aborting.get(abortId);
    			status.count--;
    			if (status.count == 0) {
    				aborting.remove(abortId);
    				System.out.println("Try to restart Transaction " + status.id);
    				printWriter.println("Try to restart Transaction" + status.id + " at site " + status.siteNumber);
    				if (status.siteNumber != 1) {
    					Registry registry = LocateRegistry.getRegistry(sites[status.siteNumber - 1]);
    					Hello stub = (Hello) registry.lookup("Server" + status.siteNumber);
    					stub.addNextOperation(status.id);
    				} else {
    					addNextOperation(status.id);
    				}
    			}
    		}

    		if (siteNumber != 1) {
    				Registry registry = LocateRegistry.getRegistry(sites[siteNumber - 1]);
    				Hello stub = (Hello) registry.lookup("Server" + siteNumber);
    				stub.addNextOperation(id);
    			} else {
    				addNextOperation(id);
    		}


    	} else if (op.equals("W")) {
    		Request r = new Request(item, siteNumber, op, val, id);
    		int trial = canGrantLock(r);
    		if (trial == 0) {
    			blockList.add(r);
    		} else if (trial == -1) {


    		} else {
    			log.get(id).add(r);

    			if (siteNumber != 1) {
    				Registry registry = LocateRegistry.getRegistry(sites[siteNumber - 1]);
    				Hello stub = (Hello) registry.lookup("Server" + siteNumber);
    				int response = stub.writeObject(r.item, val);

    				printWriter.printf("Transaction T%d Write %s to %d at site %d, %s\n", 
    						r.id, r.item, response, r.siteNumber, getCurrentTime());
    				stub.addNextOperation(id);
    			} else {
    				int response = writeObject(r.item, val);
    				printWriter.printf("Transaction T%d Write %s to %d at site %d, %s\n", 
    						r.id, r.item, response, r.siteNumber, getCurrentTime());

    				addNextOperation(id);
    			}
    		}
    	}
    }

    private void writeAll(Request r) throws Exception{
    	lastStableValue.put(r.item, r.val);
    	printWriter.println("Write to all other sites");
    	if (r.siteNumber == 1) {
    		for (int i = 2; i <= sites.length; i++) {
    			Registry registry = LocateRegistry.getRegistry(sites[i - 1]);
    			Hello stub = (Hello) registry.lookup("Server" + i);
    			stub.writeObject(r.item, r.val);
    			printWriter.printf("Transaction T%d Write %s to %d at site %d, %s\n", 
    						r.id, r.item, r.val, i, getCurrentTime());

    		}
    	} else {
    		for (int i = 1; i <= sites.length; i++) {
    			if (i == 1) {
    				writeObject(r.item, r.val);
    			} else if (i != r.siteNumber) {
    				Registry registry = LocateRegistry.getRegistry(sites[i - 1]);
    				Hello stub = (Hello) registry.lookup("Server" + i);
    				stub.writeObject(r.item, r.val);
    			}
    			if (i != r.siteNumber)
    			printWriter.printf("Transaction T%d Write %s to %d at site %d, %s\n", 
    						r.id, r.item, r.val, i, getCurrentTime());
    		}
    	}
    }

    private void unlock(Request r) throws Exception{
    	if (r.op.equals("S") || r.op.equals("F")) return;
    	LockStatus lock =  lockTable.get(r.item);
    	List<String> releaseItems = new ArrayList<String>();
    	List<Request> removeList = new ArrayList<Request>();

    	if (r.op.equals("W")) {
    		writeAll(r);
    	}
    	lock.ids.remove(r);
    	Request tmp = null;
    	if (lock.ids.size() == 0) {
    		lock.locked = false;
    		System.out.println("realease lock on " + r.item);
    		printWriter.printf("realease lock on from Transaction T%d on %s, %s\n", r.id, r.item, getCurrentTime());
    		releaseItems.add(r.item);
    		for (Request b : blockList) {
    			if (r.item.equals(b.item)) {
    				removeList.add(b);
    				if (b.op.equals("R")) {
    					for (Request o : blockList) {
    						if (b != o && o.op.equals("R") && o.item.equals(b.item)) {
    							removeList.add(o);
    						}
    					}
    				}
    				break;
    			}
    		}


    	} else if (r.op.equals("R") && lock.readLock) {
   			for (Request b : blockList) {
   				if (r.item.equals(b.item) && b.op.equals("W") && b.id == lock.ids.get(0).id) {
   					removeList.add(b);
   				}
   			}
    	}

    	for (Request b : removeList) {
    		blockList.remove(b);
    		addOperation(b.item, b.siteNumber, b.op, b.val, b.id);
    	}

    	/*
    	for (String item : releaseItems) {
    		for (Request b : blockList) {
    			if (item.equals(b.item)) {
    				//String item, int siteNumber, String op, int val, int id) {
    				blockList.remove(b);
    				addOperation(b.item, b.siteNumber, b.op, b.val, b.id);
    				//tmp = b;
    				if (b.op.equals("R") &&)
    			}
    		}
    	}
		*/
    }

    private void abortingUnlock(Request r) throws Exception {
    	if (r.op.equals("S") || r.op.equals("F")) return;
    	List<Request> removeList = new ArrayList<Request>();

    	LockStatus lock = lockTable.get(r.item);
    	lock.ids.remove(r);
    	Request tmp = null;
    	if (lock.ids.size() == 0) {
    		lock.locked = false;
    		System.out.println("realease lock on " + r.item);
    		printWriter.printf("realease lock on from Transaction T%d on %s, %s\n", r.id, r.item, getCurrentTime());
    		for (Request b : blockList) {
    			if (r.item.equals(b.item)) {
    				//String item, int siteNumber, String op, int val, int id) {
    				removeList.add(b);
    				if (b.op.equals("R")) {
    					for (Request o : blockList) {
    						if (b != o && o.op.equals("R") && o.item.equals(b.item)) {
    							removeList.add(o);
    						}
    					}
    				}
    				break;
    			}
    		}
    		//if (tmp != null) blockList.remove(tmp);
    	} else if (r.op.equals("R") && lock.readLock) {
   			for (Request b : blockList) {
   				if (r.item.equals(b.item) && b.op.equals("W") && b.id == lock.ids.get(0).id) {
   					removeList.add(b);
   				}
   			}
    	}
    	for (Request b : removeList) {
    		blockList.remove(b);
    		addOperation(b.item, b.siteNumber, b.op, b.val, b.id);
    	}

    }


    private boolean allCompatible(Request r) {
    	for (Request tmp : lockTable.get(r.item).ids) {
    		if (tmp.id != r.id) return false;
    	}
    	return true;
    }
    // 1 means can get lock    0 means cannot get lock, -1 means deadlock
    private int canGrantLock(Request r) throws Exception{
    	List<Track> tracks = lastTryLock.get(r.item);
    	tracks.add(new Track(r.id, r.op));

    	if (!lockTable.get(r.item).locked) {
    		lockTable.get(r.item).ids.add(r);
    		if (r.op.equals("R")) {
    			lockTable.get(r.item).readLock = true;
    		} else {
    			lockTable.get(r.item).readLock = false;
    		}
    		lockTable.get(r.item).locked = true;
    		System.out.println("get lock on " + r.item);
    		printWriter.printf("Transaction T%d get lock on %s, %s\n", r.id, r.item, getCurrentTime());
    		return 1;
    	} 
    	if (lockTable.get(r.item).readLock && r.op.equals("R")) {
    		lockTable.get(r.item).ids.add(r);
    		System.out.println("get lock on " + r.item);
    		printWriter.printf("Transaction T%d get lock on %s, %s\n", r.id, r.item, getCurrentTime());
    		return 1;
    	}

    	if (lockTable.get(r.item).locked && allCompatible(r)) {
			lockTable.get(r.item).ids.add(r);
			if (lockTable.get(r.item).readLock && r.op.equals("W")) {
				lockTable.get(r.item).readLock = false;
			}
			System.out.println("get lock on " + r.item);
			printWriter.printf("Transaction T%d get lock on %s, %s\n", r.id, r.item, getCurrentTime());
			return 1;
		}

		//construct edge
		printWriter.println("Transaction T" + r.id + " fails to get lock on " + r.item + ", " + getCurrentTime());
		Track tmp = null;
		if (r.op.equals("R")) {
			for (int i = tracks.size() - 1; i >= 0; i--) {
				tmp = tracks.get(i);
				if (tmp.op.equals("W") && r.id != tmp.id) {
					waitGraph.get(r.id).add(tmp.id);
					break;
				} 
			}
		} else {
			for (int i = tracks.size() - 1; i >= 0; i--) {
				tmp = tracks.get(i);
				if (tmp.id != r.id) {
					waitGraph.get(r.id).add(tmp.id);
					break;
				}
			}
		}

		if (detectCycle(tmp.id, r.id)) {
			System.out.println("Cycle detected");
			printWriter.println("Cycle detected" + ", " + getCurrentTime());
			Set<Integer> records = new HashSet<Integer>();
			records.add(tmp.id);

			recordCycle(tmp.id, r.id, records);
			System.out.println("Cycle " + records);
			printWriter.println("Cycle" + records);
			records.remove(r.id);
			System.out.println("Abort Transaction " + r.id);
			printWriter.println("Abort Transaction T" + r.id + ", " + getCurrentTime());

			for (Integer one : records) {
				abortingDependency.put(one, r.id);
			}
			
			aborting.put(r.id, new AbortingStatus(r.siteNumber, records.size(), r.id));

			rollBack(r);
			resetTransaction(r);
			return -1;
		}

    	return 0;
    }


    private boolean recordCycle(int src, int dest, Set<Integer> set) {

    	for (Integer i : waitGraph.get(src)) {
    		set.add(i);
    		if (i == dest) {
    			return true;
    		}
    		if (recordCycle(i, dest, set)) return true;
    		set.remove(i);
    	}
    	return false;
    }

    private void rollBack(Request r) throws Exception {
    	List<Request> list = log.get(r.id);
    	Set<String> touchSet = new HashSet<String>();

    	for (Request old : list) {
    		if (old.op.equals("W")) {
    			touchSet.add(old.item);
    		}
    	}

    	if (r.siteNumber != 1) {
	    	Registry registry = LocateRegistry.getRegistry(sites[r.siteNumber - 1]);
	    	Hello stub = (Hello) registry.lookup("Server" + r.siteNumber);
	    	for (String s : touchSet) {
	    		stub.undo(s, lastStableValue.get(s));
	    		printWriter.printf("rollBack %s to %d at site %d, %s\n", s, 
	    			lastStableValue.get(s), r.siteNumber, getCurrentTime());
	    	}
	    } else {
	    	for (String s : touchSet) {
	    		undo(s, lastStableValue.get(s));
	    		printWriter.printf("rollBack %s to %d at site %d, %s\n", s, 
	    			lastStableValue.get(s), r.siteNumber, getCurrentTime());
	    	}
	    }

    }


    private void resetTransaction(Request r) throws Exception {

    	//remove wait for Graph Edges
    	waitGraph.remove(r.id);
    	Set<Integer> set = waitGraph.keySet();
    	for (Integer n : set) {
    		Set<Integer> l = waitGraph.get(n);
    		if (l.contains(r.id)) {
    			l.remove(r.id);
    		}
    	}
    	waitGraph.put(r.id, new HashSet<Integer>());

    	for (Request re : blockList) {
    		if (re.id == r.id) {
    			blockList.remove(re);
    		}
    	}
    	List<Request> history = log.get(r.id);


    	for (Request request : history) {
    		abortingUnlock(request);
    	}

    	if (r.siteNumber != 1) {
    		Registry registry = LocateRegistry.getRegistry(sites[r.siteNumber - 1]);
	    	Hello stub = (Hello) registry.lookup("Server" + r.siteNumber);
	    	while (history.size() != 0) {
	    		Request old = history.get(history.size() - 1);
	    		stub.addFirst(old.item, old.siteNumber,  old.op,  old.val,  old.id);
	    		history.remove(history.size() - 1);
	    	}
    	} else {
    		while (history.size() != 0) {
	    		Request old = history.get(history.size() - 1);
	    		addFirst(old.item, old.siteNumber,  old.op,  old.val,  old.id);
	    		history.remove(history.size() - 1);
	    	}
    	}
    }

    public void addFirst(String item, int siteNumber, String op, int val, int id) throws Exception {
    	Deque<Request> queue = transactions.get(id);
    	Request r = new Request(item, siteNumber, op, val, id);
    	queue.offerFirst(r);
    }


    private boolean detectCycle(int src, int dest) {
    	for (Integer i : waitGraph.get(src)) {
    		if (i == dest) return true;
    		if (detectCycle(i, dest)) return true;
    	}
    	return false;
    }

    private String getCurrentTime() {
    	String timeStamp = new SimpleDateFormat("HH:mm:ss:SS").format(new Date());
    	return timeStamp;
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
	
    public void printCurrentStatus(int num) throws Exception {
    	printWriter.println("------------------------------");
    	printWriter.println("Test Case #" + num);
    	for (String s : map.keySet()) {
    		printWriter.print(s + " = " + map.get(s) + " ");
    	}
    	printWriter.println();
    }

    public static void main(String args[]) {
	
	try {
	    Server1 obj = new Server1();
	    //Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 0);

	    // Bind the remote object's stub in the registry
	    //Registry registry = LocateRegistry.getRegistry();

	    Hello stub = (Hello) UnicastRemoteObject.exportObject(obj, 1079);
	    Registry registry = LocateRegistry.createRegistry(1079);
	    registry.bind("Server1", stub);

	    System.err.println("Server1 ready");
	} catch (Exception e) {
	    System.err.println("Server exception: " + e.toString());
	    e.printStackTrace();
	}
    }
}