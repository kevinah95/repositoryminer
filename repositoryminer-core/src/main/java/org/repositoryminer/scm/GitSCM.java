package org.repositoryminer.scm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import ASTMCore.ASTMSource.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gastmappers.Language;
import gastmappers.Mapper;
import gastmappers.MapperFactory;
import gastmappers.exceptions.UnsupportedLanguageException;
import metrics.*;
import metrics.examcompletemetric.MetricClass;
import metrics.examcompletemetric.MetricMethod;
import metrics.examcompletemetric.MetricPackage;
import metrics.exceptions.UnsupportedMetricException;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.json.simple.JSONArray;
import org.repositoryminer.RepositoryMinerException;
import org.repositoryminer.domain.Change;
import org.repositoryminer.domain.ChangeType;
import org.repositoryminer.domain.Commit;
import org.repositoryminer.domain.Developer;
import org.repositoryminer.domain.Reference;
import org.repositoryminer.domain.ReferenceType;
import org.repositoryminer.domain.SCMType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements support for Git repositories.
 */
public class GitSCM implements ISCM {

	private static final Logger LOG = LoggerFactory.getLogger(GitSCM.class);

	private Git git;
	private int branchCounter = 0;

	@Override
	public SCMType getSCM() {
		return SCMType.GIT;
	}

	@Override
	public void open(String path) {
		LOG.info("Repository being opened.");

		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		File repoFolder = new File(path, ".git");

		if (!repoFolder.exists()) {
			throw new RepositoryMinerException("Repository not found.");
		}

		try {
			Repository repository = repositoryBuilder.setGitDir(repoFolder).readEnvironment().findGitDir().build();
			git = new Git(repository);
		} catch (IOException e) {
			throw new RepositoryMinerException(e);
		}
	}

	@Override
	public List<Reference> getReferences() {
		LOG.info("Extracting references.");

		List<Reference> refs = new ArrayList<Reference>();
		Iterable<Ref> branches = null;

		try {
			branches = git.branchList().call();
		} catch (GitAPIException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		for (Ref b : branches) {
			if (b.getName().equals("HEAD")) {
				continue;
			}

			int i = b.getName().lastIndexOf("/") + 1;
			Commit commit = resolve(b.getName());
			Reference r = new Reference(null, null, b.getName().substring(i), b.getName(), ReferenceType.BRANCH,
					commit.getCommitterDate(), null);
			refs.add(r);
			LOG.info(String.format("Branch %s analyzed.", r.getName()));
		}

		Iterable<Ref> tags = null;
		try {
			tags = git.tagList().call();
		} catch (GitAPIException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		for (Ref t : tags) {
			int i = t.getName().lastIndexOf("/") + 1;
			Commit commit = resolve(t.getName());
			Reference r = new Reference(null, null, t.getName().substring(i), t.getName(), ReferenceType.TAG,
					commit.getCommitterDate(), null);
			refs.add(r);
			LOG.info(String.format("Tag %s analyzed.", r.getName()));
		}

		return refs;
	}

	@Override
	public List<Commit> getCommits(int skip, int max) {
		LOG.info("Extracting commits.");

		List<Commit> commits = new ArrayList<Commit>();
		try {
			for (RevCommit revCommit : git.log().all().setSkip(skip).setMaxCount(max).call()) {
				LOG.info(String.format("Analyzing commit %s.", revCommit.getName()));
				commits.add(processCommit(revCommit));
			}
		} catch (GitAPIException | IOException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		return commits;
	}

	@Override
	public List<Commit> getCommits(Set<String> selectedCommits) {
		LOG.info("Extracting commits.");
		List<Commit> commits = new ArrayList<Commit>();
		
		try {
			for (RevCommit revCommit : git.log().all().call()) {
				if (selectedCommits.contains(revCommit.getName())) {
					LOG.info(String.format("Analyzing commit %s.", revCommit.getName()));
					commits.add(processCommit(revCommit));
				}
			}
		} catch (GitAPIException | IOException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		return commits;
	}

	@Override
	public Commit getHEAD() {
		return resolve(Constants.HEAD);
	}

	@Override
	public Commit resolve(String reference) {
		RevWalk revWalk = null;

		try {
			ObjectId ref = git.getRepository().resolve(reference);
			revWalk = new RevWalk(git.getRepository());
			RevCommit revCommit = revWalk.parseCommit(ref);

			return new Commit(revCommit.getName(), revCommit.getCommitterIdent().getWhen());
		} catch (RevisionSyntaxException | IOException e) {
			throw new RepositoryMinerException("Error getting the commit from " + reference + ".", e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}

	@Override
	public List<String> getCommitsNames(Reference reference) {
		LOG.info(String.format("Extracting the commits names from reference %s.", reference.getName()));

		Iterable<RevCommit> revCommits;
		if (reference.getType() == ReferenceType.BRANCH) {
			revCommits = getCommitsFromBranch(reference.getName());
		} else {
			revCommits = getCommitsFromTag(reference.getName());
		}

		if (revCommits == null) {
			return new ArrayList<String>();
		}

		List<String> names = new ArrayList<String>();
		for (RevCommit revCommit : revCommits) {
			names.add(revCommit.getName());
		}

		return names;
	}

	@Override
	public List<String> getCommitsNames() {
		LOG.info(String.format("Extracting the commits names"));

		List<String> names = new ArrayList<String>();

		try {
			for (RevCommit revCommit : git.log().all().call()) {
				names.add(revCommit.getName());
			}
		} catch (GitAPIException | IOException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		return names;
	}

	@Override
	public void checkout(String hash) {
		LOG.info(String.format("Checking out %s.", hash));
		File lockFile = new File(git.getRepository().getDirectory(), "git/index.lock");
		if (lockFile.exists()) {
			lockFile.delete();
		}

		try {
			git.reset().setMode(ResetType.HARD).call();
			git.checkout().setName("master").call();

			git.checkout().setCreateBranch(true).setName("rm_branch" + branchCounter++).setStartPoint(hash)
					.setForce(true).setOrphan(true).call();
		} catch (GitAPIException e) {
			close();
			throw new RepositoryMinerException(e);
		}
	}

	@Override
	public void close() {
		LOG.info("Repository being closed.");
		git.getRepository().close();
		git.close();
	}

	private Commit processCommit(RevCommit revCommit) {
		PersonIdent author = revCommit.getAuthorIdent();
		PersonIdent committer = revCommit.getCommitterIdent();

		Developer myAuthor = new Developer(author.getName(), author.getEmailAddress());
		Developer myCommitter = new Developer(committer.getName(), committer.getEmailAddress());

		List<String> parents = new ArrayList<String>();
		for (RevCommit parent : revCommit.getParents()) {
			parents.add(parent.getName());
		}

		List<Change> changes = null;
		try {
			changes = getChangesForCommitedFiles(revCommit.getName());
		} catch (IOException | UnsupportedLanguageException | UnsupportedMetricException | SQLException |
				 ClassNotFoundException e) {
			close();
			throw new RepositoryMinerException(e);
		}

		return new Commit(null, revCommit.getName(), myAuthor, myCommitter, revCommit.getFullMessage().trim(), changes,
				parents, author.getWhen(), committer.getWhen(), (parents.size() > 1), null);
	}

	private List<Change> getChangesForCommitedFiles(String hash) throws IOException, UnsupportedLanguageException, UnsupportedMetricException, SQLException, ClassNotFoundException {
		RevWalk revWalk = new RevWalk(git.getRepository());
		RevCommit commit = revWalk.parseCommit(ObjectId.fromString(hash));

		if (commit.getParentCount() > 1) {
			revWalk.close();
			return new ArrayList<Change>();
		}

		RevCommit parentCommit = commit.getParentCount() > 0
				? revWalk.parseCommit(ObjectId.fromString(commit.getParent(0).getName()))
				: null;

		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setBinaryFileThreshold(2048);
		df.setRepository(git.getRepository());
		df.setDiffComparator(RawTextComparator.DEFAULT);
		df.setDetectRenames(true);

		List<DiffEntry> diffEntries = df.scan(parentCommit, commit);
		df.close();
		revWalk.close();

		List<Change> changes = new ArrayList<Change>();
		for (DiffEntry entry : diffEntries) {
			String content = "";
			String contentBefore = "";

			int loc = 0;
			int locBefore = 0;
			if(entry.getNewPath() != DiffEntry.DEV_NULL){
				content = getCommitContent(commit, entry.getNewPath());
				String filename = getFilename(entry.getNewPath(), entry.getOldPath());
				String extension = FilenameUtils.getExtension(filename);
				if(extension.equals("java")){
					loc = executeAnalyzer(content, Language.JAVA);
				}
			}

			if(entry.getOldPath() != DiffEntry.DEV_NULL){
				contentBefore = getCommitContent(parentCommit, entry.getOldPath());
				String filename = getFilename(entry.getNewPath(), entry.getOldPath());
				String extension = FilenameUtils.getExtension(filename);
				if(extension.equals("java")){
					locBefore = executeAnalyzer(contentBefore, Language.JAVA);
				}
			}



			Change change = new Change(entry.getNewPath(), entry.getOldPath(), 0, 0,
					ChangeType.valueOf(entry.getChangeType().name()), content, contentBefore, loc, locBefore);

			analyzeDiff(change, entry);
			changes.add(change);
		}

		return changes;
	}

	private String getFilename(String newPath, String oldPath){
		String path;
		if(newPath != null && newPath != DiffEntry.DEV_NULL){
			path = newPath;
		}else{
			path = oldPath;
		}

		return FilenameUtils.getName(path);
	}

	private String getCommitContent(RevCommit commit, String path) throws IOException {
		try (TreeWalk treeWalk = TreeWalk.forPath(git.getRepository(), path, commit.getTree())) {
			ObjectId blobId = treeWalk.getObjectId(0);
			try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
				ObjectLoader objectLoader = objectReader.open(blobId);
				byte[] bytes = objectLoader.getBytes();
				return new String(bytes, StandardCharsets.UTF_8);
			}
		}
	}

	private void analyzeDiff(Change change, DiffEntry diff) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DiffFormatter df = new DiffFormatter(output);

		df.setRepository(git.getRepository());
		df.format(diff);

		Scanner scanner = new Scanner(output.toString("UTF-8"));
		int added = 0;
		int removed = 0;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("+") && !line.startsWith("+++")) {
				added++;
			} else if (line.startsWith("-") && !line.startsWith("---")) {
				removed++;
			}
		}

		output.close();
		df.close();
		scanner.close();

		change.setLinesAdded(added);
		change.setLinesRemoved(removed);
	}

	private Iterable<RevCommit> getCommitsFromTag(String refName) {
		try {
			List<Ref> call = git.tagList().call();
			for (Ref ref : call) {
				if (ref.getName().endsWith(refName)) {
					LogCommand log = git.log();
					Ref peeledRef = git.getRepository().peel(ref);
					if (peeledRef.getPeeledObjectId() != null) {
						return log.add(peeledRef.getPeeledObjectId()).call();
					} else {
						return log.add(ref.getObjectId()).call();
					}
				}
			}
			return null;
		} catch (GitAPIException | IncorrectObjectTypeException | MissingObjectException e) {
			close();
			throw new RepositoryMinerException(e);
		}
	}

	private Iterable<RevCommit> getCommitsFromBranch(String refName) {
		try {
			return git.log().add(git.getRepository().resolve(refName)).call();
		} catch (RevisionSyntaxException | GitAPIException | IOException e) {
			close();
			throw new RepositoryMinerException(e);
		}
	}

	private int executeAnalyzer(String sourceCode, Language language) throws UnsupportedMetricException, IOException, UnsupportedLanguageException, SQLException, ClassNotFoundException {
		MetricFactory metricFactory = new MetricFactory();
		//Get the metric from JSON.
		MetricEnum newMetricEnum = MetricEnum.getMetricFromString(MetricEnum.LOC.toString());
		//Get the Metric from the Factory.
		AbstractMetric specificMetric = metricFactory.createMetric(newMetricEnum);
		//Each mapper knows how to process the inputParameters
		JSONArray inputParameters = new JSONArray();
		AbstractInput input = specificMetric.createSpecificInput(inputParameters);

		ArrayList<OutputMapperObject> CUGast = new ArrayList<>();
		//Process a single file
		OutputMapperObject fileCU = readFromSpecificLanguage(sourceCode,  language);
		//Add the file
		CUGast.add(fileCU);

		ArrayList<ArrayList<ArrayList<String>>> pathsJSON = new ArrayList<ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<ArrayList<CompilationUnit>>> gastObjects = new ArrayList<ArrayList<ArrayList<CompilationUnit>>>();

		ArrayList<ArrayList<String>> jsonAux = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<CompilationUnit>> gastAux = new ArrayList<ArrayList<CompilationUnit>>();
		for (OutputMapperObject theOutputObject : CUGast) {
			jsonAux.add(theOutputObject.gastAsJson);
			gastAux.add(theOutputObject.gastAsObject);
		}

		pathsJSON.add(jsonAux);
		gastObjects.add(gastAux);

		input.gastJsonInputs = pathsJSON;
		input.gastObjects = gastObjects;
		input.language = language;

		specificMetric.start(input);
		Output output = specificMetric.exportOutput();
		//Get the output as a file.
		ArrayList<MetricPackage> metricResult  = output.getMetricResult();

		int loc = 0;
		for(MetricPackage p : metricResult){
			for(MetricClass c: p.getPackageClasses()){
				for(MetricMethod m: c.getMethods()){
					loc += m.getLOC();
				}
			}
		}

		//String ttJsonTemp = (new GsonBuilder()).setPrettyPrinting().create().toJson(metricResult);
		return loc;
	}

	private OutputMapperObject readFromSpecificLanguage(String sourceCode, Language language) throws UnsupportedLanguageException, IOException {
		ArrayList<String> fileCUGast = new ArrayList<>();
		// Instance the mapper factory.
		MapperFactory factory = new MapperFactory();

		// Build the Java mapper.
		Mapper mapper = factory.createMapper(language);
		// Parse the file and obtain the GAST.
		ArrayList<CompilationUnit> compilationUnits;

		// Return the GAST from a file content.
		compilationUnits = mapper.getGastCompilationUnitInMemory(sourceCode);

		// Add the results in the parsed file list.
		for (CompilationUnit compilationUnit : compilationUnits) {
			// Transform the compilation unit into its JSON representation.
			Gson gson = new Gson();
			String jsonRepresentation = gson.toJson(compilationUnit);

			// Remove the "null"'s values into a empty string.
			jsonRepresentation = jsonRepresentation.replaceAll("null", "");
			//Add the JSON
			fileCUGast.add(jsonRepresentation);
		}
		OutputMapperObject newOutput = new OutputMapperObject();
		newOutput.gastAsObject = compilationUnits;
		newOutput.gastAsJson = fileCUGast;
		return newOutput;
	}

}

class OutputMapperObject {
	public ArrayList<String> gastAsJson;
	public ArrayList<CompilationUnit> gastAsObject;

}