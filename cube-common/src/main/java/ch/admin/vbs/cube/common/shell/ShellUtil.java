/**
 * Copyright (C) 2011 / cube-team <https://cube.forge.osor.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.admin.vbs.cube.common.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class used to run shell commands from java.
 */
public class ShellUtil {
	public static final int NOT_INITIALIZED = -9153249;
	public static final int NO_TIMEOUT = 0;
	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(ShellUtil.class);
	/** Optimized thread pooling */
	private static ExecutorService exe = Executors.newCachedThreadPool();
	/** Current process. */
	private Process proc;
	/** Watchdog */
	private ProcessWatchdog watchdog = null;
	/** Return value */
	private int exitCode = NOT_INITIALIZED;
	private StringBuffer stdOut;
	private StringBuffer stdErr;

	/** Constructor. */
	public ShellUtil() {
	}

	public final void run(File workingDirectory, long timeout, String... commandWithArgs) throws ShellUtilException {
		run(Arrays.asList(commandWithArgs), workingDirectory, timeout);
	}

	public final void run(List<String> commandWithArgs) throws ShellUtilException {
		run(commandWithArgs, null, NO_TIMEOUT);
	}

	public final void run(List<String> commandWithArgs, File workingDirectory, long timeout) throws ShellUtilException {
		BufferedReader bufferedStdOutput = null;
		BufferedReader bufferedStdError = null;
		// log command
		String commandLine = getCommandLine(commandWithArgs);
		LOG.debug("Run command [{}]", commandLine);
		// run command
		try {
			// use new ProcessBuilder API
			ProcessBuilder pb = new ProcessBuilder(commandWithArgs);
			// set working directory
			if (workingDirectory != null) {
				pb.directory(workingDirectory);
			}
			// start process
			proc = pb.start();
			// create BufferedReaders to read standard and error output
			bufferedStdOutput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			bufferedStdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			// eventually start a "watchdog" if a timeout limit has been
			// provided
			if (timeout > 0) {
				watchdog = new ProcessWatchdog(proc, timeout, commandLine, bufferedStdOutput, bufferedStdError);
			}
			// wait end of process
			proc.waitFor();
			// exit code
			exitCode = proc.exitValue();
		} catch (Exception e) {
			// log error
			if (workingDirectory == null) {
				throw new ShellUtilException("Failed to run command [" + commandLine + "]", e);
			} else {
				throw new ShellUtilException("Failed to run command [" + commandLine + "] in [" + workingDirectory.getAbsolutePath() + "]", e);
			}
		} finally {
			// consume standard and error output and save them in StringBuffers
			stdOut = processOutput("stdout", bufferedStdOutput);
			stdErr = processOutput("stderr", bufferedStdError);
			// handle error case
			if (exitCode == NOT_INITIALIZED) {
				// something went really wrong. destroy process
				try {
					proc.destroy();
					LOG.debug("Process destroyed [{}]", commandLine);
				} catch (Exception e) {
					LOG.debug("Failed to destroy process [{}]", commandLine);
				}
			}
			// terminate watchdog
			if (watchdog != null) {
				watchdog.terminate();
			}
		}
		// ensure buffers are closed
		closeStream(bufferedStdOutput);
		closeStream(bufferedStdError);
	}

	private final void closeStream(BufferedReader br) {
		if (br != null) {
			try {
				br.close();
			} catch (Exception e) {
				LOG.error("Failed to close stream properly");
			}
		}
	}

	/**
	 * Consume outputstream and store its output in a StingBuffer. Add a prefix
	 * to each output line.
	 */
	private StringBuffer processOutput(String prefix, BufferedReader br) {
		StringBuffer outputStr = new StringBuffer();
		if (br != null) {
			try {
				while (br.ready()) {
					String line = br.readLine();
					// save line
					outputStr.append(line).append('\n');
					// log
					LOG.debug("<{}> {}", prefix, line);
				}
			} catch (Exception e) {
				LOG.error("Failed to process output [" + prefix + "]", e);
			}
		}
		return outputStr;
	}

	/** Watchdog. Wait a given time before terminating a process */
	private class ProcessWatchdog implements Runnable {
		private static final int REFRESH_TO = 1000;
		private boolean running;
		private Process proc;
		private long timeout;
		private final String commandString;
		private final BufferedReader stdOutput;
		private final BufferedReader stdError;

		/**
		 * Constructor.
		 * 
		 * @param proc
		 *            process
		 * @param timeout
		 *            timeout
		 * @param commandString
		 *            command
		 * @param bufferedStdError
		 * @param bufferedStdOutput
		 */
		public ProcessWatchdog(final Process proc, final long timeout, final String commandString, BufferedReader bufferedStdOutput,
				BufferedReader bufferedStdError) {
			this.proc = proc;
			this.commandString = commandString;
			this.stdOutput = bufferedStdOutput;
			this.stdError = bufferedStdError;
			this.timeout = timeout + System.currentTimeMillis();
			exe.execute(this);
		}

		/** Implements Runnable. */
		public void run() {
			running = true;
			try {
				while (running && timeout > System.currentTimeMillis()) {
					Thread.sleep(REFRESH_TO);
				}
			} catch (Exception e) {
				LOG.error("Failure", e);
			}
			// if not terminated, destroy process
			if (running) {
				try {
					proc.destroy();
					LOG.warn("Timeout reached. Process killed [{}][{}].", commandString, timeout);
				} catch (Exception e) {
					LOG.error("Failed to terminate process", e);
				}
				closeStream(stdOutput);
				closeStream(stdError);
			}
		}

		/** Stop thread. */
		public void terminate() {
			running = false;
		}
	}

	/**
	 * @return standard output line by line
	 */
	public final StringBuffer getStandardOutput() {
		return stdOut;
	}

	/**
	 * @return standard error line by line
	 */
	public final StringBuffer getStandardError() {
		return stdErr;
	}

	/**
	 * Returns the exit value of the external execution.
	 * 
	 * @return exit value {@link Integer}
	 */
	public final int getExitValue() {
		return exitCode;
	}

	/**
	 * Forces to terminate the running process.
	 */
	public void destroyProcess() {
		if (proc != null) {
			proc.destroy();
		}
	}

	/** build command line string (for logs). */
	private String getCommandLine(List<String> commandWithArgs) throws ShellUtilException {
		if (commandWithArgs == null || commandWithArgs.size() == 0) {
			throw new ShellUtilException("Parameter commandWithArgs was null!");
		}
		if (commandWithArgs.get(0).equals("virtualpin")) {
			// 'virtualpin' command contains PIN as argument in clear text and
			// should not be logged.
			return "<virtualpin>";
		}
		StringBuilder sb = new StringBuilder();
		for (String commandOrArgument : commandWithArgs) {
			sb.append(commandOrArgument).append(" ");
		}
		// remove last space
		return sb.substring(0, sb.length() - 1);
	}
}
