package ca.olivier.clion;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.remote.ext.CredentialsCase;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.debugger.RemoteDebugConfiguration;

import java.io.File;

public class SimpleGdbRemoteDebugLauncher extends CidrLauncher {

    private final SimpleGdbRemoteDebugConfiguration sgrdConfiguration;

    SimpleGdbRemoteDebugLauncher(SimpleGdbRemoteDebugConfiguration simpleGdbRemoteDebugConfiguration) {
        this.sgrdConfiguration = simpleGdbRemoteDebugConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        // Get gdb binary
        String gdbPath = null;
        if (!sgrdConfiguration.getSelectedGdb().equals(SimpleGdbRemoteDebugConfiguration.CUSTOM_GDB_NAME)) {
            for (CPPToolchains.Toolchain toolchain : CPPToolchains.getInstance().getToolchains()) {
                if (toolchain.getDebugger().getKind() == CPPDebugger.Kind.BUNDLED_GDB) {
                    if (!sgrdConfiguration.getSelectedGdb().equals(SimpleGdbRemoteDebugConfiguration.BUNDLED_GDB_NAME)) {
                        continue;
                    }
                } else {
                    if (!sgrdConfiguration.getSelectedGdb().startsWith(toolchain.getName())) {
                        continue;
                    }
                }
                gdbPath = toolchain.getDebugger().getGdbExecutablePath();
                break;
            }
        } else {
            gdbPath = sgrdConfiguration.getCustomGdbBin();
        }
        if (gdbPath == null || gdbPath.isEmpty()) {
            throw new ExecutionException("Please provide a valid GDB executable.");
        }
        File gdbFile = new File(gdbPath);
        // Create command line
        ExecutionTarget executionTarget = commandLineState.getExecutionTarget();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations;
        runConfigurations = sgrdConfiguration.getBuildAndRunConfigurations(executionTarget);
        if (runConfigurations == null) {
            throw new ExecutionException("Failed to find " + executionTarget.getDisplayName() + " configuration.");
        }
        runConfigurations.getRunFile().getName()
        try {
            GeneralCommandLine commandLine = new PtyCommandLine()
                    .withWorkDirectory(gdbFile.getParentFile())
                    .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);
            OSProcessHandler osProcessHandler = new OSProcessHandler();
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }
        File runFile = findRunFile(commandLineState);
//        CPPEn
//        GDB.getBundledGDB()
//        try {
//            GeneralCommandLine commandLine = OpenOcdComponent
//                    .createOcdCommandLine(openOcdConfiguration,
//                            runFile, "reset", true);
//            OSProcessHandler osProcessHandler = new OSProcessHandler(commandLine);
//            osProcessHandler.addProcessListener(new ProcessAdapter() {
//                @Override
//                public void processTerminated(@NotNull ProcessEvent event) {
//                    super.processTerminated(event);
//                    Project project = commandLineState.getEnvironment().getProject();
//                    if (event.getExitCode() == 0) {
//                        Informational.showSuccessfulDownloadNotification(project);
//                    } else {
//                        Informational.showFailedDownloadNotification(project);
//                    }
//                }
//            });
//            return osProcessHandler;
//        } catch (ConfigurationException e) {
//            Informational.showPluginError(getProject(), e);
//            throw new ExecutionException(e);
//        }
        return null;
    }

    @Override
    protected @NotNull CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        return null;
    }

    @Override
    protected @NotNull Project getProject() {
        return null;
    }

    @NotNull
    private File findRunFile(CommandLineState commandLineState) throws ExecutionException {
        String targetProfileName = commandLineState.getExecutionTarget().getDisplayName();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations = sgrdConfiguration
                .getBuildAndRunConfigurations(targetProfileName);
        if (runConfigurations == null) {
            throw new ExecutionException("Target is not defined");
        }
        File runFile = runConfigurations.getRunFile();
        if (runFile == null) {
            throw new ExecutionException("Run file is not defined for " + runConfigurations);
        }
        if (!runFile.exists() || !runFile.isFile()) {
            throw new ExecutionException("Invalid run file " + runFile.getAbsolutePath());
        }
        return runFile;
    }
}
