import java.io.File;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface{

    public static int globalMachineCounter = 0;

    public static HashMap<String,String> StorageServerList = new HashMap<>();

    public MetadataServer() {}

    public static void main(String args[]) {

        try {

            MetadataServer obj1 = new MetadataServer();
            ClientMetadataInterface stub1 = (ClientMetadataInterface) UnicastRemoteObject.exportObject(obj1, 0);

            MetadataServer obj2 = new MetadataServer();
            StorageMetadataInterface stub2 = (StorageMetadataInterface) UnicastRemoteObject.exportObject(obj2, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientMetadataInterface", stub1);
            registry.bind("StorageMetadataInterface", stub2);

            System.out.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public String giveMeAnID(){
        globalMachineCounter++;
        return "Machine"+globalMachineCounter;
    }

    public String find(String path) {
        return StorageServerList.get(path);
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

    public boolean add_storage_server(String machine, String top_of_the_subtree)  {

        StorageServerList.put(top_of_the_subtree, machine);
        return true;
    }

    public boolean del_storage_server(String top_of_the_subtree)  {
        return false;
    }

    public boolean add_storage_item(byte[] item) throws RemoteException {
        return false;
    }

    public boolean del_storage_item(byte[] item) throws RemoteException {
        return false;
    }
}