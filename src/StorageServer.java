import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    public static void exit(Registry registry, StorageServer objStorageServer) {
        try{
            // Unregister ourself
            registry.unbind(ServerName);

            // Unexport; this will also remove us from the RMI runtime
            UnicastRemoteObject.unexportObject(objStorageServer, true);

            System.out.println("Unbinded and exited.");
        }
        catch(Exception e){
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        // Call close method when Storage Server shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(StorageServer::close));


        Scanner in = new Scanner(System.in);
        System.out.print("Insert Local Path: ");
        //String localPathAtStartup = in.next();
        String localPathAtStartup = args[0];

        System.out.print("Insert Global Path: ");
        //globalPath = in.next();
        globalPath= args[1];
        //in.nextLine();

        System.out.print("Insert MetadataServer hostname(if left blank localhost will be used) : ");
        //String MetaDataHostNameTEMP = in.nextLine();
        String MetaDataHostNameTEMP = args[2];

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

            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry,objStorageServer)));


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
            System.out.println("LOCALPATH = " + localPath);
            Boolean response = stubStorageMetadata.add_storage_server(ServerName, globalPath);
            System.out.println("Init Response: " + response);

            sendMetaDataOfDirectory("");

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

    public boolean create(String pathGlobal) throws RemoteException {

        String localPath = globalToLocal(pathGlobal);

        File directory = new File(localPath);

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

    public static boolean sendMetaDataOfDirectory(String path){

        File myLocalPath = new File(localPath + "/" + path);

        System.out.println(myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if (globalPath.equals("/")){
            globalPath = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {

                try {
                    String adjustedFilePath;
                    if(path.equals("")) {
                        adjustedFilePath = globalPath + "/" + f.getName();
                    }
                    else {
                        adjustedFilePath = globalPath + "/" + path + "/" + f.getName();
                    }
                    boolean isDirectory = f.isDirectory();

                    stubStorageMetadata.add_storage_item(adjustedFilePath, ServerName, isDirectory);

                } catch (Exception e) {
                    System.err.println("Exception: " + e.toString());
                    e.printStackTrace();
                }

                if(f.isDirectory()){
                    boolean response = sendMetaDataOfDirectory(f.getName());
                }

            }
        }
        else {
            System.out.println("ja foste nulled");
        }

        if (globalPath.equals("")){
            globalPath = "/";
        }

        return true;
    }

    public boolean create(String globalPath, byte[] blob) throws IOException {


        String pathToPutFileIn = globalToLocal(globalPath);

        int indexLastSlash = pathToPutFileIn.lastIndexOf("/");
        int length = pathToPutFileIn.length();
        String fileToBeGotten = pathToPutFileIn.substring(indexLastSlash+1,length);

        Files.write(Paths.get(pathToPutFileIn + fileToBeGotten), blob);

        System.out.println("File received successfully");

        return true;
    }

    public boolean del(String pathInGlobalServer) throws RemoteException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);
        
        File fileToBeDeleted = new File(pathInLocalServer);

        boolean bool = fileToBeDeleted.delete();

        stubStorageMetadata.del_storage_item(pathInGlobalServer);

        return bool;
    }

    public String globalToLocal(String fullGlobalPath){

        System.out.println("globalToLocalDEBUG1 " + fullGlobalPath);
        
        int indexEndGlobal = fullGlobalPath.indexOf(globalPath);
        
        System.out.println("globalToLocalDEBUG2 " + globalPath + " " + indexEndGlobal);
        
        String relevantPartOfTheString = fullGlobalPath.substring(indexEndGlobal,fullGlobalPath.length());
        
        System.out.println("globalToLocalDEBUG3 " + relevantPartOfTheString);
        
        String output = localPath + relevantPartOfTheString;

        System.out.println("globalToLocalDEBUG4" + localPath + relevantPartOfTheString);

        return output;
    }

    public byte[] get(String pathInGlobalServer) throws IOException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);
        
        Path fileToSend = Paths.get(pathInLocalServer);

        byte[] bytesToBeSent = Files.readAllBytes(fileToSend);

        return bytesToBeSent;
    }


}
