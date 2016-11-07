import java.util.HashMap;

public class FileNode {
    public String name;
    private FileNode parentDir;

    public boolean isDirectory;
    public HashMap<String,FileNode> children;
    public String myStorageServer;

    public FileNode(String name, FileNode parentDir, boolean isDirectory, String myStorageServer) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.parentDir = parentDir;
        this.myStorageServer = myStorageServer;
        this.children = new HashMap<>();
    }

    public FileNode getParentDirectory() {
        return this.parentDir;
    }

    public void rename(String name) {
        this.name = name;
    }

}
