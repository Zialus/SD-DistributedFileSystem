import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class StorageServer {

    public String myPath;
    public String host;
    public StorageMetadataInterface stub;

    public StorageServer() {

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            this.stub = (StorageMetadataInterface) registry.lookup("StorageMetadataInterface");


        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

    }

    public void main(String[] args) {

        this.myPath = args[0];
        this.host = (args.length < 1) ? "localhost" : args[1];

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
            Boolean response = stub.add_storage_server(this.host, this.myPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    public void close(){
        try{
            Boolean response = stub.del_storage_server(this.myPath);
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
