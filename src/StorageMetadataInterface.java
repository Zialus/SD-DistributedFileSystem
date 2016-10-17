import java.rmi.Remote;
import java.rmi.RemoteException;


public interface StorageMetadataInterface extends Remote {

    boolean add_storage_server(String host, String top_of_the_subtree) throws RemoteException;

    boolean del_storage_server(String top_of_the_subtree) throws RemoteException;

    boolean add_storage_item(byte[] item) throws RemoteException;

    boolean del_storage_item(byte[] item) throws RemoteException;
}
