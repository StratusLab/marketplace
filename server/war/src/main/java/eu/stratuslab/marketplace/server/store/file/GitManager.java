package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_URI;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_USER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.REPLICATION_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public class GitManager {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private Repository repository;
	
	private String gitUser;
	private String gitPassword;

	private String gitDir;
	
	public GitManager(String dataDir) {
		gitDir = dataDir;
		
		gitUser = Configuration.getParameterValue(GIT_USER);
		gitPassword = Configuration.getParameterValue(GIT_PASSWORD);
		
		if (!isGitRepository(new File(dataDir + File.separator + ".git"))) {
			LOGGER.warning("No git repository found.");
			cloneRepository();
		} else {
			pull();
		}
	}
	
	void addToRepo(String key){
		try {
			if (repository == null)
				openGitRepository();
			
			Git git = new Git(repository);
			git.add()
			.addFilepattern(key + ".xml")
			.call();
			
			commit("Added " + key);
			
			push();
		} catch (NoFilepatternException e) {
			LOGGER.warning("Unable to add file to git repository");
		} catch (GitAPIException e) {
			LOGGER.warning("Unable to add file to git repository");
		}
	}
	
	void removeFromRepo(String key){
		try {
			if (repository == null)
				openGitRepository();
			
			Git git = new Git(repository);
			git.rm()
			.setCached(true)
			.addFilepattern(key + ".xml")
			.call();
			
			push();
		} catch (NoFilepatternException e) {
			LOGGER.warning("Unable to remove file from index");
		} catch (GitAPIException e) {
			LOGGER.warning("Unable to remove file from index");
		}
	}
	
	void close(){
		if (repository != null){
			repository.close();
		}
	}
	
	void pull() {
		if (repository == null)
			openGitRepository();

		Git git = new Git(repository);
		GitCommand<?> pull = git.pull().setCredentialsProvider(
				new UsernamePasswordCredentialsProvider(gitUser, gitPassword));
		GitRunner runner = new GitRunner(pull);
		runner.start();
	}
	
	private static boolean isGitRepository(final File gitdir) {
	    return new File(gitdir, "config").isFile()
	        && new File(gitdir, "HEAD").isFile()
	        && new File(gitdir, "objects").isDirectory()
	        && new File(gitdir, "refs/heads").isDirectory();
	}
	
	private void cloneRepository() {
		GitCommand<Git> command = Git
				.cloneRepository()
				.setURI(Configuration.getParameterValue(GIT_URI))
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitUser,
								gitPassword)).setDirectory(new File(gitDir));

		GitRunner runner = new GitRunner(command);
		runner.start();
	}
	
	private void openGitRepository(){
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			repository = builder
			  .readEnvironment() // scan environment GIT_* variables
			  .findGitDir(new File(gitDir)) // scan up the file system tree
			  .build();
		} catch (IOException e) {
			LOGGER.severe("Unable to create git repository.");
		}
	}
	
	private void commit(String message){
		try {
			if (repository == null)
				openGitRepository();
			
			Git git = new Git(repository);
			git.commit()
			.setMessage(message)
			.call();
		} catch (NoHeadException e) {
			LOGGER.warning("Unable to commit changes.");
		} catch (NoMessageException e) {
			LOGGER.warning("Unable to commit changes.");
		} catch (UnmergedPathsException e) {
			LOGGER.warning("Unable to commit changes.");
		} catch (ConcurrentRefUpdateException e) {
			LOGGER.warning("Unable to commit changes.");
		} catch (WrongRepositoryStateException e) {
			LOGGER.warning("Unable to commit changes.");
		} catch (GitAPIException e) {
			LOGGER.warning("Unable to commit changes.");
		}
	}
	
	private void push() {
		if (Configuration.getParameterValue(REPLICATION_TYPE).equals("public")) {
			if (repository == null)
				openGitRepository();

			Git git = new Git(repository);
			GitCommand<?> command = git.push().setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(gitUser,
							gitPassword));
			GitRunner runner = new GitRunner(command);
			runner.start();
		}
	}
	
	public static class GitRunner extends Thread {
		
		private GitCommand<?> command;
		
		GitRunner(GitCommand<?> command){
			this.command = command;
		}
		
		public void run(){
			try {
				this.command.call();
			} catch (GitAPIException e) {
				LOGGER.severe("Unable to execute Git command: "  + e.getMessage());
			}
		}
		
	}

}
