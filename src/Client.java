import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.stream.Stream;

public class Client {

    public static String ServerImUsing;
    public static String CurrentDirectory;
    public static ClientStorageInterface stubClientStorageInterface;
    public static ClientMetadataInterface stubClientMetadataInterface;
    public static HashMap<String,String> configsMap = new HashMap<>();
    public static String configFile;
    public static String CacheDir;
    public static Registry registry;
    public static String rmiHost;


    private Client() {}

    private static void processConfigFile() throws IOException {

        try (Stream<String> stream = Files.lines(Paths.get(configFile))) {
            stream.forEach(Client::addLineToHashMap);
        }

        // FOR DEBUG PURPOSES
        for (Map.Entry<String, String> entry : configsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println("Key->" + key + "|Value->" + value);
        }

    }

    private static void addLineToHashMap(String line){
        List<String> items = Arrays.asList(line.split("\\s+"));

        ArrayList<String> itemsWithoutCommas = new ArrayList<>();

        for (String item: items) {
            itemsWithoutCommas.add(item.replace(",", ""));
        }

        int lastItemIndex = itemsWithoutCommas.size() - 1;
        String appPath = items.get(lastItemIndex);

        for (int i = 0; i < lastItemIndex; i++){
            String extension = itemsWithoutCommas.get(i);
            configsMap.put(extension,appPath);
        }
    }

    private static String pathSanitizer(String dirtyPath){

        String cleanPath = dirtyPath;

        if ( !CurrentDirectory.equals("/") ){
            int lastSlash = CurrentDirectory.lastIndexOf("/");
            CurrentDirectory = CurrentDirectory.substring(lastSlash);
        }

        if(!cleanPath.startsWith("/")){

            if (CurrentDirectory.equals("/")){
                cleanPath =  "/" + cleanPath;
            } else {
                cleanPath = CurrentDirectory + "/" + cleanPath;
            }
        }

        return cleanPath;
    }

    private static String processInput(String[] inputCmd) throws IOException, NotBoundException {
        String outPut ="Nothing to report on";

        if (inputCmd[0].equals("cd")){

            String whereImGoing = pathSanitizer(inputCmd[1]);

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

            String directoryToBeListedTemp = (inputCmd.length < 2) ? "." : inputCmd[1];

            String directoryToBeListed = pathSanitizer(directoryToBeListedTemp);

            /*
            if(directoryToBeListed.equals(".")){
                outPut = stubClientMetadataInterface.lstat(CurrentDirectory);
            }
            else if(!directoryToBeListed.startsWith("/")){
                outPut = stubClientMetadataInterface.lstat(CurrentDirectory+"/"+directoryToBeListed);
            } else { */

            outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
            //}
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

        if (inputCmd[0].equals("rm")){

            if (inputCmd.length != 2){
                outPut = "oops..";
            } else {
                String pathOfFileToBeDeleted = inputCmd[1];

                stubClientStorageInterface.del(pathOfFileToBeDeleted);

                outPut = "File deleted successfully";
            }

        }

        if (inputCmd[0].equals("get")){

            if (inputCmd.length != 3){
                outPut = "oops..";
            } else {

                String pathToGetFileFrom = inputCmd[1];
                String pathWhereClientReceivesFiles = inputCmd[2];

                int indexLastSlash = pathToGetFileFrom.lastIndexOf("/");
                int length = pathToGetFileFrom.length();
                String fileToBeGotten = pathToGetFileFrom.substring(indexLastSlash+1,length);
                String pathWhereFileIs = pathToGetFileFrom.substring(0,indexLastSlash);

                byte[] bytesToBeReceived;

                System.out.println("WTF IS GOING ON " + pathWhereFileIs);

                ServerImUsing = stubClientMetadataInterface.find(pathWhereFileIs);

                System.out.println("WTF IS GOING ON 2 " + ServerImUsing);

                stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);

                System.out.println("WTF IS GOING ON 3 ");

                bytesToBeReceived = stubClientStorageInterface.get(pathToGetFileFrom);

                System.out.println("WTF IS GOING ON 4 ");

                System.out.println(pathWhereClientReceivesFiles + "/" + fileToBeGotten + " LALALA " + pathWhereClientReceivesFiles + " LELELE " + pathToGetFileFrom);

                Files.write(Paths.get(pathWhereClientReceivesFiles + "/" + fileToBeGotten), bytesToBeReceived);

                outPut = "File received successfully";
            }

        }

        if (inputCmd[0].equals("open")){

            String fileToOpen = inputCmd[1];

            int lastDot = fileToOpen.lastIndexOf(".");
            String extension = fileToOpen.substring(lastDot+1, fileToOpen.length() );

            String appToOpenThisExtension = configsMap.get(extension);


            int indexLastSlash = fileToOpen.lastIndexOf("/");
            int length = fileToOpen.length();
            String fileToBeGotten = fileToOpen.substring(indexLastSlash+1,length);
            String pathWhereFileIs = fileToOpen.substring(0,indexLastSlash);

            byte[] bytesToBeReceived;


            ServerImUsing = stubClientMetadataInterface.find(pathWhereFileIs);


            stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);


            bytesToBeReceived = stubClientStorageInterface.get(fileToOpen);

            File tempFile = File.createTempFile(fileToBeGotten,extension);

            Files.write((tempFile.toPath()), bytesToBeReceived);

            System.out.println("LETS OPEN IT " + appToOpenThisExtension + " " + tempFile.getPath());
            Runtime.getRuntime().exec(appToOpenThisExtension + " " + tempFile.getPath());

        }

        return outPut;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            configFile = "apps.conf";
            rmiHost = "localhost";
        } else if (args.length == 2) {
            configFile = args[0];
            rmiHost = args[1];
        } else {
            System.err.println("Wrong number of arguments");
            System.exit(1);
        }

        CurrentDirectory = "/";

        try {
            processConfigFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            registry = LocateRegistry.getRegistry(rmiHost);

            stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

            System.out.println("----------------------------------------------------");
            ServerImUsing = stubClientMetadataInterface.find("/");
            System.out.println("Server where '/' is: " + ServerImUsing);

            stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImUsing);
            boolean answer = stubClientStorageInterface.create("/home/tiago/johncena");
            System.out.println("Answer to create things: " + answer);

            String response = stubClientMetadataInterface.lstat("/");
            System.out.println("Response: " + response);

            System.out.println("----------------------------------------------------");

        } catch (NotBoundException e) {
            System.err.println("RMI Not Bound related exception: " + e.toString());
            e.printStackTrace();
        } catch (RemoteException e) {
            System.err.println("Remote related exception: " + e.toString());
            e.printStackTrace();
        }

        Scanner stdin = new Scanner(System.in);

        System.out.print(CurrentDirectory+">>");

        while(stdin.hasNextLine()){
            String fullCmd = stdin.nextLine();
            String[] cmdList = fullCmd.split(" ");
            String outPut;
            try {
                outPut = processInput(cmdList);
            } catch (IOException e) {
                e.printStackTrace();
                outPut = "IO stuff happened";
            } catch (NotBoundException e) {
                e.printStackTrace();
                outPut = "RMI stuff happened";
            }
            System.out.println(outPut);
            System.out.print(CurrentDirectory+">>");

        }

    }

}
