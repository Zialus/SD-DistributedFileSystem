import java.util.ArrayList;

public class FileSystemTree {

    private FileNode root; // root folder

    public class FileNode {
        public String name;
        private FileNode parentDir;

        public boolean isDirectory;
        public ArrayList<FileNode> children;
        public String myStorageServer;

        public FileNode(String name, FileNode parentDir, boolean isDir, String myStorageServer) {
            this.name = name;
            this.isDirectory = isDir;
            this.parentDir = parentDir;
            this.myStorageServer = myStorageServer;
            if (isDir) {
                this.children = new ArrayList<FileNode>();
            }
        }

        public FileNode getParentDirectory() {
            return this.parentDir;
        }

        public void rename(String name) {
            this.name = name;
        }

    }

    public FileSystemTree() {
        // every file system should have a root folder
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String StorageServer) {

        FileNode f = new FileNode(name, dirPath, isDir, StorageServer);
        dirPath.children.add(f);

    }


}
