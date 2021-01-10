package fcup;

import lombok.extern.java.Log;

import java.util.Arrays;

@Log
public class FileSystemTree {

    public final FileNode root; // root folder

    public FileSystemTree() {
        this.root = new FileNode("/", null, true, null);
    }

    public void addToFileSystem(String name, FileNode dirPath, boolean isDir, String storageServer) {
        FileNode f = new FileNode(name, dirPath, isDir, storageServer);
        log.info("added child -> " + f.name + " -> with parent " + dirPath.name);
        dirPath.children.put(f.name, f);

    }

    public void removeFromFileSystem(String fullPath) {
        PairBoolNode p = find(fullPath);

        if (!p.bool) {
            log.info("You are trying to remove something that doesn't exist in the filesystem | Path-> " + fullPath);
        } else if (p.node.parentDir != null) {
            p.node.parentDir.children.remove(p.node.name);
        } else {
            log.info("I'm an orphan and my name is -> " + p.node.name);
            p.node = null;
        }
    }

    public PairBoolNode find(String path) {
        if ("/".equals(path)) {
            return new PairBoolNode(true, root);
        }

        log.info("Trying to find: " + path);
        String[] pathParts = path.split("/");
        pathParts = Arrays.copyOfRange(pathParts, 1, pathParts.length);

        int pathPartsLeft = pathParts.length - 1;
        FileNode currentNode = root;

        for (String part : pathParts) {
            currentNode = currentNode.children.get(part);
            if (currentNode == null) {
                return new PairBoolNode(false, null);
            }
            if (currentNode.name.equals(part) && (pathPartsLeft == 0)) {
                return new PairBoolNode(true, currentNode);
            }
            pathPartsLeft--;
        }

        return new PairBoolNode(false, null);
    }

}
