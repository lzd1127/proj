package proj;




class Request{

	String item;
	int siteNumber;
	String op;
	int val;
	int id;

	Request(String op, int siteNumber, int id) {
		this.op = op;
		this.siteNumber = siteNumber;
		this.id = id;
	}

	Request(String item, int siteNumber, String op, int val, int id) {
		this.item = item;
		this.siteNumber = siteNumber;
		this.op = op;
		this.val = val;
		this.id = id;
	}

	Request(String item, int siteNumber, String op, int id) {
		this.op = op;
		this.siteNumber = siteNumber;
		this.op = op;
		this.id = id;
	}


}