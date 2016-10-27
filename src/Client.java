
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    public static String ServerImUsing;
    public static String CurrentDirectory = "/";
    public static ClientStorageInterface stubClientStorageInterface;
    public static ClientMetadataInterface stubClientMetadataInterface;

    private Client() {}

    public static String processInput(String[] inputCmd) throws RemoteException {
        String outPut ="YOU FUCKED UP";

        if (inputCmd[0].equals("cd")){
            String whereImGoing = inputCmd[1];
            CurrentDirectory = whereImGoing;
            ServerImUsing = stubClientMetadataInterface.find(whereImGoing);
            outPut = "Successfully changed to directory " + whereImGoing;
        }
        if (inputCmd[0].equals("pwd")){
            outPut = CurrentDirectory;
        }
        if (inputCmd[0].equals("ls")){

            String directoryToBeListed = (inputCmd.length < 2) ? "." : inputCmd[1];

            outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
        }

        return outPut;
    };

    public static void main(String[] args) {

        String rmiHost = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(rmiHost);

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


        try {
            System.out.print(CurrentDirectory+"> ");
            while(stdin.hasNextLine()){
                String fullCmd = stdin.nextLine();
                String[] cmdList = fullCmd.split(" ");
                String outPut = processInput(cmdList);
                System.out.println(outPut);
                System.out.print(CurrentDirectory+"> ");

            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }



    }
}