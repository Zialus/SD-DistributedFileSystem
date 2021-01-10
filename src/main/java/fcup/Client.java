package fcup;

import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {

    private static final HashMap<String, String> configsMap = new HashMap<>();

    private static String currentDirectory;

    private static ClientMetadataInterface stubClientMetadataInterface;

    private static String configFile;

    private static Registry registry;

    private static String rmiHost;

    private static void processConfigFile() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(configFile))) {
            stream.forEach(Client::addLineToHashMap);
        }
    }

    private static void addLineToHashMap(String line) {
        List<String> items = Arrays.asList(line.split("\\s+"));

        ArrayList<String> itemsWithoutCommas = items.stream().map(item -> item.replace(",", "")).collect(Collectors.toCollection(ArrayList::new));

        int lastItemIndex = itemsWithoutCommas.size() - 1;
        String appPath = items.get(lastItemIndex);

        for (int i = 0; i < lastItemIndex; i++) {
            String extension = itemsWithoutCommas.get(i);
            configsMap.put(extension, appPath);
        }
    }

    private static String pathSanitizer(String dirtyPath) {
        String cleanPath = dirtyPath;

        if (".".equals(cleanPath)) {
            cleanPath = currentDirectory;
        } else if ("..".equals(cleanPath)) {
            int indexLastSlash = currentDirectory.lastIndexOf('/');
            if (indexLastSlash > 0) {
                cleanPath = currentDirectory.substring(0, indexLastSlash);
            } else {
                cleanPath = "/";
            }
        } else if (!cleanPath.startsWith("/")) {
            if ("/".equals(currentDirectory)) {
                cleanPath = '/' + cleanPath;
            } else {
                cleanPath = currentDirectory + '/' + cleanPath;
            }
        }

        File f = new File(cleanPath);
        try {
            cleanPath = f.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return cleanPath;
    }

    private static String changeDir(String[] inputCmd) throws RemoteException {
        String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of cd command";
        } else {

            String whereImGoing = pathSanitizer(inputCmd[1]);

            String serverImGoingToUse = stubClientMetadataInterface.find(whereImGoing);
            FileType filetype = stubClientMetadataInterface.findInfo(whereImGoing);


            if (serverImGoingToUse.isEmpty()) {

                outPut = "Can't find directory " + whereImGoing;
            } else if (filetype == FileType.DIRECTORY) {
                currentDirectory = whereImGoing;
                outPut = "Changed to directory " + whereImGoing;
            } else {
                outPut = "Incorrect use of cd command";
            }

        }

        return outPut;
    }

    private static String presentWorkingDir(String[] inputCmd) {
        String outPut;

        if (inputCmd.length != 1) {
            outPut = "Incorrect use of pwd command";
        } else {
            outPut = currentDirectory;
        }

        return outPut;
    }

    private static String listFiles(String[] inputCmd) throws RemoteException {
        String outPut;

        if (inputCmd.length > 2) {
            outPut = "Incorrect use of ls command";
        } else {
            String directoryToBeListedTemp = (inputCmd.length == 1) ? "." : inputCmd[1];
            String directoryToBeListed = pathSanitizer(directoryToBeListedTemp);

            outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
            if (outPut.isEmpty()) {
                outPut = directoryToBeListed + ": no such file or directory";
            }
        }

        return outPut;
    }

    private static String putFile(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of put command";
        } else {

            Path pathOfFileToBeSent = Paths.get(inputCmd[1]);
            String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

            int indexLastSlash = pathOfFileToBeSent.toString().lastIndexOf('/');
            int length = pathOfFileToBeSent.toString().length();
            String fileToBeSent = pathOfFileToBeSent.toString().substring(indexLastSlash + 1, length);

            String serverImGoingToUse = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);

            if (!serverImGoingToUse.isEmpty()) {
                ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                System.out.println("File coming from: " + pathOfFileToBeSent + " going too: " + pathWhereServerReceivesFiles);

                boolean maybeCreated = stubClientStorageInterface.create(pathWhereServerReceivesFiles + '/' + fileToBeSent, bytesToBeSent);

                if (maybeCreated) {
                    outPut = "File sent successfully";
                } else {
                    outPut = "File could not be sent";
                }
            } else {
                outPut = pathWhereServerReceivesFiles + ": No such directory";
            }

        }

        return outPut;
    }

    private static String removeFile(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of rm command";
        } else {

            String pathOfFileToBeDeletedTemp = inputCmd[1];
            String pathOfFileToBeDeleted = pathSanitizer(pathOfFileToBeDeletedTemp);
            String serverImGoingToUse = stubClientMetadataInterface.find(pathOfFileToBeDeleted);
            if (!serverImGoingToUse.isEmpty()) {
                ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);
                boolean answer = stubClientStorageInterface.del(pathOfFileToBeDeleted);

                if (answer) {
                    outPut = "File deleted successfully";
                } else {
                    outPut = "File not deleted successfully";
                }
            } else {
                outPut = pathOfFileToBeDeleted + ": no such file or directory";
            }
        }

        return outPut;
    }

    private static String getFile(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of get command";
        } else {

            String pathToGetFileFrom = pathSanitizer(inputCmd[1]);
            String pathWhereClientReceivesFiles = inputCmd[2];

            int indexLastSlash = pathToGetFileFrom.lastIndexOf('/');
            int length = pathToGetFileFrom.length();
            String fileToBeGotten = pathToGetFileFrom.substring(indexLastSlash + 1, length);
            String pathWhereFileIs = pathToGetFileFrom.substring(0, indexLastSlash);

            byte[] bytesToBeReceived;

            String serverImGoingToUse = stubClientMetadataInterface.find(pathToGetFileFrom);

            if (!serverImGoingToUse.isEmpty()) {
                ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                bytesToBeReceived = stubClientStorageInterface.get(pathToGetFileFrom);

                if (Files.isDirectory(Paths.get(pathWhereClientReceivesFiles))) {
                    Files.write(Paths.get(pathWhereClientReceivesFiles + '/' + fileToBeGotten), bytesToBeReceived);
                    System.out.println("File coming from: " + pathWhereFileIs + '/' + fileToBeGotten + " going too: " + pathWhereClientReceivesFiles + '/' + fileToBeGotten);
                    outPut = "File received successfully";
                } else {
                    outPut = "No such directory";
                }
            } else {
                outPut = "No such file";
            }

        }

        return outPut;
    }

    private static String makeDir(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of mkdir command";
        } else {
            String pathOfDirectoryToBeCreated = pathSanitizer(inputCmd[1]);
            int indexLastSlash = pathOfDirectoryToBeCreated.lastIndexOf('/');
            int length = pathOfDirectoryToBeCreated.length();

            String pathWhereDirWillBeCreated;
            if (indexLastSlash == 0) {
                pathWhereDirWillBeCreated = "/";
            } else {
                pathWhereDirWillBeCreated = pathOfDirectoryToBeCreated.substring(0, indexLastSlash);
            }
            String dirName = pathOfDirectoryToBeCreated.substring(indexLastSlash + 1, length);

            String serverImGoingToUse = stubClientMetadataInterface.find(pathWhereDirWillBeCreated);
            if (!serverImGoingToUse.isEmpty()) {
                ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                boolean maybeCreated = stubClientStorageInterface.create(pathWhereDirWillBeCreated + '/' + dirName);

                if (maybeCreated) {
                    outPut = "Dir " + pathOfDirectoryToBeCreated + " created successfully";
                } else {
                    outPut = "Dir " + pathOfDirectoryToBeCreated + " could not be created!";
                }
            } else {
                outPut = pathWhereDirWillBeCreated + ": no such file or directory..";
            }
        }

        return outPut;
    }

    private static String openFile(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        String fileToOpen = pathSanitizer(inputCmd[1]);

        int lastDot = fileToOpen.lastIndexOf('.');
        String extension = fileToOpen.substring(lastDot + 1, fileToOpen.length());

        String appToOpenThisExtension = configsMap.get(extension);

        if (appToOpenThisExtension != null) {
            int indexLastSlash = fileToOpen.lastIndexOf('/');
            int length = fileToOpen.length();
            String fileToBeGotten = fileToOpen.substring(indexLastSlash + 1, length);

            byte[] bytesToBeReceived;

            String serverImGoingToUse = stubClientMetadataInterface.find(fileToOpen);
            FileType fileType = stubClientMetadataInterface.findInfo(fileToOpen);

            if (!serverImGoingToUse.isEmpty()) {
                if (fileType == FileType.FILE) {
                    ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                    bytesToBeReceived = stubClientStorageInterface.get(fileToOpen);

                    File tempFile = File.createTempFile(fileToBeGotten, extension);

                    Files.write((tempFile.toPath()), bytesToBeReceived);

                    Runtime.getRuntime().exec(appToOpenThisExtension + ' ' + tempFile.getPath());

                    outPut = "Opened file " + fileToOpen + " with " + appToOpenThisExtension;
                } else {
                    outPut = "Can't open a directory";
                }
            } else {
                outPut = inputCmd[1] + ": no such file or directory";
            }
        }

        outPut = "No app to open this file type";

        return outPut;
    }

    private static String moveDir(String[] inputCmd) throws IOException, NotBoundException {
        String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of command mv";
        } else {
            String fileInDestiny;
            String fileToMove = pathSanitizer(inputCmd[1]);
            String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

            int lastDotMV = fileToMove.lastIndexOf('.');
            String extensionMV = fileToMove.substring(lastDotMV + 1, fileToMove.length());

            int indexLastSlashMV = fileToMove.lastIndexOf('/');
            int lengthMV = fileToMove.length();

            String fileToBeGottenMV = fileToMove.substring(indexLastSlashMV + 1, lengthMV);

            String serverComingFrom = stubClientMetadataInterface.find(fileToMove);
            FileType originFiletype = stubClientMetadataInterface.findInfo(fileToMove);


            FileType filetype = stubClientMetadataInterface.findInfo(pathWhereServerReceivesFiles);
            String serverGoingTo = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);


            if (!serverComingFrom.isEmpty() && !serverGoingTo.isEmpty()) {
                if (originFiletype == FileType.DIRECTORY) {
                    outPut = "Can't move a directory";
                } else {
                    ClientStorageInterface stubClientStorageInterfaceFrom = (ClientStorageInterface) registry.lookup(serverComingFrom);
                    ClientStorageInterface stubClientStorageInterfaceTo = (ClientStorageInterface) registry.lookup(serverGoingTo);

                    byte[] bytesToBeReceivedMV = stubClientStorageInterfaceFrom.get(fileToMove);

                    File tempFile = File.createTempFile(fileToBeGottenMV, extensionMV);

                    Files.write((tempFile.toPath()), bytesToBeReceivedMV);

                    Path pathOfFileToBeSent = Paths.get(tempFile.toString());
                    byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                    if ((filetype == FileType.FILE) || (filetype == FileType.NULL)) {
//                                System.out.println("ENTREI AQUI -> " + filetype.toString());
                        int indexLastSlash2 = pathWhereServerReceivesFiles.lastIndexOf('/');
                        int length2 = pathWhereServerReceivesFiles.length();
                        fileInDestiny = pathWhereServerReceivesFiles.substring(indexLastSlash2 + 1, length2);
                    } else {
                        fileInDestiny = fileToBeGottenMV;
                    }

                    System.out.println("File coming from: " + pathOfFileToBeSent + " going too: " + pathWhereServerReceivesFiles);

                    stubClientStorageInterfaceTo.create(pathWhereServerReceivesFiles + '/' + fileInDestiny, bytesToBeSent);

                    stubClientStorageInterfaceFrom.del(fileToMove);

                    outPut = "File moved to " + pathWhereServerReceivesFiles + '/' + fileInDestiny;
                }
            } else if (serverComingFrom.isEmpty()) {
                outPut = fileToMove + ": no such file or directory";
            } else {
                outPut = pathWhereServerReceivesFiles + ": no such directory";
            }
        }

        return outPut;
    }

    private static String processInput(String[] inputCmd) throws IOException, NotBoundException {
        String outPut = "";

        switch (inputCmd[0]) {
            case "exit":
                System.exit(0);
                break;
            case "cd":
                outPut = changeDir(inputCmd);
                break;
            case "pwd":
                outPut = presentWorkingDir(inputCmd);
                break;
            case "ls":
                outPut = listFiles(inputCmd);
                break;
            case "put":
                outPut = putFile(inputCmd);
                break;
            case "rm":
                outPut = removeFile(inputCmd);
                break;
            case "get":
                outPut = getFile(inputCmd);
                break;
            case "mkdir":
                outPut = makeDir(inputCmd);
                break;
            case "open":
                outPut = openFile(inputCmd);
                break;
            case "mv":
                outPut = moveDir(inputCmd);
                break;
            default:
                outPut = inputCmd[0] + ": command not found";
                break;
        }

        return outPut;
    }

    public static void main(String[] args) throws IOException {

        switch (args.length) {
            case 1:
                configFile = args[0];
                rmiHost = "localhost";
                break;
            case 2:
                configFile = args[0];
                rmiHost = args[1];
                break;
            default:
                System.err.println("Wrong number of arguments");
                System.exit(1);
                break;
        }

        currentDirectory = "/";

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
        } catch (AccessException e) {
            System.err.println("Access related exception: " + e.toString());
            e.printStackTrace();
        } catch (RemoteException e) {
            System.err.println("Remote related exception: " + e.toString());
            e.printStackTrace();
        }

        TerminalBuilder builder = TerminalBuilder.builder();
        Terminal terminal = builder.system(true).build();

        Completer completer = new Completers.FileNameCompleter();
        Parser parser = new DefaultParser();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .build();

        // Color for output
        String ANSI_GREEN = "\u001B[32m";

        String ANSI_RESET = "\u001B[0m";

        String prompt = ANSI_GREEN + currentDirectory + " $ " + ANSI_RESET;

        while (true) {
            String fullCmd = reader.readLine(prompt);
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
        }

    }

}
