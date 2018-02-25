package fcup;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;

public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface {

    private static final FileSystemTree fileSystem = new FileSystemTree();

    private final HashMap<String, String> StorageServerList = new HashMap<>();

    private int globalMachineCounter = 0;

    private static void exit(Registry registry, MetadataServer obj1, MetadataServer obj2) {
        try {
            registry.unbind("ClientMetadataInterface");
            registry.unbind("StorageMetadataInterface");

            UnicastRemoteObject.unexportObject(obj1, true);
            UnicastRemoteObject.unexportObject(obj2, true);

            System.out.println("Unbinded and exited.");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            MetadataServer objClientMetaInterface = new MetadataServer();
            ClientMetadataInterface stubClientMetaInterface = (ClientMetadataInterface) UnicastRemoteObject.exportObject(objClientMetaInterface, 0);

            MetadataServer objStorageMetaInterface = new MetadataServer();
            StorageMetadataInterface stubStorageMetaInterface = (StorageMetadataInterface) UnicastRemoteObject.exportObject(objStorageMetaInterface, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientMetadataInterface", stubClientMetaInterface);
            registry.bind("StorageMetadataInterface", stubStorageMetaInterface);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry, objClientMetaInterface, objStorageMetaInterface)));

            System.out.println("MetaData Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public String giveMeAnID() {
        globalMachineCounter++;
        return "Machine" + globalMachineCounter;
    }

    @Override
    public String find(String path) {
        PairBoolNode pair = fileSystem.find(path);

        if (pair.bool) {
            return pair.node.myStorageServer;
        } else {
            return "";
        }
    }

    @Override
    public FileType findInfo(String path) throws RemoteException {
        PairBoolNode pair = fileSystem.find(path);

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
    public String lstat(String path) throws RemoteException {
        FileNode dirToBeListed;

        if ("/".equals(path)) {
            dirToBeListed = fileSystem.root;
        } else {
            PairBoolNode didYouFindIt = fileSystem.find(path);
            dirToBeListed = didYouFindIt.node;
        }

        if (dirToBeListed == null) {
            return "";
        }

        StringBuilder output = new StringBuilder(".\n..\n");

        dirToBeListed.children.forEach((key, value) -> output.append(key).append('\n'));

        return new String(output);
    }

    @Override
    public void addStorageServer(String machine, String top_of_the_subtree) throws RemoteException {
        StorageServerList.put(top_of_the_subtree, machine);
        addStorageItem(top_of_the_subtree, machine, true);
        System.out.println("I added machine " + machine + " which contains the sub-tree " + top_of_the_subtree);
    }

    @Override
    public void delStorageServer(String top_of_the_subtree) throws RemoteException {
        String machine = StorageServerList.get(top_of_the_subtree);
        StorageServerList.remove(top_of_the_subtree);
        delStorageItem(top_of_the_subtree);
        System.out.println("I removed the machine " + machine + " that contained the sub-tree " + top_of_the_subtree);
    }

    @Override
    public void addStorageItem(String item, String serverName, boolean isDirectory) throws RemoteException {

        if ("/".equals(item)) {
            fileSystem.root.myStorageServer = serverName;
        } else {
            String[] pathElements = item.split("/");
            // Copy without first element
            pathElements = Arrays.copyOfRange(pathElements, 1, pathElements.length);

            int lastElement = pathElements.length - 1;

            if (pathElements.length == 1) {
                fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
            } else {

                int lastSplit = item.lastIndexOf('/');
                String parentPath = item.substring(0, lastSplit);

                PairBoolNode maybeFoundParentNode = fileSystem.find(parentPath);

                boolean didYouFindIt = maybeFoundParentNode.bool;
                if (didYouFindIt) {
                    FileNode parentNode = maybeFoundParentNode.node;
                    fileSystem.addToFileSystem(pathElements[lastElement], parentNode, isDirectory, serverName);
                } else {
                    System.err.println("Something went really wrong");
                }
            }
        }
    }

    @Override
    public void delStorageItem(String item) throws RemoteException {
        fileSystem.removeFromFileSystem(item);
    }

}
