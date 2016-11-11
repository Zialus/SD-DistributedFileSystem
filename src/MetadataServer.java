
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;

public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface{

    public static FileSystemTree fileSystem = new FileSystemTree();

    public int globalMachineCounter = 0;

    public HashMap<String,String> StorageServerList = new HashMap<>();

    public MetadataServer() {}

    public static void exit(Registry registry, MetadataServer obj1, MetadataServer obj2) {
        try{
            // Unregister ourself
            registry.unbind("ClientMetadataInterface");
            registry.unbind("StorageMetadataInterface");

            // Unexport; this will also remove us from the RMI runtime
            UnicastRemoteObject.unexportObject(obj1, true);
            UnicastRemoteObject.unexportObject(obj2, true);

            System.out.println("Unbinded and exited.");
        }
        catch(Exception e){
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

            Runtime.getRuntime().addShutdownHook(new Thread(() -> exit(registry,objClientMetaInterface,objStorageMetaInterface)));

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
        PairBoolNode pair = fileSystem.find(path);

        if (pair.bool) {
                return pair.node.myStorageServer;
        }
        else{
            return "";
        }
    }


    public FileType findInfo(String path) throws RemoteException {
        PairBoolNode pair = fileSystem.find(path);

        if (pair.bool) {
            if (pair.node.isDirectory) {
                return FileType.DIRECTORY;
            } else {
                return FileType.FILE;
            }
        }
        else{
            return FileType.NULL;
        }
    }

    public String lstat(String path) throws RemoteException {
        StringBuilder output = new StringBuilder(".\n..\n");

        FileNode dirToBeListed;

        if (path.equals("/") ) {
            dirToBeListed = fileSystem.root;
        } else {
            PairBoolNode didYouFindIt = fileSystem.find(path);
            dirToBeListed = didYouFindIt.node;
        }

        if (dirToBeListed == null){
            return "";
        }

        dirToBeListed.children.entrySet().forEach(entry -> output.append(entry.getKey()).append("\n"));

        return new String(output);
    }

    public boolean add_storage_server(String machine, String top_of_the_subtree) throws RemoteException {

        StorageServerList.put(top_of_the_subtree, machine);
        add_storage_item(top_of_the_subtree,machine,true);
        System.out.println("I added machine " + machine + " on the sub-tree " + top_of_the_subtree);
        return true;
    }

    public boolean del_storage_server(String top_of_the_subtree) throws RemoteException {
        String machine = StorageServerList.get(top_of_the_subtree);
        StorageServerList.remove(top_of_the_subtree);
        del_storage_item(top_of_the_subtree);
        System.out.println("I added machine " + machine + " on the sub-tree " + top_of_the_subtree);
        return true;
    }


    public boolean add_storage_item(String item, String serverName, boolean isDirectory) throws RemoteException {

        if (item.equals("/")){
            fileSystem.root.myStorageServer = serverName;
            return true;
        } else {

            String[] pathElements = item.split("/");
            // copy without first element
            pathElements = Arrays.copyOfRange(pathElements, 1, pathElements.length);

//        System.out.println("-----------------------");
//        for (String path: pathElements) {
//            System.out.println("->> " + path);
//        }
//        System.out.println("-----------------------");

            int lastElement = pathElements.length - 1;

            if (pathElements.length == 1) {
                fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
            } else {

                int lastSplit;

                lastSplit = item.lastIndexOf("/");

                String parentPath = item.substring(0, lastSplit);

                PairBoolNode maybeFoundParentNode = fileSystem.find(parentPath);

                boolean didYouFindIt = maybeFoundParentNode.bool;
                if (didYouFindIt) {
                    FileNode parentNode = maybeFoundParentNode.node;

                    fileSystem.addToFileSystem(pathElements[lastElement], parentNode, isDirectory, serverName);
                } else {
                    System.out.println("get fucked m8");
                    //fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
                }
            }


            return false;
        }
    }

    public boolean del_storage_item(String item) throws RemoteException {

        fileSystem.removeFromFileSystem(item);

        return false;

    }

}