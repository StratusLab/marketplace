package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.DATA_DIR;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_URI;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_USER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.REPLICATION_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
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
import org.w3c.dom.Document;

import eu.stratuslab.marketplace.server.cfg.Configuration;
import eu.stratuslab.marketplace.server.utils.MetadataFileUtils;

public class GitStore extends FileStore {

	private FileStore fileStore;
	private String dataDir;
	private String gitUser;
	private String gitPassword;
	private Repository repository;
	
	private HashMap<String, Document> localUpdates = new HashMap<String, Document>();
	
	private final ScheduledExecutorService repoUpdater = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> updaterHandle;
	
	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private FileMonitor monitor;
	
	public GitStore(FileStore store) {
		dataDir = Configuration.getParameterValue(DATA_DIR);
		gitUser = Configuration.getParameterValue(GIT_USER);
		gitPassword = Configuration.getParameterValue(GIT_PASSWORD);
		
		monitor = new FileMonitor(dataDir, ".xml");
		
		if (!isGitRepository(new File(dataDir + File.separator + ".git"))) {
			LOGGER.warning("No git repository found.");
			cloneRepository();
		} else {
			pull();
		}
		
		fileStore = store;
		
		final Runnable updater = new Runnable() {
        	public void run() {
        		LOGGER.info("Updating from git repository.");
        		pull();
        	}
        };
        
        updaterHandle = repoUpdater.scheduleWithFixedDelay(updater, 
    			5, 5, TimeUnit.MINUTES);
    }
	
	public static boolean isGitRepository(final File gitdir) {
	    return new File(gitdir, "config").isFile()
	        && new File(gitdir, "HEAD").isFile()
	        && new File(gitdir, "objects").isDirectory()
	        && new File(gitdir, "refs/heads").isDirectory();
	}
	
	private void cloneRepository(){
		GitCommand<Git> command = Git.cloneRepository() 
				   .setURI(Configuration.getParameterValue(GIT_URI))
				   .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUser, gitPassword))
				   .setDirectory(new File(dataDir));
		
		GitRunner runner = new GitRunner(command);
		runner.start();
	}
	
	private void pull(){
		if (repository == null)
			openGitRepository();

		Git git = new Git(repository);
		GitCommand<?> pull = git.pull()
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitUser, gitPassword));
		GitRunner runner = new GitRunner(pull);
		runner.start();
	}
	
	private void openGitRepository(){
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			repository = builder
			  .readEnvironment() // scan environment GIT_* variables
			  .findGitDir(new File(dataDir)) // scan up the file system tree
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
	
	private void push(){
		if(Configuration.getParameterValue(REPLICATION_TYPE).equals("public")){
			if (repository == null)
				openGitRepository();

			Git git = new Git(repository);
			GitCommand<?> command = git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitUser, gitPassword));
			GitRunner runner = new GitRunner(command);
			runner.start();
		}
	}
	
	private String getKeyFromPath(String path){
		String[] elements = path.split(File.separatorChar=='\\' ? "\\\\" : File.separator);
		int length = elements.length;
		String key = elements[length - 3] + "/" + elements[length - 2] + "/" + FilenameUtils.getBaseName(elements[length - 1]);
		
		return key;
	}
	
	@Override
	public void store(String key, Document metadata) {
		fileStore.store(key,  metadata);
		localUpdates.put(key, metadata);
		
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

	@Override
	public void remove(String key) {
		try {
			if (repository == null)
				openGitRepository();
			
			Git git = new Git(repository);
			git.rm()
			.setCached(true)
			.addFilepattern(key + ".xml")
			.call();
			
			push();
			
			fileStore.remove(key);
		} catch (NoFilepatternException e) {
			LOGGER.warning("Unable to remove file from index");
		} catch (GitAPIException e) {
			LOGGER.warning("Unable to remove file from index");
		}
	}

	@Override
	public String read(String key) {
		return fileStore.read(key);
	}

	@Override
	public List<String> updates(int limit) {
		List<String> updates = new ArrayList<String>();
		int i = 0;
		
		File file = monitor.getFile();
		
		while(i < limit && file != null){
			try {
				String key = getKeyFromPath(file.getCanonicalPath());
												
				if (!localUpdates.containsKey(key)) {
					String document = MetadataFileUtils.
						readFileAsString(file.getCanonicalPath());
					updates.add(document);
			
					i++;
				} else {
					localUpdates.remove(key);
				}
				file = monitor.getFile();
					
			} catch (IOException e) {
				LOGGER.severe("Unable to read file: " + e.getMessage());
			}
		}
		
		return updates;
	}

	@Override
	public void shutdown() {
		repository.close();
		fileStore.shutdown();
		updaterHandle.cancel(true);
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
