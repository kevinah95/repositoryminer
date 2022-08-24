package org.repositoryminer.domain;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a change made in a commit.
 */
public class Method {
	private String name;
	private int loc;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLoc() {
		return loc;
	}

	public void setLoc(int loc) {
		this.loc = loc;
	}

	public int getComplexity() {
		return complexity;
	}

	public void setComplexity(int complexity) {
		this.complexity = complexity;
	}

	private int complexity;

	/**
	 * Converts documents to changes.
	 *
	 * @param documents
	 *
	 * @return a list of changes.
	 */
	public static List<Method> parseDocuments(List<Document> documents) {
		List<Method> methods = new ArrayList<Method>();
		if (documents == null)
			return methods;

		for (Document doc : documents) {
			Method method = new Method(doc.getString("name"), doc.getInteger("loc", 0),
					doc.getInteger("complexity", 0));
			methods.add(method);
		}
		return methods;
	}

	/**
	 * Converts methods to documents.
	 *
	 * @param methods
	 *
	 * @return a list of documents.
	 */
	public static List<Document> toDocumentList(List<Method> methods) {
		List<Document> list = new ArrayList<Document>();
		for (Method c : methods) {
			Document doc = new Document();
			doc.append("name", c.getName()).append("loc", c.getLoc())
					.append("complexity", c.getComplexity());
			list.add(doc);
		}
		return list;
	}

	public Method() {
	}

	public Method(String name, int loc, int complexity) {
		this.name = name;
		this.loc = loc;
		this.complexity = complexity;
	}
}