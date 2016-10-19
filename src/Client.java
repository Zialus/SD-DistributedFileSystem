import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static String ServerImUsing;
    public static String CurrentDirectory;


    private Client() {}

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            ClientMetadataInterface stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            ServerImUsing = stubClientMetadataInterface.find("/A");
            System.out.println("server: " + ServerImUsing);


            ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);

            boolean answer = stubClientStorageInterface.create("/Users/rmf/AAAAA/lol");
            System.out.println("answer: " + answer);


            String response = stubClientMetadataInterface.lstat("/A");
            System.out.println("response: " + response);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}