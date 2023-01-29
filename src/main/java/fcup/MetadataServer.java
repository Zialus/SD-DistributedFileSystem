package fcup;

import lombok.extern.java.Log;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;

@Log
public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface {

    private static final FileSystemTree fileSystem = new FileSystemTree();

    private final HashMap<String, String> storageServerList = new HashMap<>();

    private int globalMachineCounter = 0;

    private static void exit(final Registry registry, final MetadataServer obj1, final MetadataServer obj2) {
        try {
            registry.unbind("ClientMetadataInterface");
            registry.unbind("StorageMetadataInterface");

            UnicastRemoteObject.unexportObject(obj1, true);
            UnicastRemoteObject.unexportObject(obj2, true);

            log.info("Unbinded and exited.");
        } catch (final Exception e) {
            log.severe("Server exception: " + e.toString());
        }
    }

    public static void main(final String[] args) {

        try {
            LocateRegistry.createRegistry(1099);

            final MetadataServer objClientMetaInterface = new MetadataServer();
            final ClientMetadataInterface stubClientMetaInterface = (ClientMetadataInterface) UnicastRemoteObject.exportObject(objClientMetaInterface, 0);

            final MetadataServer objStorageMetaInterface = new MetadataServer();
            final StorageMetadataInterface stubStorageMetaInterface = (StorageMetadataInterface) UnicastRemoteObject.exportObject(objStorageMetaInterface, 0);

            final Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientMetadataInterface", stubClientMetaInterface);
            registry.bind("StorageMetadataInterface", stubStorageMetaInterface);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry, objClientMetaInterface, objStorageMetaInterface)));

            log.info("MetaData Server ready");

        } catch (final Exception e) {
            log.severe("Server exception: " + e.toString());
        }
    }

    @Override
    public String giveMeAnID() {
        globalMachineCounter++;
        return "Machine" + globalMachineCounter;
    }

    @Override
    public String find(final String path) {
        final PairBoolNode pair = fileSystem.find(path);

        if (pair.bool) {
            return pair.node.myStorageServer;
        } else {
            return "";
        }
    }

    @Override
    public FileType findInfo(final String path) throws RemoteException {
        final PairBoolNode pair = fileSystem.find(path);

        if (pair.bool) {
            if (pair.node.isDirectory) {
                return FileType.DIRECTORY;
            } else {
                return FileType.FILE;
            }
        } else {
            return FileType.NULL;
        }
    }

    @Override
    public String lstat(final String path) throws RemoteException {
        final FileNode dirToBeListed;

        if ("/".equals(path)) {
            dirToBeListed = fileSystem.root;
        } else {
            final PairBoolNode didYouFindIt = fileSystem.find(path);
            dirToBeListed = didYouFindIt.node;
        }

        if (dirToBeListed == null) {
            return "";
        }

        final StringBuilder output = new StringBuilder(".\n..\n");

        dirToBeListed.children.forEach((key, value) -> output.append(key).append('\n'));

        return new String(output);
    }

    @Override
    public void addStorageServer(final String machine, final String topOfTheSubtree) throws RemoteException {
        storageServerList.put(topOfTheSubtree, machine);
        addStorageItem(topOfTheSubtree, machine, true);
        log.info("I added machine " + machine + " which contains the sub-tree " + topOfTheSubtree);
    }

    @Override
    public void delStorageServer(final String topOfTheSubtree) throws RemoteException {
        final String machine = storageServerList.get(topOfTheSubtree);
        storageServerList.remove(topOfTheSubtree);
        delStorageItem(topOfTheSubtree);
        log.info("I removed the machine " + machine + " that contained the sub-tree " + topOfTheSubtree);
    }

    @Override
    public void addStorageItem(final String item, final String serverName, final boolean isDirectory) throws RemoteException {

        if ("/".equals(item)) {
            fileSystem.root.myStorageServer = serverName;
        } else {
            String[] pathElements = item.split("/");
            // Copy without first element
            pathElements = Arrays.copyOfRange(pathElements, 1, pathElements.length);

            final int lastElement = pathElements.length - 1;

            if (pathElements.length == 1) {
                fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
            } else {

                final int lastSplit = item.lastIndexOf('/');
                final String parentPath = item.substring(0, lastSplit);

                final PairBoolNode maybeFoundParentNode = fileSystem.find(parentPath);

                final boolean didYouFindIt = maybeFoundParentNode.bool;
                if (didYouFindIt) {
                    final FileNode parentNode = maybeFoundParentNode.node;
                    fileSystem.addToFileSystem(pathElements[lastElement], parentNode, isDirectory, serverName);
                } else {
                    log.severe("Something went really wrong");
                }
            }
        }
    }

    @Override
    public void delStorageItem(final String item) throws RemoteException {
        fileSystem.removeFromFileSystem(item);
    }

}
