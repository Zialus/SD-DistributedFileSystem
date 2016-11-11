package FCUP;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface StorageMetadataInterface extends Remote {

    void add_storage_server(String host, String top_of_the_subtree) throws RemoteException;

    void del_storage_server(String top_of_the_subtree) throws RemoteException;

    void add_storage_item(String item, String serverName, boolean isDirectory) throws RemoteException;

    void del_storage_item(String item) throws RemoteException;

    String giveMeAnID() throws RemoteException ;
}
