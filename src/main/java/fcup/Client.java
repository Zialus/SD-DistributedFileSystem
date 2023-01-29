package fcup;

import lombok.extern.java.Log;
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

@Log
public class Client {

    private static final HashMap<String, String> configsMap = new HashMap<>();

    private static String currentDirectory;

    private static ClientMetadataInterface stubClientMetadataInterface;

    private static String configFile;

    private static Registry registry;

    private static String rmiHost;

    private static final String ANSI_GREEN = "\u001B[32m";

    private static final String ANSI_RESET = "\u001B[0m";

    private static void processConfigFile() throws IOException {
        try (final Stream<String> stream = Files.lines(Paths.get(configFile))) {
            stream.forEach(Client::addLineToHashMap);
        }
    }

    private static void addLineToHashMap(final String line) {
        final List<String> items = Arrays.asList(line.split("\\s+"));

        final ArrayList<String> itemsWithoutCommas = items.stream().map(item -> item.replace(",", "")).collect(Collectors.toCollection(ArrayList::new));

        final int lastItemIndex = itemsWithoutCommas.size() - 1;
        final String appPath = items.get(lastItemIndex);

        for (int i = 0; i < lastItemIndex; i++) {
            final String extension = itemsWithoutCommas.get(i);
            configsMap.put(extension, appPath);
        }
    }

    private static String pathSanitizer(final String dirtyPath) {
        String cleanPath = dirtyPath;

        if (".".equals(cleanPath)) {
            cleanPath = currentDirectory;
        } else if ("..".equals(cleanPath)) {
            final int indexLastSlash = currentDirectory.lastIndexOf('/');
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

        final File f = new File(cleanPath);
        try {
            cleanPath = f.getCanonicalPath();
        } catch (final IOException e) {
            log.severe(e.toString());
        }


        return cleanPath;
    }

    private static String changeDir(final String[] inputCmd) throws RemoteException {
        final String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of cd command";
        } else {

            final String whereImGoing = pathSanitizer(inputCmd[1]);

            final String serverImGoingToUse = stubClientMetadataInterface.find(whereImGoing);
            final FileType filetype = stubClientMetadataInterface.findInfo(whereImGoing);


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

    private static String presentWorkingDir(final String[] inputCmd) {
        final String outPut;

        if (inputCmd.length != 1) {
            outPut = "Incorrect use of pwd command";
        } else {
            outPut = currentDirectory;
        }

        return outPut;
    }

    private static String listFiles(final String[] inputCmd) throws RemoteException {
        String outPut;

        if (inputCmd.length > 2) {
            outPut = "Incorrect use of ls command";
        } else {
            final String directoryToBeListedTemp = (inputCmd.length == 1) ? "." : inputCmd[1];
            final String directoryToBeListed = pathSanitizer(directoryToBeListedTemp);

            outPut = stubClientMetadataInterface.lstat(directoryToBeListed);
            if (outPut.isEmpty()) {
                outPut = directoryToBeListed + ": no such file or directory";
            }
        }

        return outPut;
    }

    private static String putFile(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of put command";
        } else {

            final Path pathOfFileToBeSent = Paths.get(inputCmd[1]);
            final String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

            final int indexLastSlash = pathOfFileToBeSent.toString().lastIndexOf('/');
            final int length = pathOfFileToBeSent.toString().length();
            final String fileToBeSent = pathOfFileToBeSent.toString().substring(indexLastSlash + 1, length);

            final String serverImGoingToUse = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);

            if (!serverImGoingToUse.isEmpty()) {
                final ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                final byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                System.out.println("File coming from: " + pathOfFileToBeSent + " going too: " + pathWhereServerReceivesFiles);

                final boolean maybeCreated = stubClientStorageInterface.create(pathWhereServerReceivesFiles + '/' + fileToBeSent, bytesToBeSent);

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

    private static String removeFile(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of rm command";
        } else {

            final String pathOfFileToBeDeletedTemp = inputCmd[1];
            final String pathOfFileToBeDeleted = pathSanitizer(pathOfFileToBeDeletedTemp);
            final String serverImGoingToUse = stubClientMetadataInterface.find(pathOfFileToBeDeleted);
            if (!serverImGoingToUse.isEmpty()) {
                final ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);
                final boolean answer = stubClientStorageInterface.del(pathOfFileToBeDeleted);

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

    private static String getFile(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of get command";
        } else {

            final String pathToGetFileFrom = pathSanitizer(inputCmd[1]);
            final String pathWhereClientReceivesFiles = inputCmd[2];

            final int indexLastSlash = pathToGetFileFrom.lastIndexOf('/');
            final int length = pathToGetFileFrom.length();
            final String fileToBeGotten = pathToGetFileFrom.substring(indexLastSlash + 1, length);
            final String pathWhereFileIs = pathToGetFileFrom.substring(0, indexLastSlash);

            final byte[] bytesToBeReceived;

            final String serverImGoingToUse = stubClientMetadataInterface.find(pathToGetFileFrom);

            if (!serverImGoingToUse.isEmpty()) {
                final ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

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

    private static String makeDir(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        if (inputCmd.length != 2) {
            outPut = "Incorrect use of mkdir command";
        } else {
            final String pathOfDirectoryToBeCreated = pathSanitizer(inputCmd[1]);
            final int indexLastSlash = pathOfDirectoryToBeCreated.lastIndexOf('/');
            final int length = pathOfDirectoryToBeCreated.length();

            final String pathWhereDirWillBeCreated;
            if (indexLastSlash == 0) {
                pathWhereDirWillBeCreated = "/";
            } else {
                pathWhereDirWillBeCreated = pathOfDirectoryToBeCreated.substring(0, indexLastSlash);
            }
            final String dirName = pathOfDirectoryToBeCreated.substring(indexLastSlash + 1, length);

            final String serverImGoingToUse = stubClientMetadataInterface.find(pathWhereDirWillBeCreated);
            if (!serverImGoingToUse.isEmpty()) {
                final ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                final boolean maybeCreated = stubClientStorageInterface.create(pathWhereDirWillBeCreated + '/' + dirName);

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

    private static String openFile(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        final String fileToOpen = pathSanitizer(inputCmd[1]);

        final int lastDot = fileToOpen.lastIndexOf('.');
        final String extension = fileToOpen.substring(lastDot + 1, fileToOpen.length());

        final String appToOpenThisExtension = configsMap.get(extension);

        if (appToOpenThisExtension != null) {
            final int indexLastSlash = fileToOpen.lastIndexOf('/');
            final int length = fileToOpen.length();
            final String fileToBeGotten = fileToOpen.substring(indexLastSlash + 1, length);

            final byte[] bytesToBeReceived;

            final String serverImGoingToUse = stubClientMetadataInterface.find(fileToOpen);
            final FileType fileType = stubClientMetadataInterface.findInfo(fileToOpen);

            if (!serverImGoingToUse.isEmpty()) {
                if (fileType == FileType.FILE) {
                    final ClientStorageInterface stubClientStorageInterface = (ClientStorageInterface) registry.lookup(serverImGoingToUse);

                    bytesToBeReceived = stubClientStorageInterface.get(fileToOpen);

                    final File tempFile = File.createTempFile(fileToBeGotten, extension);

                    Files.write(tempFile.toPath(), bytesToBeReceived);

                    final String[] commandLineArgs = {appToOpenThisExtension, tempFile.getPath()};

                    Runtime.getRuntime().exec(commandLineArgs);

                    outPut = "Opened file " + fileToOpen + " with " + appToOpenThisExtension;
                } else {
                    outPut = "Can't open a directory";
                }
            } else {
                outPut = inputCmd[1] + ": no such file or directory";
            }
        } else {
            outPut = "No app to open this file type";
        }

        return outPut;
    }

    private static String moveDir(final String[] inputCmd) throws IOException, NotBoundException {
        final String outPut;

        if (inputCmd.length != 3) {
            outPut = "Incorrect use of command mv";
        } else {
            final String fileInDestiny;
            final String fileToMove = pathSanitizer(inputCmd[1]);
            final String pathWhereServerReceivesFiles = pathSanitizer(inputCmd[2]);

            final int lastDotMV = fileToMove.lastIndexOf('.');
            final String extensionMV = fileToMove.substring(lastDotMV + 1, fileToMove.length());

            final int indexLastSlashMV = fileToMove.lastIndexOf('/');
            final int lengthMV = fileToMove.length();

            final String fileToBeGottenMV = fileToMove.substring(indexLastSlashMV + 1, lengthMV);

            final String serverComingFrom = stubClientMetadataInterface.find(fileToMove);
            final FileType originFiletype = stubClientMetadataInterface.findInfo(fileToMove);


            final FileType filetype = stubClientMetadataInterface.findInfo(pathWhereServerReceivesFiles);
            final String serverGoingTo = stubClientMetadataInterface.find(pathWhereServerReceivesFiles);


            if (!serverComingFrom.isEmpty() && !serverGoingTo.isEmpty()) {
                if (originFiletype == FileType.DIRECTORY) {
                    outPut = "Can't move a directory";
                } else {
                    final ClientStorageInterface stubClientStorageInterfaceFrom = (ClientStorageInterface) registry.lookup(serverComingFrom);
                    final ClientStorageInterface stubClientStorageInterfaceTo = (ClientStorageInterface) registry.lookup(serverGoingTo);

                    final byte[] bytesToBeReceivedMV = stubClientStorageInterfaceFrom.get(fileToMove);

                    final File tempFile = File.createTempFile(fileToBeGottenMV, extensionMV);

                    Files.write(tempFile.toPath(), bytesToBeReceivedMV);

                    final Path pathOfFileToBeSent = Paths.get(tempFile.toString());
                    final byte[] bytesToBeSent = Files.readAllBytes(pathOfFileToBeSent);

                    if ((filetype == FileType.FILE) || (filetype == FileType.NULL)) {
                        log.finest("ENTREI AQUI -> " + filetype.toString());
                        final int indexLastSlash2 = pathWhereServerReceivesFiles.lastIndexOf('/');
                        final int length2 = pathWhereServerReceivesFiles.length();
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

    private static String processInput(final String[] inputCmd) throws IOException, NotBoundException {
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

    public static void main(final String[] args) throws IOException {

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
        } catch (final IOException e) {
            log.severe(e.toString());
        }

        try {
            registry = LocateRegistry.getRegistry(rmiHost);

            stubClientMetadataInterface = (ClientMetadataInterface) registry.lookup("ClientMetadataInterface");

        } catch (final NotBoundException e) {
            log.severe("RMI Not Bound related exception: " + e.toString());
        } catch (final AccessException e) {
            log.severe("Access related exception: " + e.toString());
        } catch (final RemoteException e) {
            log.severe("Remote related exception: " + e.toString());
        }

        final TerminalBuilder builder = TerminalBuilder.builder();
        final Terminal terminal = builder.system(true).build();

        final Completer completer = new Completers.FileNameCompleter();
        final Parser parser = new DefaultParser();

        final LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .build();

        // Color for output
        final String prompt = ANSI_GREEN + currentDirectory + " $ " + ANSI_RESET;

        while (true) {
            final String fullCmd = reader.readLine(prompt);
            final String[] cmdList = fullCmd.split(" ");
            String outPut;
            try {
                outPut = processInput(cmdList);
            } catch (final IOException e) {
                log.severe(e.toString());
                outPut = "IO stuff happened";
            } catch (final NotBoundException e) {
                log.severe(e.toString());
                outPut = "RMI stuff happened";
            }
            System.out.println(outPut);
        }

    }

}
