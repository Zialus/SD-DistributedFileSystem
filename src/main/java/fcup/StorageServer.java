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

    private static void exit(final Registry registry, final StorageServer objStorageServer) {

        close();
        removeMetadataOfDirectory("");

        try {
            // Unregister and Un-export the Storage Server
            registry.unbind(serverName);
            UnicastRemoteObject.unexportObject(objStorageServer, true);

            log.info("Unbound and Un-exported.");
        } catch (final Exception e) {
            log.severe("Exit messed up: " + e.toString());
        }
    }

    public static void main(final String[] args) {

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
            final StorageServer objStorageServer = new StorageServer();
            final ClientStorageInterface stubClientStorage = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            final Registry registry = LocateRegistry.getRegistry(metaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            serverName = stubStorageMetadata.giveMeAnID();
            registry.bind(serverName, stubClientStorage);

            // Initialize the storage server by adding its directories to the MetaDataServer
            log.info("LOCALPATH = " + localPathAtStartup + " GLOBALPATH = " + globalPath);

            init(localPathAtStartup, globalPath);

            // Call exit method when Storage Server shuts down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry, objStorageServer)));

            log.info(serverName + " is ready");
        } catch (final Exception e) {
            log.severe("Client exception: " + e.toString());
            System.exit(1);
        }


    }

    private static void init(final String localPath, final String globalPath) {
        try {
            StorageServer.localPath = localPath;
            stubStorageMetadata.addStorageServer(serverName, globalPath);
            if ("/".equals(globalPath)) {
                stubStorageMetadata.addStorageItem("/", serverName, true);
            }
            sendMetaDataOfDirectory("");
        } catch (final RemoteException e) {
            log.severe("Client exception: " + e.toString());
        }
    }

    private static void close() {
        try {
            stubStorageMetadata.delStorageServer(globalPath);
        } catch (final RemoteException e) {
            log.severe("Close messed up: " + e.toString());
        }
    }

    private static void sendMetaDataOfDirectory(final String path) {
        String globalPathAux = globalPath;
        final File myLocalPath = new File(localPath + '/' + path);

        log.info("Sending to metaData the local path " + myLocalPath.getPath());

        final File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)) {
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (final File f : listOfFiles) {
                final String adjustedFilePath;
                if (path.isEmpty()) {
                    adjustedFilePath = globalPathAux + '/' + f.getName();
                } else {
                    adjustedFilePath = globalPathAux + path + '/' + f.getName();
                }

                final boolean isDirectory = f.isDirectory();
                try {
                    stubStorageMetadata.addStorageItem(adjustedFilePath, serverName, isDirectory);
                } catch (final RemoteException e) {
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

    private static void removeMetadataOfDirectory(final String path) {
        String globalPathAux = globalPath;
        final File myLocalPath = new File(localPath + path);

        log.info("Going to remove metadata of " + myLocalPath.getPath());

        final File[] listOfFiles = myLocalPath.listFiles();

        if ("/".equals(globalPath)) {
            globalPathAux = "";
        }

        if (listOfFiles != null) {
            for (final File f : listOfFiles) {

                final String adjustedFilePath;
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
                } catch (final RemoteException e) {
                    log.severe(e.toString());
                }
            }
        } else {
            log.info("This is just an empty directory " + myLocalPath);
        }

    }

    @Override
    public boolean create(final String globalPath) throws RemoteException {

        final String createdLocalPath = globalToLocal(globalPath);

        final File directory = new File(createdLocalPath);

        log.info("Creating directory: " + directory.toString());

        final boolean mkdir = directory.mkdir();

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
    public boolean create(final String globalPath, final byte[] blob) {

        final int indexLastSlash = globalPath.lastIndexOf('/');
        final int length = globalPath.length();

        final String pathToPutTheFileIn = globalPath.substring(0, indexLastSlash);
        final String fileName = globalPath.substring(indexLastSlash + 1, length);

        final String localPathToPutFileIn = globalToLocal(pathToPutTheFileIn);
        final String fullFinalPath = localPathToPutFileIn + '/' + fileName;

        try {
            Files.write(Paths.get(fullFinalPath), blob);
            log.info("Final Path: " + fullFinalPath + " File received successfully");
            stubStorageMetadata.addStorageItem(globalPath, serverName, false);
            return true;
        } catch (final IOException e) {
            log.severe("Final Path: " + fullFinalPath + " File could not be received " + e.toString());
            return false;
        }

    }

    @Override
    public boolean del(final String pathInGlobalServer) throws RemoteException {
        final String pathInLocalServer = globalToLocal(pathInGlobalServer);

        boolean bool;
        try {
            Files.delete(Paths.get(pathInLocalServer));
            bool = true;
        } catch (final IOException e) {
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

    private String globalToLocal(final String fullGlobalPath) {
        final int indexEndGlobal = fullGlobalPath.indexOf(globalPath) + globalPath.length();

        final String relevantPartOfTheString = fullGlobalPath.substring(indexEndGlobal);

        final String output = localPath + relevantPartOfTheString;
        return output;
    }

    @Override
    public byte[] get(final String pathInGlobalServer) throws IOException {
        final String pathInLocalServer = globalToLocal(pathInGlobalServer);

        log.info("pathinloco" + pathInLocalServer);

        final Path fileToSend = Paths.get(pathInLocalServer);

        return Files.readAllBytes(fileToSend);
    }

}
