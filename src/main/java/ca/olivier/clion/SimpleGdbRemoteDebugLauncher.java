package ca.olivier.clion;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.toolchains.GDB;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SimpleGdbRemoteDebugLauncher extends CidrLauncher {

    private final SimpleGdbRemoteDebugConfiguration simpleGdbRemoteDebugConfiguration;

    SimpleGdbRemoteDebugLauncher(SimpleGdbRemoteDebugConfiguration simpleGdbRemoteDebugConfiguration) {
        this.simpleGdbRemoteDebugConfiguration = simpleGdbRemoteDebugConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
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
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations = simpleGdbRemoteDebugConfiguration
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
