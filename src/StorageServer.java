import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class StorageServer implements ClienteStorageInterface {

    public static String myPath;
    public static String host;
    public static StorageMetadataInterface stub;

    public StorageServer() {}

    public static void main(String[] args) {

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            stub = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        myPath = args[0];
        host = (args.length < 1) ? "localhost" : args[1];

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

    public void init(String local_path, String filesystem_path){
        try {
            Boolean response = stub.add_storage_server(host, myPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    public void close(){
        try{
            Boolean response = stub.del_storage_server(myPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public boolean create(String path) throws RemoteException {
        return false;
    }

    @Override
    public boolean create(String path, byte[] blob) throws IOException {
        return false;
    }

    @Override
    public boolean del(String path) throws RemoteException {
        return false;
    }

    @Override
    public byte[] get(String path) throws RemoteException {
        return new byte[0];
    }
}
