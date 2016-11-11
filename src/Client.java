import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {

    public static String CurrentDirectory;
    public static ClientMetadataInterface stubClientMetadataInterface;
    public static HashMap<String,String> configsMap = new HashMap<>();
    public static String configFile;
    public static String CacheDir;
    public static Registry registry;
    public static String rmiHost;

    //color for output
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    private Client() {}

    private static void processConfigFile() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(configFile))) {
            stream.forEach(Client::addLineToHashMap);
        }
    }

    private static void addLineToHashMap(String line){
        List<String> items = Arrays.asList(line.split("\\s+"));

        ArrayList<String> itemsWithoutCommas = items.stream().map(item -> item.replace(",", "")).collect(Collectors.toCollection(ArrayList::new));

        int lastItemIndex = itemsWithoutCommas.size() - 1;
        String appPath = items.get(lastItemIndex);

        for (int i = 0; i < lastItemIndex; i++){
            String extension = itemsWithoutCommas.get(i);
            configsMap.put(extension,appPath);
        }
    }

    public static String pathSanitizer(String dirtyPath){
        String cleanPath = dirtyPath;

        if(cleanPath.endsWith("/") && !cleanPath.equals("/")){
            cleanPath = cleanPath.substring(0, cleanPath.length()-1);
        }

        if(!cleanPath.startsWith("/") && !cleanPath.equals(".") && !cleanPath.equals("..")){
            if (CurrentDirectory.equals("/")){
                cleanPath =  "/" + cleanPath;
            } else {
                cleanPath = CurrentDirectory + "/" + cleanPath;
            }
        }

        if(cleanPath.equals(".")){
            cleanPath = CurrentDirectory;
        }

        if(cleanPath.equals("..")){
            int indexLastSlash = CurrentDirectory.lastIndexOf("/");
            if(indexLastSlash > 0) {
                cleanPath = CurrentDirectory.substring(0, indexLastSlash);
            }
            else{
                cleanPath = "/";
            }
        }


        if(cleanPath.contains("..") || cleanPath.contains(".")){
            File f = new File(cleanPath);
            try {
                cleanPath = f.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return cleanPath;
    }

    private static String processInput(String[] inputCmd) throws IOException, NotBoundException {
        String outPut = inputCmd[0] + ": command not found";

        switch (inputCmd[0]) {
            case "cd":

                if (inputCmd.length != 2) {
                    outPut = "Incorrect use of cd command";
                } else {

                    String whereImGoing = pathSanitizer(inputCmd[1]);

                    String ServerImGoingToUse = stubClientMetadataInterface.find(whereImGoing);

                    if (ServerImGoingToUse.equals("")) {
                        outPut = "Can't find directory " + whereImGoing;
                    } else {
                        CurrentDirectory = whereImGoing;
                        outPut = "Changed to directory " + whereImGoing;
                    }
                }

                break;

            case "pwd":

                if (inputCmd.length != 1) {
                    outPut = "Incorrect use of pwd command";
                } else {
                    outPut = CurrentDirectory;
                }
                break;

            case "ls":

                if (inputCmd.length > 2) {
                    outPut = "Incorrect use of ls command";
                } else {
                    String directoryToBeListedTemp = (inputCmd.length == 1) ? "." : inputCmd[1];
                    String directoryToBeListed = pathSanitizer(directoryToBeListedTemp);
                    outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
                    if (outPut.equals("")) {
                        outPut = inputCmd[1] + ": no such file or directory";
                    }
                }

                break;
            case "put":

                if (inputCmd.length != 3) {
                    outPut = "Incorrect use of put command";
                } else {

                    Path pathOfFileToBeSent = Paths.get(inputCmd[1]);
                    String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

                    int indexLastSlash = pathOfFileToBeSent.toString().lastIndexOf("/");
                    int length = pathOfFileToBeSent.toString().length();
                    String fileToBeSent = pathOfFileToBeSent.toString().substring(indexLastSlash + 1, length);

                    String ServerImGoingToUse = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);
                    System.out.println("SERVERIMUSING " + ServerImGoingToUse);
                    ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImGoingToUse);

                    byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                    System.out.println("File comming from " + pathOfFileToBeSent + " going too ---> " + pathWhereServerReceivesFiles);

                    stubClientStorageInterface.create(pathWhereServerReceivesFiles + "/" + fileToBeSent, bytesToBeSent);

                    outPut = "File sent successfully";
                }

                break;
            case "rm":

                if (inputCmd.length != 2) {
                    outPut = "Incorrect use of rm command";
                } else {

                    String pathOfFileToBeDeletedTEMP = inputCmd[1];
                    String pathOfFileToBeDeleted = pathSanitizer(pathOfFileToBeDeletedTEMP);
                    String ServerImGoingToUse = stubClientMetadataInterface.find(pathOfFileToBeDeleted);
                    if(!ServerImGoingToUse.equals("")) {
                        ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImGoingToUse);
                        boolean answer = stubClientStorageInterface.del(pathOfFileToBeDeleted);

                        if (answer) {
                            outPut = "File deleted successfully";
                        } else {
                            outPut = "File not deleted successfully";
                        }
                    }
                    else {
                        outPut = pathOfFileToBeDeleted + ": no such file or directory";
                    }
                }

                break;

            case "get":

                if (inputCmd.length != 3) {
                    outPut = "Incorrect use of get command";
                } else {

                    String pathToGetFileFrom = pathSanitizer(inputCmd[1]);
                    String pathWhereClientReceivesFiles = inputCmd[2];

                    int indexLastSlash = pathToGetFileFrom.lastIndexOf("/");
                    int length = pathToGetFileFrom.length();
                    String fileToBeGotten = pathToGetFileFrom.substring(indexLastSlash + 1, length);
                    String pathWhereFileIs = pathToGetFileFrom.substring(0, indexLastSlash);

                    byte[] bytesToBeReceived;

                    String ServerImGoingToUse = stubClientMetadataInterface.find(pathWhereFileIs);

                    ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImGoingToUse);

                    bytesToBeReceived = stubClientStorageInterface.get(pathToGetFileFrom);

                    System.out.println("File comming from " + pathWhereFileIs + "/" + fileToBeGotten + " going too ---> " + pathWhereClientReceivesFiles + "/" + fileToBeGotten);

                    Files.write(Paths.get(pathWhereClientReceivesFiles + "/" + fileToBeGotten), bytesToBeReceived);

                    outPut = "File received successfully";
                }

                break;

            case "open":

                String fileToOpen = pathSanitizer(inputCmd[1]);

                int lastDot = fileToOpen.lastIndexOf(".");
                String extension = fileToOpen.substring(lastDot + 1, fileToOpen.length());

                String appToOpenThisExtension = configsMap.get(extension);

                int indexLastSlash = fileToOpen.lastIndexOf("/");
                int length = fileToOpen.length();
                String fileToBeGotten = fileToOpen.substring(indexLastSlash + 1, length);

                byte[] bytesToBeReceived;

                String ServerImGoingToUse = stubClientMetadataInterface.find(fileToOpen);
                if (!ServerImGoingToUse.equals("")) {
                    System.out.println("SERVER I'M USING " + ServerImGoingToUse);

                    ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(ServerImGoingToUse);

                    bytesToBeReceived = stubClientStorageInterface.get(fileToOpen);

                    File tempFile = File.createTempFile(fileToBeGotten, extension);

                    Files.write((tempFile.toPath()), bytesToBeReceived);

                    Runtime.getRuntime().exec(appToOpenThisExtension + " " + tempFile.getPath());

                    outPut = "Opened file " + fileToOpen + " with " + appToOpenThisExtension;
                } else {
                    outPut = inputCmd[1] + ": no such file or directory";
                }
                break;
            case "mv":
                if (inputCmd.length != 3) {
                    outPut = "Incorrect use of command mv";
                } else {
                    String fileToMove = pathSanitizer(inputCmd[1]);
                    String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

                    int lastDotMV = fileToMove.lastIndexOf(".");
                    String extensionMV = fileToMove.substring(lastDotMV + 1, fileToMove.length());

                    int indexLastSlashMV = fileToMove.lastIndexOf("/");
                    int lengthMV = fileToMove.length();

                    String fileToBeGottenMV = fileToMove.substring(indexLastSlashMV + 1, lengthMV);

                    String ServerComingFrom = stubClientMetadataInterface.find(fileToMove);
                    String ServerGoingTo = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);
                    if (!ServerComingFrom.equals("")) {
                        System.out.println("SERVER I'M USING " + ServerComingFrom);

                        ClientStorageInterface stubClientStorageInterfaceFrom = (ClientStorageInterface) registry.lookup(ServerComingFrom);
                        ClientStorageInterface stubClientStorageInterfaceTo = (ClientStorageInterface) registry.lookup(ServerGoingTo);

                        byte[] bytesToBeReceivedMV = stubClientStorageInterfaceFrom.get(fileToMove);

                        File tempFile = File.createTempFile(fileToBeGottenMV, extensionMV);

                        Files.write((tempFile.toPath()), bytesToBeReceivedMV);

                        Path pathOfFileToBeSent = Paths.get(tempFile.toString());



                        int indexLastSlash2 = pathOfFileToBeSent.toString().lastIndexOf("/");
                        int length2 = pathOfFileToBeSent.toString().length();
                        String fileToBeSent = pathOfFileToBeSent.toString().substring(indexLastSlash2 + 1, length2);

                        byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                        System.out.println("File comming from " + pathOfFileToBeSent + " going too ---> " + pathWhereServerReceivesFiles);

                        stubClientStorageInterfaceTo.create(pathWhereServerReceivesFiles + "/" + fileToBeGottenMV, bytesToBeSent);

                        boolean answer = stubClientStorageInterfaceFrom.del(fileToMove);
                    }
                }
                break;
        }


        return outPut;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            configFile = System.getProperty("user.dir") + "/../../../apps.conf";
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

        } catch (NotBoundException e) {
            System.err.println("RMI Not Bound related exception: " + e.toString());
            e.printStackTrace();
        } catch (RemoteException e) {
            System.err.println("Remote related exception: " + e.toString());
            e.printStackTrace();
        }

        Scanner stdin = new Scanner(System.in);

        System.out.print(ANSI_GREEN + CurrentDirectory+ " $ " + ANSI_RESET);

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
            System.out.print(ANSI_GREEN + CurrentDirectory+ " $ " + ANSI_RESET);

        }

    }

}
