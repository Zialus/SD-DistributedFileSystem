package fcup;

import java.util.Arrays;

public class FileSystemTree {

    public final FileNode root; // root folder

    public FileSystemTree() {
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String StorageServer) {

        FileNode f = new FileNode(name, dirPath, isDir, StorageServer);
        System.out.println("added child -> " + f.name + " -> with parent " + dirPath.name);
        dirPath.children.put(f.name, f);

    }

    public void removeFromFileSystem(String fullPath) {
        PairBoolNode p = find(fullPath);

        if(!p.bool){
            System.out.println("You are trying to remove something that doesn't exist in the filesystem | Path-> " + fullPath);
        } else if (p.node.parentDir != null){
            p.node.parentDir.children.remove(p.node.name);
        } else {
            System.out.println("I'm an orphan and my name is -> " + p.node.name);
            p.node = null;
        }
    }

    public PairBoolNode find(String path){
        if ("/".equals(path)){
            return new PairBoolNode(true,root);
        }

        System.out.println("Trying to find: " + path);
        String[] pathParts = path.split("/");
        pathParts = Arrays.copyOfRange(pathParts, 1, pathParts.length);

        int pathPartsLeft = pathParts.length-1;
        FileNode currentNode = root;
        for (String part : pathParts) {

            System.out.println("part !!!!! " + part);
            currentNode = currentNode.children.get(part);
            if (currentNode == null){
                return new PairBoolNode(false, null);
            }
            if (currentNode.name.equals(part) && pathPartsLeft == 0 ){
                return new PairBoolNode(true, currentNode);
            }
            pathPartsLeft--;
        }

        return new PairBoolNode(false, null);
    }


}
