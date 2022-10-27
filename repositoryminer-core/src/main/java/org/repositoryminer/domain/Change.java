package org.repositoryminer.domain;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

/**
 * Represents a change made in a commit.
 */
public class Change {

    private String newPath;
    private String oldPath;
    private int linesAdded;
    private int linesRemoved;
    private ChangeType type;
    private String content;
    private String contentBefore;
    private int loc;
    private List<Method> methods;
    private int locBefore;
    private int cyclo;
    private int cycloBefore;
    private List<Package> packages;

    public Change() {
    }

    public List<Package> getPackages() {
        return packages;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    public Change(String newPath, String oldPath,
                  int linesAdded, int linesRemoved,
                  ChangeType type,
                  String content, String contentBefore,
                  int loc, int locBefore, int cyclo, int cycloBefore, List<Method> methods, List<Package> packages) {
        super();
        this.newPath = newPath;
        this.oldPath = oldPath;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.type = type;
        this.content = content;
        this.contentBefore = contentBefore;
        this.loc = loc;
        this.locBefore = locBefore;
        this.cyclo = cyclo;
        this.cycloBefore = cycloBefore;
        this.methods = methods;
        this.packages = packages;
    }

    /**
     * Converts documents to changes.
     *
     * @param documents
     * @return a list of changes.
     */
    public static List<Change> parseDocuments(List<Document> documents) {
        List<Change> changes = new ArrayList<Change>();
        if (documents == null)
            return changes;

        for (Document doc : documents) {
            Change change = new Change(doc.getString("new_path"), doc.getString("old_path"),
                    doc.getInteger("lines_added", 0), doc.getInteger("lines_removed", 0),
                    ChangeType.valueOf(doc.getString("type")),
                    doc.getString("content"), doc.getString("content_before"),
                    doc.getInteger("loc"), doc.getInteger("locBefore"),
                    doc.getInteger("cyclo"), doc.getInteger("cycloBefore"),
                    Method.parseDocuments(doc.get("methods", List.class)),
                    Package.parseDocuments(doc.get("packages", List.class)));
            changes.add(change);
        }
        return changes;
    }

    /**
     * Converts changes to documents.
     *
     * @param changes
     * @return a list of documents.
     */
    public static List<Document> toDocumentList(List<Change> changes) {
        List<Document> list = new ArrayList<Document>();
        for (Change c : changes) {
            Document doc = new Document();
            doc.append("new_path", c.getNewPath()).append("old_path", c.getOldPath())
                    .append("lines_added", c.getLinesAdded()).append("lines_removed", c.getLinesRemoved())
                    .append("type", c.getType().toString())
                    .append("content", c.getContent())
                    .append("content_before", c.getContentBefore())
                    .append("loc", c.getLoc())
                    .append("locBefore", c.getLocBefore())
                    .append("cyclo", c.getCyclo())
                    .append("cycloBefore", c.getCycloBefore())
                    .append("methods", Method.toDocumentList(c.getMethods()))
                    .append("packages", Package.toDocumentList(c.getPackages()));
            list.add(doc);
        }
        return list;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getLocBefore() {
        return locBefore;
    }

    public void setLocBefore(int locBefore) {
        this.locBefore = locBefore;
    }

    public int getCyclo() {
        return cyclo;
    }

    public void setCyclo(int cyclo) {
        this.cyclo = cyclo;
    }

    public int getCycloBefore() {
        return cycloBefore;
    }

    public void setCycloBefore(int cycloBefore) {
        this.cycloBefore = cycloBefore;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(int linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public ChangeType getType() {
        return type;
    }

    public void setType(ChangeType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentBefore() {
        return contentBefore;
    }

    public void setContentBefore(String contentBefore) {
        this.contentBefore = contentBefore;
    }

}