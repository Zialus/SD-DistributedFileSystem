package fcup;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.logging.Logger;

/**
 * Benchmarks for the in-memory metadata tree used by the MetadataServer.
 */
@State(Scope.Benchmark)
public class FileSystemTreeBenchmark {

    private static final int FILES_PER_DIRECTORY = 64;

    @Param({"4", "16"})
    public int depth;

    private FileSystemTree tree;

    private String deepFilePath;

    private String deepDirPath;

    private String missingPath;

    @Setup(Level.Trial)
    public void setup() {
        BenchmarkSupport.silenceLogging();
        tree = buildTree(depth);

        final StringBuilder dir = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            dir.append("/dir").append(i);
        }
        deepDirPath = dir.toString();
        deepFilePath = deepDirPath + "/file" + (FILES_PER_DIRECTORY - 1) + ".txt";
        missingPath = deepDirPath + "/does-not-exist.txt";
    }

    /**
     * Builds a tree with {@code depth} nested directories, each containing
     * {@link #FILES_PER_DIRECTORY} files.
     */
    static FileSystemTree buildTree(final int depth) {
        final FileSystemTree t = new FileSystemTree();
        FileNode current = t.root;
        for (int i = 0; i < depth; i++) {
            for (int f = 0; f < FILES_PER_DIRECTORY; f++) {
                t.addToFileSystem("file" + f + ".txt", current, false, "Machine1");
            }
            final String dirName = "dir" + i;
            t.addToFileSystem(dirName, current, true, "Machine1");
            current = current.children.get(dirName);
        }
        for (int f = 0; f < FILES_PER_DIRECTORY; f++) {
            t.addToFileSystem("file" + f + ".txt", current, false, "Machine1");
        }
        return t;
    }

    @Benchmark
    public PairBoolNode findDeepFile() {
        return tree.find(deepFilePath);
    }

    @Benchmark
    public PairBoolNode findDeepDirectory() {
        return tree.find(deepDirPath);
    }

    @Benchmark
    public PairBoolNode findMissing() {
        return tree.find(missingPath);
    }

    @Benchmark
    public FileSystemTree buildTree() {
        return buildTree(depth);
    }

    @Benchmark
    public PairBoolNode addAndRemoveFile() {
        final PairBoolNode dir = tree.find(deepDirPath);
        tree.addToFileSystem("tmp.txt", dir.node, false, "Machine2");
        tree.removeFromFileSystem(deepDirPath + "/tmp.txt");
        return dir;
    }

    static final class BenchmarkSupport {
        private BenchmarkSupport() {
        }

        /**
         * The tree logs every lookup at INFO level; keep console I/O out of the measurements.
         */
        static void silenceLogging() {
            Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);
        }
    }
}
