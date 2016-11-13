package fcup;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface StorageMetadataInterface extends Remote {

    void addStorageServer(String host, String top_of_the_subtree) throws RemoteException;

    void delStorageServer(String top_of_the_subtree) throws RemoteException;

    void addStorageItem(String item, String serverName, boolean isDirectory) throws RemoteException;

    void delStorageItem(String item) throws RemoteException;

    String giveMeAnID() throws RemoteException ;
}
