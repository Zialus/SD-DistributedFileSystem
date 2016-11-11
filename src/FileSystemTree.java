import java.util.Arrays;

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

    public void removeFromFileSystem(String fullPath) {
        PairBoolNode p = find(fullPath);

        if(!p.bool){
            System.out.println("ggfgfr " + fullPath);
        }
        System.out.println("vaidarmerda ->" + p.node.parentDir.name + "|||| no fim vem o filho " + p.node.name);
        p.node.parentDir.children.remove(p.node.name);
        System.out.println("deleting child-------" + p.node.name + " with parent " + p.node.parentDir.name);
    }

    public void printNode(FileNode node){

        while (node != null) {
            node.children.entrySet().forEach(entry -> {
                printNode(entry.getValue());
                System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue() + " Parent" + entry.getValue().getParentDirectory().name);
            });
        }

    }
    public void printTree(){

        printNode(root);

    }

    public PairBoolNode find(String path){

        if (path.equals("/")){
            return new PairBoolNode(true,root);
        }

        System.out.println("trying to find: " + path);
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



