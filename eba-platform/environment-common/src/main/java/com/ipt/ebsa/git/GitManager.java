package com.ipt.ebsa.git;

import java.io.Console;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidMergeHeadsException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Usage: Instantiate with default constructor, then you must call one of {@link #gitClone(String, File, String, boolean)},
 * {@link #gitClone(String, String, String, boolean)}, {@link #gitInit(File)} before any of the other methods.
 * @author James Shepherd
 */
public class GitManager {

	public static final String	BASE_STARTING_POINT		= "refs/heads/";
	public static final String	MASTER_STARTING_POINT	= GitManager.BASE_STARTING_POINT + "master";
	private static final Logger	LOG = LogManager.getLogger(GitManager.class);
	private static final String NOTHING_TO_PUSH_ERR_MSG = "Nothing to push.";
	
	private String username = null;
	private String password = null;
	private boolean isInteractivePasswordEnabled = false;
	private Git git = null;
	private String knownHostsPath;
	private String identityfilePath;
	
	/**
	 * git clone
	 * @param remoteUrl
	 * @param localPath
	 * @param branchName
	 * @param bare
	 */
	public void gitClone(String remoteUrl, final File localPath, String branchName, boolean bare) {
		LOG.debug("Cloning remote uri [" + remoteUrl + "] to local path [" + localPath + "]");
		CloneCommand cc = Git.cloneRepository().setURI(remoteUrl).setDirectory(localPath);
		addCredentials(cc);
		cc.setBranch(branchName);
		cc.setRemote(Constants.DEFAULT_REMOTE_NAME);
		cc.setBare(bare);
		if (identityfilePath != null && knownHostsPath != null) {
			final SshSessionFactory sessionFactory = new JschConfigSessionFactory() {
				
				@Override
				protected void configure(Host hc, Session session) {
				}
				
				@Override
				protected JSch createDefaultJSch(FS fs) throws JSchException {
					LOG.debug("Using configured private key at [" + identityfilePath+ "] and knownhosts file [" + knownHostsPath + "]");
					JSch defaultJSch = super.createDefaultJSch(fs);
					defaultJSch.addIdentity(identityfilePath);
					defaultJSch.setKnownHosts(knownHostsPath);
					return defaultJSch;
				}
			};
			
			cc.setTransportConfigCallback(new TransportConfigCallback() {
				
				@Override
				public void configure(Transport transport) {
					if (transport instanceof SshTransport) {
						((SshTransport) transport).setSshSessionFactory(sessionFactory);
					}
					// Won't be a SshTransport if we're running against a local git directory, for example 
				}
			});
		}
		
		try {
			setGit(cc.call());
		} catch (InvalidRemoteException e) {
			throw new RuntimeException("Failed to git clone", e);
		} catch (TransportException e) {
			throw new RuntimeException("Failed to git clone", e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to git clone", e);
		}
	}

	/**
	 * git clone
	 * @param remoteUrl
	 * @param localPath
	 * @param branchName
	 * @param bare
	 */
	public void gitClone(String remoteUrl, final String localPath, String branchName, boolean bare) {
		gitClone(remoteUrl, new File(localPath), branchName, bare);
	}
	
	
	public void setIdentityFile(String identityfilePath) {
		this.identityfilePath = identityfilePath;
	}
	
	public void setKnownHosts(String knownHostsPath) {
		this.knownHostsPath = knownHostsPath;
	}
	
	/**
	 * git init
	 * @param workingDir
	 */
	public void gitInit(File workingDir) {
		workingDir = createDirIfNull(workingDir);
		
		try {
			setGit(Git.init().setDirectory(workingDir).call());
			LOG.debug("Initialised local git repo at " + workingDir);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to init git repo at: " + workingDir, e);
		}
	}
	
	/**
	 * @return the working dir of this git repo
	 */
	public File getWorkingDir() {
		if (null != getGit()) {
			return getGit().getRepository().getWorkTree();
		}
		
		return null;
	}
	
	/**
	 * Adds credentials to the transport command if some have been set up in the config.
	 * If the password is not set and the configuration is such to allow interactive password provision 
	 * then accept a password from system.in  
	 * @param cc
	 * @see #setInteractivePasswordEnabled(boolean)
	 * @see #setPassword(String)
	 * @see #setUsername(String)
	 */
	protected void addCredentials(TransportCommand<?, ?> cc) {
		if (getUsername() != null) {
			String password = getPassword();
			if ( password == null && isInteractivePasswordEnabled()) {
				Console c = System.console();
		        if (c == null) {
		            System.err.println("No console.");
		        }
		        else {
		           char[] pwd = c.readPassword("Enter your GIT password: ");
		           password = new String(pwd);
		        }
			}
			cc.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getUsername(), password));
		}
	}

	/**
	 * git commit
	 * @param message
	 */
	public void commit(String message) {
		LOG.debug("Commiting with message [" + message + "]");
		try {
			checkGit();
			getGit().commit().setMessage(message).call();
		} catch (NoHeadException e) {
			throw new RuntimeException("Failed to commit", e);
		} catch (NoMessageException e) {
			throw new RuntimeException("Failed to commit", e);
		} catch (UnmergedPathsException e) {
			throw new RuntimeException("Failed to commit", e);
		} catch (ConcurrentRefUpdateException e) {
			throw new RuntimeException("Failed to commit", e);
		} catch (WrongRepositoryStateException e) {
			throw new RuntimeException("Failed to commit", e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to commit", e);
		}
	}
	
	private void checkGit() {
		if (null == getGit()) {
			throw new RuntimeException("You must call gitClone or gitInit before other operations");
		}
	}

	/**
	 * git pull
	 */
	public void pull() {
		try {
			checkGit();
			LOG.debug("Pulling from " + getGit().getRepository().getBranch());
			PullCommand pull = getGit().pull();
			addCredentials(pull);
			checkPullResult(pull.call());
		} catch (IOException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (WrongRepositoryStateException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (DetachedHeadException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (InvalidRemoteException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (CanceledException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (RefNotFoundException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (NoHeadException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (TransportException e) {
			throw new RuntimeException("Failed to pull", e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to pull", e);
		}
	}
	
	private void checkPullResult(PullResult pullResult) {
		LOG.debug("Pull result: " + pullResult.getMergeResult().getMergeStatus());
		
		if (!pullResult.isSuccessful()) {
			throw new RuntimeException(String.format("Merge Failed. Fetch result [%s] merge result [%s]", pullResult.getFetchResult().getMessages(), pullResult.getMergeResult().getMergeStatus()));
		}
	}
	
	/**
	 * git push
	 */
	public void push() {
		checkGit();
		LOG.debug("Pushing to remote repo " + getGit().getRepository());
		Iterable<PushResult> result = null;
		try {
			PushCommand push = getGit().push();
			addCredentials(push);
			result = push.call();
			checkPushResult(result);
		} catch (org.eclipse.jgit.api.errors.TransportException e) {
			if (!e.getMessage().equals(NOTHING_TO_PUSH_ERR_MSG)) {
				throw new RuntimeException("Filed to push", e);
			} else {
				LOG.info(NOTHING_TO_PUSH_ERR_MSG);
			}
		} catch (InvalidRemoteException e) {
			throw new RuntimeException("Filed to push", e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Filed to push", e);
		} catch (Exception e) {
			throw new RuntimeException("Filed to push", e);
		}
	}
	
	private void checkPushResult(Iterable<PushResult> pushResults) {
		for (PushResult pushResult : pushResults) {
			for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
				Status status = update.getStatus();
				LOG.debug("Push Result: " + status);
				
				if (status != Status.OK) {
					throw new RuntimeException("Failed to push: " + update.getRemoteName() + " with status " + status + (update.getMessage() == null ? "" : "(additional error information: [" + update.getMessage() + "])"));
				}
			}
		}
	}
	
	/**
	 * git checkout
	 * @param branchName
	 */
	public void checkoutBranch(String branchName) {
		checkGit();
		LOG.debug("Checking out branch with name [" + branchName + "] ");
		CheckoutCommand cc = getGit().checkout();
		cc.setCreateBranch(false);
		cc.setName(branchName);
		cc.setForce(true);
		try {
			cc.call();
		} catch (RefAlreadyExistsException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s]", branchName), e);
		} catch (RefNotFoundException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s]", branchName), e);
		} catch (InvalidRefNameException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s]", branchName), e);
		} catch (CheckoutConflictException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s]", branchName), e);
		} catch (GitAPIException e) {
			throw new RuntimeException(String.format("Failed to checkout branch [%s]", branchName), e);
		}
	}
	
	/**
	 * git checkout -b
	 * @param branchName
	 * @param startPoint commit to point to (if null, HEAD which is what you usually want)
	 * @return
	 */
	public String checkoutNewBranch(String branchName, String startPoint) {
		checkGit();
		LOG.debug("Checking out a new branch with name [" + branchName + "] from starting point [" + startPoint + "]");
		CheckoutCommand cc = getGit().checkout();
		cc.setCreateBranch(true);
		cc.setName(branchName);
		cc.setStartPoint(startPoint);
		cc.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK);
		cc.setForce(true);
		try {
			return cc.call().getName();
		} catch (RefAlreadyExistsException e) {
			throw new RuntimeException(String.format("Failed to checkout new branch [%s] starting at [%s]", branchName, startPoint));
		} catch (RefNotFoundException e) {
			throw new RuntimeException(String.format("Failed to checkout new branch [%s] starting at [%s]", branchName, startPoint));
		} catch (InvalidRefNameException e) {
			throw new RuntimeException(String.format("Failed to checkout new branch [%s] starting at [%s]", branchName, startPoint));
		} catch (CheckoutConflictException e) {
			throw new RuntimeException(String.format("Failed to checkout new branch [%s] starting at [%s]", branchName, startPoint));
		} catch (GitAPIException e) {
			throw new RuntimeException(String.format("Failed to checkout new branch [%s] starting at [%s]", branchName, startPoint));
		}
	}
	
	/**
	 * git add .
	 */
	public void addAllFiles() {
		checkGit();
		LOG.debug("Adding file pattern for all files to be selected");
		try {
			getGit().add().addFilepattern(".").call();
		} catch (NoFilepatternException e) {
			throw new RuntimeException("Failed to add all files", e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to add all files", e);
		}
	}

	/**
	 * e.g "path/file.ext"
	 * e.g "file.txt"
	 * e.g "."
	 * 
	 * @param filepattern
	 * @return
	 */
	public void addFileByPattern(String filepattern) {
		checkGit();
		LOG.debug("Adding files be specified pattern " + filepattern);
		try {
			getGit().add().addFilepattern(filepattern).call();
		} catch (NoFilepatternException e) {
			throw new RuntimeException(String.format("Failed to add files with pattern [%s]", filepattern), e);
		} catch (GitAPIException e) {
			throw new RuntimeException(String.format("Failed to add files with pattern [%s]", filepattern), e);
		}
	}

	/**
	 * git merge
	 * @param branchName
	 * @return true if successful, i.e. nothing in workspace to resolve
	 */
	public boolean merge(String branchName) {
		checkGit();
		try {
			Ref refToMerge = git.getRepository().getRef(branchName);
			
			if (null == refToMerge) {
				throw new RuntimeException(String.format("Failed to find Ref for branch name [%s]", branchName));
			}
			
			return getGit().merge()
				.include(refToMerge)
				.setStrategy(MergeStrategy.RESOLVE)
				.setCommit(true)
				.setSquash(false)
				.setFastForward(FastForwardMode.NO_FF)
				.call().getMergeStatus().isSuccessful();
		} catch (NoHeadException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (ConcurrentRefUpdateException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (CheckoutConflictException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (InvalidMergeHeadsException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (WrongRepositoryStateException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (NoMessageException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (GitAPIException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to merge " + branchName, e);
		}
	}
	
	private static File createDirIfNull(File repoDir) {
		if (null == repoDir) {
			try {
				repoDir = org.eclipse.jgit.util.FileUtils.createTempDir("git-", "-repo", null);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create tmp dir", e);
			}
		}
		return repoDir;
	}

	/**
	 * Close the encapsulated git resources.
	 */
	public void close() {
		if (null != git) {
			git.close();
		}
	}
	
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the isInteractivePasswordEnabled
	 */
	public boolean isInteractivePasswordEnabled() {
		return isInteractivePasswordEnabled;
	}

	/**
	 * @param isInteractivePasswordEnabled the isInteractivePasswordEnabled to set
	 */
	public void setInteractivePasswordEnabled(boolean isInteractivePasswordEnabled) {
		this.isInteractivePasswordEnabled = isInteractivePasswordEnabled;
	}

	/**
	 * 
	 * @return actual Git library handle
	 */
	public Git getGit() {
		return git;
	}

	private void setGit(Git git) {
		this.git = git;
	}
	
	/**
	 * @return path to .git metadata dir
	 */
	public File getGitMetadataDir() {
		if (null != git) {
			return this.git.getRepository().getDirectory();
		}
		
		return null;
	}
	
	
	public String getHashForHead() {
		try {
			return git.getRepository()
					  .getRef(git.getRepository().getBranch())
				      .getObjectId().name();
		} catch (IOException e) {
			throw new RuntimeException("Unable to obtain hash for HEAD revision");
		}
	}
}
