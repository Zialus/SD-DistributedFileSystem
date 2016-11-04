
import java.util.Arrays;
import java.util.HashMap;



public class FileSystemTree {

    public FileNode root; // root folder

    public FileSystemTree() {
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String StorageServer) {

        FileNode f = new FileNode(name, dirPath, isDir, StorageServer);
        System.out.println("added child -------" + f.name + " with parent " + dirPath.name);
        dirPath.children.put(f.name, f);

    }

    public Pair find(String path){
        System.out.println("trying to find: " + path);
        String[] pathParts = path.split("/");
        pathParts = Arrays.copyOfRange(pathParts, 1, pathParts.length);

        int pathPartsLeft = pathParts.length-1;
        FileNode currentNode = root;
        for (String part : pathParts) {

            System.out.println("part !!!!! " + part);
            currentNode = currentNode.children.get(part);
            if (currentNode == null){
                return new Pair(false, null);
            }
            if (currentNode.name.equals(part) && pathPartsLeft == 0 ){
                return new Pair(true, currentNode);
            }
            pathPartsLeft--;
        }

        return new Pair(false, null);
    }


}



