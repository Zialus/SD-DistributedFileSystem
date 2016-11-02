
import java.util.HashMap;



public class FileSystemTree {

    public FileNode root; // root folder

    public FileSystemTree() {
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String StorageServer) {

        FileNode f = new FileNode(name, dirPath, isDir, StorageServer);
        dirPath.children.put(f.name, f);

    }

    public Pair find(String path){
        System.out.println("AHLAAA AKBAR BOOOOOOOM");
        String[] pathParts = path.split("/");

        FileNode currentNode = root;
        for (String part : pathParts) {
            currentNode = currentNode.children.get(part);
            if (currentNode == null){
                return new Pair(false, null);
            }
            if (currentNode.name.equals(part)){
                return new Pair(true, currentNode);
            }
        }

        return new Pair(false, null);
    }


}



