

import java.util.HashMap;

public class FileSystemTree {

    private FileNode root; // root folder

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
        }

        public FileNode getParentDirectory() {
            return this.parentDir;
        }

        public void rename(String name) {
            this.name = name;
        }

    }

    public FileSystemTree() {
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String StorageServer) {

        FileNode f = new FileNode(name, dirPath, isDir, StorageServer);
        dirPath.children.put(f.name, f);

    }

    public Pair find(String path){

        String[] pathParts = path.split("/");

        FileNode currentNode = root;
        for (String part : pathParts) {
            currentNode = currentNode.children.get(part);
            if (currentNode == null){
                return new Pair(false, null);
            }
        }



        return new Pair(true, path);
    }


}
