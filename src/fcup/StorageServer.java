package fcup;

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

        try{
            // Unregister and Un-export the Storage Server
            registry.unbind(ServerName);
            UnicastRemoteObject.unexportObject(objStorageServer, true);

            System.out.println("Unbound and Un-exported.");
        }
        catch(Exception e){
            System.err.println("Exit messed up: " + e.toString());
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
            System.out.println("LOCALPATH = " + localPathAtStartup + " GLOBALPATH = " + globalPath);

            init(localPathAtStartup, globalPath);

            // Call exit method when Storage Server shuts down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry,objStorageServer)));

            System.out.println(ServerName + " is ready");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }


    }


    private static void init(String local_path, String globalPath){
        try {
            localPath = local_path;
            stubStorageMetadata.addStorageServer(ServerName, globalPath);
            stubStorageMetadata.addStorageItem("/",ServerName,true);
            sendMetaDataOfDirectory("");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void close(){
        try {
            stubStorageMetadata.delStorageServer(globalPath);
        } catch (RemoteException e) {
            System.out.println("Close messed up: " + e.toString());
            e.printStackTrace();
        }
    }

    private static void sendMetaDataOfDirectory(String path){
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + path);

        System.out.println("Sending to metaData the local path " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)){
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {
                String adjustedFilePath;
                if("".equals(path)) {
                    adjustedFilePath = globalPathAux + "/" + f.getName();
                }
                else {
                    adjustedFilePath = globalPathAux + path + "/" + f.getName();
                }

                boolean isDirectory = f.isDirectory();
                try {
                    stubStorageMetadata.addStorageItem(adjustedFilePath, ServerName, isDirectory);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                if(f.isDirectory()){
                    System.out.println("Calling sendMetada() with ajustedFilePath ->> " + adjustedFilePath);
                    sendMetaDataOfDirectory(adjustedFilePath);
                }

            }
        }
        else {
            System.out.println("Directory " + myLocalPath + " is empty");
        }
    }

    public boolean create(String globalPath) throws RemoteException {

        String localPath = globalToLocal(globalPath);

        File directory = new File(localPath);

        System.out.println("Creating directory: " + directory.toString());

        boolean mkdir = directory.mkdir();

        if (mkdir){
            System.out.println("Directory" +  directory.toString() + "created successfully");
            stubStorageMetadata.addStorageItem(globalPath, ServerName, true);
            return true;
        } else {
            System.out.println("Directory" +  directory.toString() + "could not be created");
            return false;
        }
    }

    public boolean create(String globalPath, byte[] blob) {

        int indexLastSlash = globalPath.lastIndexOf("/");
        int length = globalPath.length();

        String pathToPutTheFileIn = globalPath.substring(0, indexLastSlash);
        String fileName = globalPath.substring(indexLastSlash+1,length);

        String localPathToPutFileIn = globalToLocal(pathToPutTheFileIn);
        String fullFinalPath = localPathToPutFileIn + "/" + fileName;

        try {
            Files.write(Paths.get(fullFinalPath), blob);
            System.out.println("Final Path: " + fullFinalPath + " File received successfully");
            stubStorageMetadata.addStorageItem(globalPath, ServerName, false);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Final Path: " + fullFinalPath + " File could not be received");
            return false;
        }

    }

    public boolean del(String pathInGlobalServer) throws RemoteException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);

        File fileToBeDeleted = new File(pathInLocalServer);

        boolean bool = fileToBeDeleted.delete();

        if (bool) {
            stubStorageMetadata.delStorageItem(pathInGlobalServer);
            return true;
        } else {
            return false;
        }

    }

    private static void removeMetadataOfDirectory(String path){
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + path);

        System.out.println("Going to remove metadata of " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)){
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {

                String adjustedFilePath;
                if("".equals(path)) {
                    adjustedFilePath = globalPathAux + "/" + f.getName();
                }
                else {
                    adjustedFilePath = globalPathAux + path + "/" + f.getName();
                }

                if(f.isDirectory()){
                    removeMetadataOfDirectory(adjustedFilePath);
                }

                try {
                    stubStorageMetadata.delStorageItem(adjustedFilePath);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            System.out.println("This is just an empty directory " + myLocalPath);
        }

    }

    private String globalToLocal(String fullGlobalPath){
        int indexEndGlobal = fullGlobalPath.indexOf(globalPath);

        String relevantPartOfTheString = fullGlobalPath.substring(indexEndGlobal,fullGlobalPath.length());

        String output = localPath + relevantPartOfTheString;
        return output;
    }

    public byte[] get(String pathInGlobalServer) throws IOException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);

        Path fileToSend = Paths.get(pathInLocalServer);

        return Files.readAllBytes(fileToSend);
    }


}
