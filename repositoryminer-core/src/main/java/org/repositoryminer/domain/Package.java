package org.repositoryminer.domain;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Package {
    private String name;
    private List<Class> classes;

    public Package() {
    }

    public Package(String name, List<Class> classes) {
        this.name = name;
        this.classes = classes;
    }

    public List<Class> getClasses() {
        return classes;
    }

    public void setClasses(List<Class> classes) {
        this.classes = classes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Converts documents to changes.
     *
     * @param documents
     * @return a list of changes.
     */
    public static List<Package> parseDocuments(List<Document> documents) {
        List<Package> packages = new ArrayList<Package>();
        if (documents == null)
            return packages;

        for (Document doc : documents) {
            Package method = new Package(doc.getString("name"), Class.parseDocuments(doc.get("classes", List.class)));
            packages.add(method);
        }
        return packages;
    }

    /**
     * Converts methods to documents.
     *
     * @param packages
     * @return a list of documents.
     */
    public static List<Document> toDocumentList(List<Package> packages) {
        List<Document> list = new ArrayList<Document>();
        for (Package p : packages) {
            Document doc = new Document();
            doc.append("name", p.getName())
                    .append("classes", Class.toDocumentList(p.classes));
            list.add(doc);
        }
        return list;
    }
}
