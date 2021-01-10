package fcup;

import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Log
public class StorageServer implements ClientStorageInterface {

    private static String localPath;

    private static String globalPath;

    private static String serverName;

    private static StorageMetadataInterface stubStorageMetadata;

    private static void exit(Registry registry, StorageServer objStorageServer) {

        close();
        removeMetadataOfDirectory("");

        try {
            // Unregister and Un-export the Storage Server
            registry.unbind(serverName);
            UnicastRemoteObject.unexportObject(objStorageServer, true);

            log.info("Unbound and Un-exported.");
        } catch (Exception e) {
            log.severe("Exit messed up: " + e.toString());
        }
    }

    public static void main(String[] args) {

        String localPathAtStartup = "";
        String metaDataHostName = "";

        switch (args.length) {
            case 2:
                localPathAtStartup = args[0];
                globalPath = args[1];
                metaDataHostName = "localhost";
                break;
            case 3:
                localPathAtStartup = args[0];
                globalPath = args[1];
                metaDataHostName = args[2];
                break;
            default:
                log.severe("Wrong number of arguments");
                System.exit(1);
                break;
        }

        try {
            StorageServer objStorageServer = new StorageServer();
            ClientStorageInterface stubClientStorage = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            Registry registry = LocateRegistry.getRegistry(metaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            serverName = stubStorageMetadata.giveMeAnID();
            registry.bind(serverName, stubClientStorage);

            // Initialize the storage server by adding its directories to the MetaDataServer
            log.info("LOCALPATH = " + localPathAtStartup + " GLOBALPATH = " + globalPath);

            init(localPathAtStartup, globalPath);

            // Call exit method when Storage Server shuts down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry, objStorageServer)));

            log.info(serverName + " is ready");
        } catch (Exception e) {
            log.severe("Client exception: " + e.toString());
            System.exit(1);
        }


    }

    private static void init(String localPath, String globalPath) {
        try {
            StorageServer.localPath = localPath;
            stubStorageMetadata.addStorageServer(serverName, globalPath);
            if ("/".equals(globalPath)) {
                stubStorageMetadata.addStorageItem("/", serverName, true);
            }
            sendMetaDataOfDirectory("");
        } catch (RemoteException e) {
            log.severe("Client exception: " + e.toString());
        }
    }

    private static void close() {
        try {
            stubStorageMetadata.delStorageServer(globalPath);
        } catch (RemoteException e) {
            log.severe("Close messed up: " + e.toString());
        }
    }

    private static void sendMetaDataOfDirectory(String path) {
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + '/' + path);

        log.info("Sending to metaData the local path " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)) {
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {
                String adjustedFilePath;
                if (path.isEmpty()) {
                    adjustedFilePath = globalPathAux + '/' + f.getName();
                } else {
                    adjustedFilePath = globalPathAux + path + '/' + f.getName();
                }

                boolean isDirectory = f.isDirectory();
                try {
                    stubStorageMetadata.addStorageItem(adjustedFilePath, serverName, isDirectory);
                } catch (RemoteException e) {
                    log.severe(e.toString());
                }

                if (f.isDirectory()) {
                    log.info("Calling sendMetadata() with adjustedFilePath ->> " + f.getName());
                    sendMetaDataOfDirectory(f.getName());
                }

            }
        } else {
            log.info("Directory " + myLocalPath + " is empty");
        }
    }

    private static void removeMetadataOfDirectory(String path) {
        String globalPathAux = globalPath;
        File myLocalPath = new File(localPath + path);

        log.info("Going to remove metadata of " + myLocalPath.getPath());

        File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)) {
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (File f : listOfFiles) {

                String adjustedFilePath;
                if (path.isEmpty()) {
                    adjustedFilePath = globalPathAux + '/' + f.getName();
                } else {
                    adjustedFilePath = globalPathAux + path + '/' + f.getName();
                }

                if (f.isDirectory()) {
                    removeMetadataOfDirectory(adjustedFilePath);
                }

                try {
                    stubStorageMetadata.delStorageItem(adjustedFilePath);
                } catch (RemoteException e) {
                    log.severe(e.toString());
                }
            }
        } else {
            log.info("This is just an empty directory " + myLocalPath);
        }

    }

    @Override
    public boolean create(String globalPath) throws RemoteException {

        String createdLocalPath = globalToLocal(globalPath);

        File directory = new File(createdLocalPath);

        log.info("Creating directory: " + directory.toString());

        boolean mkdir = directory.mkdir();

        if (mkdir) {
            log.info("Directory" + directory.toString() + "created successfully");
            stubStorageMetadata.addStorageItem(globalPath, serverName, true);
            return true;
        } else {
            log.info("Directory" + directory.toString() + "could not be created");
            return false;
        }
    }

    @Override
    public boolean create(String globalPath, byte[] blob) {

        int indexLastSlash = globalPath.lastIndexOf('/');
        int length = globalPath.length();

        String pathToPutTheFileIn = globalPath.substring(0, indexLastSlash);
        String fileName = globalPath.substring(indexLastSlash + 1, length);

        String localPathToPutFileIn = globalToLocal(pathToPutTheFileIn);
        String fullFinalPath = localPathToPutFileIn + '/' + fileName;

        try {
            Files.write(Paths.get(fullFinalPath), blob);
            log.info("Final Path: " + fullFinalPath + " File received successfully");
            stubStorageMetadata.addStorageItem(globalPath, serverName, false);
            return true;
        } catch (IOException e) {
            log.severe("Final Path: " + fullFinalPath + " File could not be received " + e.toString());
            return false;
        }

    }

    @Override
    public boolean del(String pathInGlobalServer) throws RemoteException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);

        boolean bool;
        try {
            Files.delete(Paths.get(pathInLocalServer));
            bool = true;
        } catch (IOException e) {
            log.severe(e.toString());
            bool = false;
        }

        if (bool) {
            stubStorageMetadata.delStorageItem(pathInGlobalServer);
            return true;
        } else {
            return false;
        }

    }

    private String globalToLocal(String fullGlobalPath) {
        int indexEndGlobal = fullGlobalPath.indexOf(globalPath) + globalPath.length();

        String relevantPartOfTheString = fullGlobalPath.substring(indexEndGlobal, fullGlobalPath.length());

        String output = localPath + relevantPartOfTheString;
        return output;
    }

    @Override
    public byte[] get(String pathInGlobalServer) throws IOException {
        String pathInLocalServer = globalToLocal(pathInGlobalServer);

        log.info("pathinloco" + pathInLocalServer);

        Path fileToSend = Paths.get(pathInLocalServer);

        return Files.readAllBytes(fileToSend);
    }

}
