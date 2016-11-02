
import com.sun.org.apache.regexp.internal.RE;

import java.io.File;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;

public class MetadataServer implements ClientMetadataInterface, StorageMetadataInterface{

    public FileSystemTree fileSystem = new FileSystemTree();

    public int globalMachineCounter = 0;

    public HashMap<String,String> StorageServerList = new HashMap<>();

    public MetadataServer() {}

    public static void main(String[] args) {

        try {

            MetadataServer obj1 = new MetadataServer();
            ClientMetadataInterface stub1 = (ClientMetadataInterface) UnicastRemoteObject.exportObject(obj1, 0);

            MetadataServer obj2 = new MetadataServer();
            StorageMetadataInterface stub2 = (StorageMetadataInterface) UnicastRemoteObject.exportObject(obj2, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ClientMetadataInterface", stub1);
            registry.bind("StorageMetadataInterface", stub2);

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
        Pair pair = fileSystem.find(path);

        return pair.node.name;
    }

    public String lstat(String path) throws RemoteException {

        String output = "";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                output = output + listOfFile.getName() + "\n";
            }
        }
        else{
            output = "No files.\n";
        }

        return output;
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

    public boolean add_storage_server(String machine, String top_of_the_subtree)  {

        StorageServerList.put(top_of_the_subtree, machine);
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

        System.out.println("-----------------------");
        for (String path: pathElements) {
            System.out.println("->> " + path);
        }
        System.out.println("-----------------------");

        int lastElement = pathElements.length-1;

        if (pathElements.length < 2) {
            fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root,isDirectory, serverName);
        } else {

            int lastSplit;

            if (isDirectory) {
                lastSplit = item.lastIndexOf("/");
                lastSplit = item.lastIndexOf("",lastSplit);
            } else {
                lastSplit = item.lastIndexOf("/");
            }


            System.out.println("item = "+ item);
            String parentPath = item.substring(0,lastSplit-1);
            System.out.println("parentPath = " + parentPath);
            Pair maybeFoundParentNode = fileSystem.find(parentPath);

            boolean didYouFindIt = maybeFoundParentNode.bool;
            if(didYouFindIt) {
                FileNode parentNode = maybeFoundParentNode.node;

                fileSystem.addToFileSystem(pathElements[lastElement], parentNode, isDirectory, serverName);
            }
            else{
                fileSystem.addToFileSystem(pathElements[lastElement], fileSystem.root, isDirectory, serverName);
            }
        }


        return false;
    }

    public boolean del_storage_item(String item) throws RemoteException {
        return false;
    }
}