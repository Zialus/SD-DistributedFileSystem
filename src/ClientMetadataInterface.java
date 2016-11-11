import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientMetadataInterface extends Remote {
    String find(String path) throws RemoteException;
    FileType findInfo(String path) throws RemoteException;
    String lstat(String path) throws RemoteException;
}