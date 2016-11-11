package FCUP;

import java.util.HashMap;

public class FileNode {
    public final String name;
    public final FileNode parentDir;

    public final boolean isDirectory;
    public final HashMap<String,FileNode> children;
    public String myStorageServer;

    public FileNode(String name, FileNode parentDir, boolean isDirectory, String myStorageServer) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.parentDir = parentDir;
        this.myStorageServer = myStorageServer;
        this.children = new HashMap<>();
    }

}
