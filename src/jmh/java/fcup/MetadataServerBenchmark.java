package fcup;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.rmi.RemoteException;

/**
 * Benchmarks for the MetadataServer operations served to clients and storage servers.
 * The RMI layer is not involved: methods are called directly on the server object.
 */
@State(Scope.Benchmark)
public class MetadataServerBenchmark {

    private static final int DEPTH = 8;

    private static final int FILES_PER_DIRECTORY = 128;

    private MetadataServer server;

    private String deepDirPath;

    private String deepFilePath;

    private String[] itemPaths;

    @Setup(Level.Trial)
    public void setup() throws RemoteException {
        FileSystemTreeBenchmark.BenchmarkSupport.silenceLogging();
        server = new MetadataServer();
        server.addStorageServer("Machine1", "/bench");

        final StringBuilder dir = new StringBuilder("/bench");
        for (int i = 0; i < DEPTH; i++) {
            dir.append("/dir").append(i);
            server.addStorageItem(dir.toString(), "Machine1", true);
        }
        deepDirPath = dir.toString();

        itemPaths = new String[FILES_PER_DIRECTORY];
        for (int f = 0; f < FILES_PER_DIRECTORY; f++) {
            itemPaths[f] = deepDirPath + "/file" + f + ".txt";
            server.addStorageItem(itemPaths[f], "Machine1", false);
        }
        deepFilePath = itemPaths[FILES_PER_DIRECTORY - 1];
    }

    @Benchmark
    public String find() {
        return server.find(deepFilePath);
    }

    @Benchmark
    public FileType findInfo() throws RemoteException {
        return server.findInfo(deepFilePath);
    }

    @Benchmark
    public String lstat() throws RemoteException {
        return server.lstat(deepDirPath);
    }

    @Benchmark
    public String addStorageItems() throws RemoteException {
        // Re-adding existing paths replaces the nodes, so the tree size stays constant.
        for (final String path : itemPaths) {
            server.addStorageItem(path, "Machine2", false);
        }
        return deepFilePath;
    }

    @Benchmark
    public String giveMeAnID() {
        return server.giveMeAnID();
    }
}
