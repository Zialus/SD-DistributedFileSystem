import java.rmi.Remote;
import java.rmi.RemoteException;


public interface StorageMetadataInterface extends Remote {

    boolean add_storage_server(String host, String top_of_the_subtree) throws RemoteException;

    boolean del_storage_server(String top_of_the_subtree) throws RemoteException;

    boolean add_storage_item(String item, String serverName, boolean isDirectory) throws RemoteException;

    boolean del_storage_item(String item) throws RemoteException;

    String giveMeAnID() throws RemoteException ;
}
