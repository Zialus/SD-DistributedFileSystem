
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    public static String ServerImUsing;
    public static String CurrentDirectory = "/";
    public static ClientStorageInterface stubClientStorageInterface;
    public static ClientMetadataInterface stubClientMetadataInterface;

    private Client() {}

    public static String processInput(String inputCmd) throws RemoteException {
        String outPut ="YOU FUCKED UP";
        if (inputCmd.equals("cd")){
            outPut = "ola";
        }
        if (inputCmd.equals("pwd")){
            outPut = CurrentDirectory;
        }
        if (inputCmd.equals("ls")){
            outPut = stubClientMetadataInterface.lstat(".");
        }

        return outPut;
    };

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);

            stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            ServerImUsing = stubClientMetadataInterface.find("/A");
            System.out.println("server: " + ServerImUsing);

            stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);

            boolean answer = stubClientStorageInterface.create("/Users/rmf/AAAAA/lol");
            System.out.println("answer: " + answer);


            String response = stubClientMetadataInterface.lstat("/A");
            System.out.println("response: " + response);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }


        Scanner stdin = new Scanner(System.in);

        String cmd;

        try {
            System.out.print(CurrentDirectory+"> ");
            while(stdin.hasNextLine()){
                cmd = stdin.nextLine();
                String outPut = processInput(cmd);
                System.out.println(outPut);
                System.out.print(CurrentDirectory+"> ");

            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }



    }
}