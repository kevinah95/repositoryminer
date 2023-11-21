package org.repositoryminer.scm;

import java.util.List;
import java.util.Set;

import org.repositoryminer.domain.Commit;
import org.repositoryminer.domain.Reference;
import org.repositoryminer.domain.SCMType;

/**
 * Interface that define what is needed to support a repository.
 */
public interface ISCM {

	/**
	 * @return The supported version control system type.
	 */
	public SCMType getSCM();

	/**
	 * Prepare the repository to analysis.
	 * 
	 * @param path
	 *            the repository path.
	 */
	public void open(String path);

	/**
	 * @return all references (e.g. tags and branches).
	 */
	public List<Reference> getReferences();

	/**
	 * Returns all the commits by a limited set of them per turn.
	 * 
	 * @param skip
	 *            how much commits should be skipped.
	 * @param max
	 *            max number of fetched commits.
	 * @return a set of commits;
	 */
	public List<Commit> getCommits(int skip, int max, boolean skip_content, String branchName);

	/**
	 * Returns the commits with hash in the given list of hashes.
	 * 
	 * @param selectedCommits
	 *            a list of commits hashes.
	 * @return a set of selected commits
	 */
	public List<Commit> getCommits(Set<String> selectedCommits, boolean skip_content);

	/**
	 * Gets the commit which the HEAD is pointed at.
	 * <p>
	 * <b>Note: For now, only these commit attributes need to be set: hash and
	 * committer date.</b>
	 * 
	 * @return the commit of the current HEAD location.
	 */
	public Commit getHEAD();

	/**
	 * Converts a reference to a commit to a commit object.
	 * <p>
	 * <b>Note: For now, only these commit attributes need to be set: hash and
	 * committer date.</b>
	 * 
	 * @return the referenced commit.
	 */
	public Commit resolve(String reference);

	/**
	 * @param reference
	 *            the reference.
	 * @return the hashes of commits associated to a reference.
	 */
	public List<String> getCommitsNames(Reference reference);

	/**
	 * @return the hashes of all commits.
	 */
	public List<String> getCommitsNames();

	/**
	 * Does a checkout to a given point.
	 * 
	 * @param point
	 *            the checkout point.
	 */
	public void checkout(String point);

	/**
	 * Terminates the analysis and free resources.
	 */
	public void close();

}
