import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class StorageServer implements ClientStorageInterface {

    private static String localPath;
    private static String globalPath;
    private static String ServerName;

    private static StorageMetadataInterface stubStorageMetadata;

    private static void exit(Registry registry, StorageServer objStorageServer) {

        close();
        removeMetadataOfDirectory("");
        try {
            stubStorageMetadata.del_storage_server(globalPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try{
            // Unregister this Storage Server
            registry.unbind(ServerName);

            // Un-export; this will also remove us from the RMI runtime
            UnicastRemoteObject.unexportObject(objStorageServer, true);

            System.out.println("Unbinded and exited.");
        }
        catch(Exception e){
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String localPathAtStartup = args[0];
        globalPath = args[1];
        String metaDataHostName = args[2];

        try {
            StorageServer objStorageServer = new StorageServer();
            ClientStorageInterface stubClientStorage = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            Registry registry = LocateRegistry.getRegistry(metaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            ServerName = stubStorageMetadata.giveMeAnID();
            registry.bind(ServerName, stubClientStorage);

            // Initialize the storage server by adding its directories to the MetaDataServer
            init(localPathAtStartup, globalPath);

            // Call exit method when Storage Server shuts down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry,objStorageServer)));

            System.out.println(ServerName + " is ready");
            System.out.println("My path is " + localPath);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }


    }


    private static void init(String local_path, String globalPath){
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

    private static void close(){
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

    private static boolean sendMetaDataOfDirectory(String path){
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + path);

        System.out.println("ggtfftf " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if (globalPath.equals("/")){
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {


                String adjustedFilePath;
                if(path.equals("")) {
                    adjustedFilePath = globalPathAux + "/" + f.getName();
                }
                else {
                    adjustedFilePath = globalPathAux + path + "/" + f.getName();
                }

                boolean isDirectory = f.isDirectory();
                try {
                    stubStorageMetadata.add_storage_item(adjustedFilePath, ServerName, isDirectory);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if(f.isDirectory()){
                    System.out.println("Vou chamar o sendMetada com o ->> " + adjustedFilePath);
                    sendMetaDataOfDirectory(adjustedFilePath);
                }

            }
        }
        else {
            System.out.println("This is just an empty directory " + myLocalPath);
        }

        return true;
    }

    public boolean create(String globalPath, byte[] blob) throws IOException {

        int indexLastSlash = globalPath.lastIndexOf("/");
        int length = globalPath.length();
        String fileName = globalPath.substring(indexLastSlash+1,length);
        String pathToPutTheFileIn = globalPath.substring(0, indexLastSlash);

        System.out.println("Globalpath no create ->> " + pathToPutTheFileIn);
        String localpathToPutFileIn = globalToLocal(pathToPutTheFileIn);
        System.out.println("pathToPutFileIn no create ->> " + localpathToPutFileIn);

        String finalName = localpathToPutFileIn + "/" + fileName;

        Files.write(Paths.get(finalName), blob);

//        System.out.println("File received successfully");
        System.out.println("FINALNAME " + finalName);
        stubStorageMetadata.add_storage_item(globalPath, ServerName, false);
        return true;
    }



    public boolean del(String pathInGlobalServer) throws RemoteException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);

        File fileToBeDeleted = new File(pathInLocalServer);

        boolean bool = fileToBeDeleted.delete();

        stubStorageMetadata.del_storage_item(pathInGlobalServer);

        return bool;
    }

    private static boolean removeMetadataOfDirectory(String path){
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + path);

        System.out.println("mylocalpath do remove " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if (globalPath.equals("/")){
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {


                String adjustedFilePath;
                if(path.equals("")) {
                    adjustedFilePath = globalPathAux + "/" + f.getName();
                }
                else {
                    adjustedFilePath = globalPathAux + path + "/" + f.getName();
                }

                if(f.isDirectory()){
                    System.out.println("Vou chamar o removeMetadataOfDirectory com o ->> " + adjustedFilePath);
                    removeMetadataOfDirectory(adjustedFilePath);
                }

                try {
                    stubStorageMetadata.del_storage_item(adjustedFilePath);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            System.out.println("This is just an empty directory " + myLocalPath);
        }

        return true;
    }

    private String globalToLocal(String fullGlobalPath){

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

        return Files.readAllBytes(fileToSend);
    }


}
