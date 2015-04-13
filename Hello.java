package proj;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Hello extends Remote {
    int readObject(String a) throws RemoteException;
    int writeObject(String a, int b) throws RemoteException;
    int issueWrite(String a, int b, int siteNumber) throws Exception;
    void undo(String a, int b) throws RemoteException;
    

    void addRequest(String item, int siteNumber, String op, int val, int id)throws Exception;

    void  addNextOperation(int id)throws Exception;
    void  addOperation(String item, int siteNumber, String op, int val, int id) throws Exception;

    void addFirst(String item, int siteNumber, String op, int val, int id) throws Exception;
}