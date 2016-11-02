import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class StorageServer implements ClientStorageInterface {

    public static String localPath;
    public static String globalPath;
    public static String ServerName;

    public static String MetaDataHostName;
    public static StorageMetadataInterface stubStorageMetadata;

    public static void main(String[] args) {

        // Call close method when Storage Server shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(StorageServer::close));

        Scanner in = new Scanner(System.in);
        System.out.print("Insert Local Path: ");
        String localPathAtStartup = in.next();
        System.out.print("Insert Global Path: ");
        globalPath = in.next();
        in.nextLine();

        System.out.print("Insert MetadataServer hostname(if left blank localhost will be used) : ");
        String MetaDataHostNameTEMP = in.nextLine();
        MetaDataHostName = (MetaDataHostNameTEMP.equals("")) ? "localhost" : MetaDataHostNameTEMP;

        try {
            StorageServer objStorageServer = new StorageServer();
            ClientStorageInterface stubClientStorage = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            Registry registry = LocateRegistry.getRegistry(MetaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            ServerName = stubStorageMetadata.giveMeAnID();
            registry.bind(ServerName, stubClientStorage);

            // Initialize the storage server by adding its directories to the MetaDataServer
            init(localPathAtStartup, globalPath);

            System.out.println(ServerName + " is ready");
            System.out.println("My path is " + localPath);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }


    }


    public static void init(String local_path, String globalPath){
        try {
            localPath = local_path;
            Boolean response = stubStorageMetadata.add_storage_server(ServerName, globalPath);
            System.out.println("Init Response: " + response);

            sendMyMetaData("");

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void close(){
        try{
            Boolean response = stubStorageMetadata.del_storage_server(localPath);
            System.out.println("Close Response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean create(String path) throws RemoteException {
        File directory = new File(path);

        // If the directory does not exist, create it
        System.out.println("creating directory: " + directory.toString());

        boolean result = false;

        try{
            result = directory.mkdir();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        if(result) {
            System.out.println("DIR created");
        }
        return result;
    }

    public static boolean sendMyMetaData(String path){

        File myLocalPath = new File(localPath + "/"+ path);
        File[] listOfFiles = myLocalPath.listFiles();

        for (File f : listOfFiles) {
            try {
                String adjustedFilePath = globalPath + "/" + path + "/" + f.getName();
                boolean isDirectory = f.isDirectory();
                stubStorageMetadata.add_storage_item(adjustedFilePath, ServerName, isDirectory);
                System.out.println(adjustedFilePath);
            } catch (Exception e) {
                System.err.println("Exception: " + e.toString());
                e.printStackTrace();
            }
        }


        return true;
    }

    public boolean create(String path, byte[] blob) throws IOException {
        return false;
    }

    public boolean del(String path) throws RemoteException {
        return false;
    }

    public byte[] get(String path) throws RemoteException {
        return new byte[0];
    }
}
