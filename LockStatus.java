package proj;

import java.util.List;
import java.util.ArrayList;




class LockStatus {

	public boolean locked;
	public List<Request> ids;
	public boolean readLock;


	LockStatus() {
		locked = false;
		ids = new ArrayList<Request>();
	}

}