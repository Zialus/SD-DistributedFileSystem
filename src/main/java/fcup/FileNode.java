package fcup;

import java.util.HashMap;
import java.util.Map;

public class FileNode {

    public final String name;

    public final FileNode parentDir;

    public final boolean isDirectory;

    public final Map<String, FileNode> children;

    public String myStorageServer;

    public FileNode(String name, FileNode parentDir, boolean isDirectory, String myStorageServer) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.parentDir = parentDir;
        this.myStorageServer = myStorageServer;
        this.children = new HashMap<>();
    }

}
