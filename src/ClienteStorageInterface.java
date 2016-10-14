import com.sun.org.apache.xpath.internal.operations.Bool;

import java.awt.image.BufferedImage;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Blob;

public interface ClienteStorageInterface extends Remote {

    Boolean create(String path) throws RemoteException; // creates a directory
    Boolean create(String path, Blob blob) throws RemoteException; // creates a file

    Boolean del(String path);
    Boolean get(String path);

}
