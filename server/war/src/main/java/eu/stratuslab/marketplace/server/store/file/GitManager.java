package eu.stratuslab.marketplace.server.store.file;

import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_URI;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_PASSWORD;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_USER;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_KEY;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_KEY_PASSPHRASE;
import static eu.stratuslab.marketplace.server.cfg.Parameter.GIT_KNOWN_HOSTS;
import static eu.stratuslab.marketplace.server.cfg.Parameter.REPLICATION_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import eu.stratuslab.marketplace.server.cfg.Configuration;

public class GitManager {

	private static final Logger LOGGER = Logger.getLogger("org.restlet");
	
	private Repository repository;
	
	private String gitUser;
	private String gitPassword;
	private String gitKey;
	private String gitKeyPass;
	private String gitKnownHosts;
	
	private String gitDir;
	
	private boolean keyAuth = false;
	
	private final ScheduledExecutorService repoUpdater = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> updaterHandle;
	
	public GitManager(String dataDir) {
		gitDir = dataDir;
		
		gitUser = Configuration.getParameterValue(GIT_USER);
		gitPassword = Configuration.getParameterValue(GIT_PASSWORD);
		
		if (gitUser == null || gitPassword == null){
			keyAuth = true;
		
			gitKey = Configuration.getParameterValue(GIT_KEY);
			gitKeyPass = Configuration.getParameterValue(GIT_KEY_PASSPHRASE);
			gitKnownHosts = Configuration.getParameterValue(GIT_KNOWN_HOSTS);
			
			initKeyAuth();
		}
		
		if (!isGitRepository(new File(gitDir + File.separator + ".git"))) {
			LOGGER.warning("No git repository found.");
			cloneRepository();
		} else {
			pull();
		}
		
		final Runnable updater = new Runnable() {
        	public void run() {
        		LOGGER.info("Updating from git repository.");
        		pull();
        	}
        };
        
        updaterHandle = repoUpdater.scheduleWithFixedDelay(updater, 
    			5, 5, TimeUnit.MINUTES);
	}
	
	private void initKeyAuth() {
		JSch jsch = new JSch();
	    try {
	        jsch.addIdentity(gitKey);
	        jsch.setKnownHosts(gitKnownHosts);
	    } catch (JSchException e) {
	        LOGGER.severe("Error configuring git key authentication: " + e.getMessage());  
	    }
	    JschConfigSessionFactory sessionFactory = new JschConfigSessionFactory() {
	    	@Override
	    	protected void configure(OpenSshConfig.Host hc, Session session) {
	    	    CredentialsProvider provider = new CredentialsProvider() {
	    	        @Override
	    	        public boolean isInteractive() {
	    	            return false;
	    	        }

	    	        @Override
	    	        public boolean supports(CredentialItem... items) {
	    	            return true;
	    	        }

	    	        @Override
	    	        public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
	    	            for (CredentialItem item : items) {
	    	                ((CredentialItem.StringType) item).setValue(gitKeyPass);
	    	            }
	    	            return true;
	    	        }
	    	    };
	    	    UserInfo userInfo = new CredentialsProviderUserInfo(session, provider);
	    	    session.setUserInfo(userInfo);
	    	}
	    	};
	    SshSessionFactory.setInstance(sessionFactory);
	}
	
	void addToRepo(String key){
		try {
			
			if (repository == null)
				openGitRepository();
			
			Git git = new Git(repository);
			git.add()
			.addFilepattern(key + ".xml")
			.call();
			
			Status status = git.status().call();
			if(!status.getAdded().isEmpty()){
				commit("Added " + key);
				push();
			}
			
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
		
		updaterHandle.cancel(true);
	}
	
	private void pull() {
		if (repository == null)
			openGitRepository();

		Git git = new Git(repository);
		PullCommand pull = git.pull();
		
		if(!keyAuth){
			pull.setCredentialsProvider(
				new UsernamePasswordCredentialsProvider(gitUser, gitPassword));
		}
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
		LOGGER.info("Cloning repo");
		CloneCommand command = Git
				.cloneRepository()
				.setURI(Configuration.getParameterValue(GIT_URI)).setDirectory(new File(gitDir));
		
		if(!keyAuth){
				command.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitUser,
						gitPassword));
		}

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
			PushCommand command = git.push();
			
			if(!keyAuth) {
				command.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(gitUser,
						gitPassword));
			}
			
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
