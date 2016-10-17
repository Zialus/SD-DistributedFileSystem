import java.io.File;
import java.nio.file.Path;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface{

    public MetadataServer() {}


    public static void main(String args[]) {

        try {
            MetadataServer obj = new MetadataServer();
            ClientMetadataInterface stub = (ClientMetadataInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientMetadataInterface", stub);

            System.err.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public String find(String path) {
        return null;
    }

    public String lstat(String path) throws RemoteException {

        String output = "";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                output = output + listOfFile.getName() + "\n";
            }
        }
        else{
            output = "No files.\n";
        }

        return output;
    }

    public boolean add_storage_server(String host, String top_of_the_subtree)  {
        return false;
    }

    public boolean del_storage_server(String top_of_the_subtree)  {
        return false;
    }

    @Override
    public boolean add_storage_item(byte[] item) throws RemoteException {
        return false;
    }

    @Override
    public boolean del_storage_item(byte[] item) throws RemoteException {
        return false;
    }
}