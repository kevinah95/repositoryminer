package org.repositoryminer.domain;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Class {
    String name;

    public Class(String name, List<Method> methods) {
        this.name = name;
        this.methods = methods;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public void setMethods(List<Method> methods) {
        this.methods = methods;
    }

    List<Method> methods;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class() {
    }

    /**
     * Converts documents to changes.
     *
     * @param documents
     * @return a list of changes.
     */
    public static List<Class> parseDocuments(List<Document> documents) {
        List<Class> classes = new ArrayList<Class>();
        if (documents == null)
            return classes;

        for (Document doc : documents) {
            Class aClass = new Class(doc.getString("name"), Method.parseDocuments(doc.get("methods", List.class)));
            classes.add(aClass);
        }
        return classes;
    }

    /**
     * Converts methods to documents.
     *
     * @param classes
     * @return a list of documents.
     */
    public static List<Document> toDocumentList(List<Class> classes) {
        List<Document> list = new ArrayList<Document>();
        for (Class c : classes) {
            Document doc = new Document();
            doc.append("name", c.getName())
                .append("methods", Method.toDocumentList(c.getMethods()));
            list.add(doc);
        }
        return list;
    }
}
