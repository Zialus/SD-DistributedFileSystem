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


        //String host = (args.length < 1) ? "localhost" : args[0];
        //this.myPath = (args.length < 2) ? null : args[1];

    }

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
