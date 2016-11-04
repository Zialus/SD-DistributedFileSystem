
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Client {

    public static String ServerImUsing;
    public static String CurrentDirectory = "/";
    public static ClientStorageInterface stubClientStorageInterface;
    public static ClientMetadataInterface stubClientMetadataInterface;
    public static HashMap<String,String> configs = new HashMap<>();


    private Client() {}

    public static void processConfigFile() {


    }

    public static String processInput(String[] inputCmd) throws IOException {
        String outPut ="YOU FUCKED UP";

        if (inputCmd[0].equals("cd")){

            String whereImGoing = inputCmd[1];

            if (whereImGoing.endsWith("..") && !CurrentDirectory.equals("/") ){
                int lastSlash = CurrentDirectory.lastIndexOf("/");
                CurrentDirectory = CurrentDirectory.substring(lastSlash);
            }

            if(!whereImGoing.startsWith("/")){

                if (CurrentDirectory.equals("/")){
                    whereImGoing =  "/" + whereImGoing;
                } else {
                    whereImGoing = CurrentDirectory + "/" + whereImGoing;
                }
            }

            String ServerImUsingTEMP = stubClientMetadataInterface.find(whereImGoing);

            if (ServerImUsingTEMP.equals("")) {
                outPut = "Can't find directory " + whereImGoing;
            } else {
                ServerImUsing = ServerImUsingTEMP;
                CurrentDirectory = whereImGoing;
                System.out.println("SERVING I'M USING "+ ServerImUsing);
                outPut = "Successfully changed to directory " + whereImGoing;
            }

        }
        if (inputCmd[0].equals("pwd")){
            outPut = CurrentDirectory;
        }
        if (inputCmd[0].equals("ls")){

            String directoryToBeListed = (inputCmd.length < 2) ? "." : inputCmd[1];

            if(directoryToBeListed.equals(".")){
                outPut = stubClientMetadataInterface.lstat(CurrentDirectory);
            }
            else if(!directoryToBeListed.startsWith("/")){
                outPut = stubClientMetadataInterface.lstat(CurrentDirectory+"/"+directoryToBeListed);

            }
            else {
                outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
            }
        }

        if (inputCmd[0].equals("put")){

            if (inputCmd.length != 3){
                outPut = "oops..";
            } else {

                Path pathOfFileToBeSent = Paths.get(inputCmd[1]);
                String pathWhereServerReceivesFiles = inputCmd[2];

                byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                stubClientStorageInterface.create(pathWhereServerReceivesFiles, bytesToBeSent);

                outPut = "File sent successfully";
            }

        }

        if (inputCmd[0].equals("get")){

            if (inputCmd.length != 3){
                outPut = "oops..";
            } else {

                String pathToGetFilesFrom = inputCmd[1];
                String pathWhereClientReceivesFiles = inputCmd[2];

                byte[] bytesToBeReceived;

                bytesToBeReceived = stubClientStorageInterface.get(pathToGetFilesFrom);

                Files.write(Paths.get(pathWhereClientReceivesFiles), bytesToBeReceived);

                outPut = "File received successfully";
            }

        }

        if (inputCmd[0].equals("open")){


        }

        return outPut;
    };

    public static void main(String[] args) {

        String rmiHost = (args.length < 1) ? "localhost" : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(rmiHost);

            stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            ServerImUsing = stubClientMetadataInterface.find("/");
            System.out.println("server: " + ServerImUsing);

            stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);

            boolean answer = stubClientStorageInterface.create("/home/tiago/johncena");
            System.out.println("answer: " + answer);


            String response = stubClientMetadataInterface.lstat("/");
            System.out.println("response: " + response);
            System.out.println("--------");

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