import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class StorageServer implements ClientStorageInterface {

    public static String localPath;
    public static String globalPath;
    public static String ServerName;

    public static String MetaDataHostName;
    public static StorageMetadataInterface stubStorageMetadata;


    public StorageServer(){}

    public static void main(String[] args) {

        localPath = args[0];
        globalPath = args[1];
        MetaDataHostName = (args.length <= 2) ? "localhost" : args[2];

        try {

            StorageServer objStorageServer = new StorageServer();
            ClientStorageInterface stub = (ClientStorageInterface) UnicastRemoteObject.exportObject(objStorageServer, 0);

            Registry registry = LocateRegistry.getRegistry(MetaDataHostName);
            stubStorageMetadata = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

            ServerName = stubStorageMetadata.giveMeAnID();
            registry.bind(ServerName, stub);

            System.out.println(ServerName + " is ready");
            System.out.println("My path is " + localPath);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }



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

    public void init(String local_path, String globalPath){
        try {
            Boolean response = stubStorageMetadata.add_storage_server(MetaDataHostName, globalPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    public void close(){
        try{
            Boolean response = stubStorageMetadata.del_storage_server(localPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public boolean create(String path) throws RemoteException {
        return false;
    }

    public boolean create(String path, byte[] blob) throws IOException {
        return false;
    }

    public boolean del(String path) throws RemoteException {
        return false;
    }

    public byte[] get(String path) throws RemoteException {
        return new byte[0];
    }
}
