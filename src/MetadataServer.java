
import java.io.BufferedReader;
import java.io.File;
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

        System.out.println("tryimgtofind " + path);
        Pair pair = fileSystem.find(path);

        if(pair.node == null){
            System.out.println("NULLLL CRLH");
        }

        if (pair.bool == true) {
            System.out.println("hhhghgghghhghggh");
            return pair.node.myStorageServer;
        }
        else{
            return "fuck you....";
        }
    }

    public String lstat(String path) throws RemoteException {
        StringBuilder output = new StringBuilder(".\n");

        FileNode dirToBeListed;
        if (path.equals("/") ) {
            dirToBeListed = fileSystem.root;
        } else {
            Pair didYouFindIt = fileSystem.find(path);
            dirToBeListed = didYouFindIt.node;
        }

        dirToBeListed.children.entrySet().forEach(entry -> {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
            output.append( entry.getKey() + "\n");
        });

        return new String(output);
    }


//    public String cd(String dir, String serverPath) throws RemoteException {
//
//        if(dir.equals("..")){
//            String [] fields = serverPath.split("/");
//            serverPath = "";
//            for(int i =0; i < fields.length - 1; i++){
//                if(!fields[i].equals(""))
//                    serverPath = serverPath +"/" + fields[i];
//            }
//        }
//        else{
//            File folder = new File(serverPath);
//            File[] listOfFiles = folder.listFiles();
//
//            for (int i = 0; i < listOfFiles.length; i++) {
//                if(listOfFiles[i].isDirectory()){
//                    if(dir.equals(listOfFiles[i].getName())){
//                        serverPath = serverPath + "/" + dir;
//                        return serverPath;
//                    }
//                }
//            }
//            return "NA";
//        }
//        return serverPath;
//    }

    public boolean add_storage_server(String machine, String top_of_the_subtree) throws RemoteException {

        StorageServerList.put(top_of_the_subtree, machine);

        add_storage_item(top_of_the_subtree,machine,true);

//        String slashFreeDirName = top_of_the_subtree.substring(1); // take out the "/"
//
//        System.out.println("slashSolo " + slashFreeDirName);
//        fileSystem.addToFileSystem(slashFreeDirName, fileSystem.root, true, machine);
        System.out.println("I added machine " + machine + " on the sub-tree " + top_of_the_subtree);
        return true;
    }

    public boolean del_storage_server(String top_of_the_subtree)  {
        return false;
    }


    public boolean add_storage_item(String item, String serverName, boolean isDirectory) throws RemoteException {

        String[] pathElements = item.split("/");
        // copy without first element
        pathElements = Arrays.copyOfRange(pathElements, 1, pathElements.length);

//        System.out.println("-----------------------");
//        for (String path: pathElements) {
//            System.out.println("->> " + path);
//        }
//        System.out.println("-----------------------");

        int lastElement = pathElements.length-1;

        if (pathElements.length == 1) {
            fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root,isDirectory, serverName);
        } else {

            int lastSplit;

//            if (isDirectory) {
//                lastSplit = item.lastIndexOf("/");
//                lastSplit = item.lastIndexOf("/",lastSplit);
//            } else {
                lastSplit = item.lastIndexOf("/");
//            }

            System.out.println("66666666666666666666");
            System.out.println("item = "+ item);
            String parentPath = item.substring(0,lastSplit);
            System.out.println("parentPath = " + parentPath);
            System.out.println("99999999999999999999");
            Pair maybeFoundParentNode = fileSystem.find(parentPath);

            boolean didYouFindIt = maybeFoundParentNode.bool;
            if(didYouFindIt) {
                FileNode parentNode = maybeFoundParentNode.node;

                fileSystem.addToFileSystem(pathElements[lastElement], parentNode, isDirectory, serverName);
            }
            else{
                System.out.println("get fucked m8");
                //fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
            }
        }


        return false;
    }

    public boolean del_storage_item(String item) throws RemoteException {
        return false;
    }
}