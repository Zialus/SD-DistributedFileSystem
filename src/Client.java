import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ClientMetadataInterface stub = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            String response = stub.lstat("/lol");
            System.out.println("response: " + response);
//
//            String path_to_look = "/courses";
//            String response = stub.find(path_to_look);
//            System.out.println("response: " + response);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}