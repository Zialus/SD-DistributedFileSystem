import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static String ServerImUsing;

    private Client() {}

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ClientMetadataInterface stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            String response = stubClientMetadataInterface.lstat("/lol");
            System.out.println("response: " + response);

            String ServerImUsing = stubClientMetadataInterface.find("/courses");
            System.out.println("server: " + ServerImUsing);

            ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);


        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}