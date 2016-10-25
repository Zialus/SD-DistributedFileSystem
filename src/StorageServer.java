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


    public StorageServer(){}

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(StorageServer::close));

        try {
            StorageServer objStorageServer = new StorageServer();
            ClientStorageInterface stub = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            Registry registry = LocateRegistry.getRegistry(MetaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            ServerName = stubStorageMetadata.giveMeAnID();
            registry.bind(ServerName, stub);


            Scanner in = new Scanner(System.in);
            System.out.print("Insert Local Path: ");
            String localPathAtStartup = in.next();
            System.out.print("Insert Global Path: ");
            globalPath = in.next();
            in.nextLine();

            System.out.print("Insert Meadata Server hostname(if left blank localhost will be used) : ");
            String MetaDataHostNameTEMP = in.nextLine();
            MetaDataHostName = (MetaDataHostNameTEMP.equals("")) ? "localhost" : MetaDataHostNameTEMP;

            init(localPathAtStartup, globalPath);

            Boolean response = stubStorageMetadata.add_storage_server(ServerName, globalPath);
            System.out.println("response: " + response);


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
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void close(){
        try{
            Boolean response = stubStorageMetadata.del_storage_server(localPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean create(String path) throws RemoteException {
        File directory = new File(path);

        // if the directory does not exist, create it
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
