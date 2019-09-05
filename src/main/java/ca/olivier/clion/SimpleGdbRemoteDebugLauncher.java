package ca.olivier.clion;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import com.jetbrains.cidr.execution.testing.CidrLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SimpleGdbRemoteDebugLauncher extends CidrLauncher {

    private final SimpleGdbRemoteDebugConfiguration sgrdConfiguration;
    private ProcessHandler gdbServerProcess;

    SimpleGdbRemoteDebugLauncher(SimpleGdbRemoteDebugConfiguration simpleGdbRemoteDebugConfiguration) {
        this.sgrdConfiguration = simpleGdbRemoteDebugConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        // FIXME - I think this is the run button. This process should simply start the bin remotely I think if not
        //  nothing at all.
        // Create process handler
        ExecutionTarget executionTarget = commandLineState.getExecutionTarget();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations;
        runConfigurations = sgrdConfiguration.getBuildAndRunConfigurations(executionTarget);
        if (runConfigurations == null) {
            throw new ExecutionException("Failed to find " + executionTarget.getDisplayName() + " configuration.");
        }
        return new RemoteProcessHandler(sgrdConfiguration, runConfigurations.getRunFile());
    }

    @NotNull
    @Override
    public CidrDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState,
                                              @NotNull XDebugSession xDebugSession) throws ExecutionException {
        xDebugSession.stop();
        // Create process handler
        ExecutionTarget executionTarget = commandLineState.getExecutionTarget();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations;
        runConfigurations = sgrdConfiguration.getBuildAndRunConfigurations(executionTarget);
        if (runConfigurations == null) {
            throw new ExecutionException("Failed to find " + executionTarget.getDisplayName() + " configuration.");
        }
        gdbServerProcess = new RemoteProcessHandler(sgrdConfiguration, runConfigurations.getRunFile());
        RunContentExecutor gdbServerProcessRun = new RunContentExecutor(sgrdConfiguration.getProject(),
                gdbServerProcess)
                .withTitle("GDB Server")
                .withActivateToolWindow(true)
                .withStop(gdbServerProcess::destroyProcess,
                        () -> !gdbServerProcess.isProcessTerminated() && !gdbServerProcess.isProcessTerminating());
        gdbServerProcessRun.run();
        return super.startDebugProcess(commandLineState, xDebugSession);
    }

    @Override
    protected @NotNull CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState, @NotNull XDebugSession xDebugSession) throws ExecutionException {
        CidrRemoteDebugParameters remoteDebugParameters = new CidrRemoteDebugParameters();

        remoteDebugParameters.setSymbolFile(findRunFile(commandLineState).getAbsolutePath());
        remoteDebugParameters.setRemoteCommand(sgrdConfiguration.getHost() + ":" + sgrdConfiguration.getGdbPort());

        // Get gdb binary
        CPPToolchains.Toolchain toolchain = null;
        if (!sgrdConfiguration.getSelectedGdb().equals(SimpleGdbRemoteDebugConfiguration.CUSTOM_GDB_NAME)) {
            for (CPPToolchains.Toolchain currentToolchain : CPPToolchains.getInstance().getToolchains()) {
                if (currentToolchain.getDebugger().getKind() == CPPDebugger.Kind.BUNDLED_GDB) {
                    if (!sgrdConfiguration.getSelectedGdb().equals(SimpleGdbRemoteDebugConfiguration.BUNDLED_GDB_NAME)) {
                        continue;
                    }
                } else {
                    if (!sgrdConfiguration.getSelectedGdb().startsWith(currentToolchain.getName())) {
                        continue;
                    }
                }
                toolchain = currentToolchain;
                break;
            }
        } else {
            String gdbPath;
            CPPToolchains.Toolchain defaultToolchain = CPPToolchains.getInstance().getDefaultToolchain();
            if (defaultToolchain == null) {
                throw new ExecutionException("Failed to get default toolchain.");
            }
            toolchain = defaultToolchain.copy();
            gdbPath = sgrdConfiguration.getCustomGdbBin();
            CPPDebugger cppDebugger = CPPDebugger.create(CPPDebugger.Kind.CUSTOM_GDB, gdbPath);
            toolchain.setDebugger(cppDebugger);
        }
        if (toolchain == null) {
            throw new ExecutionException("Please provide a valid GDB executable.");
        }
        CLionGDBDriverConfiguration gdbDriverConfiguration = new CLionGDBDriverConfiguration(getProject(), toolchain);
        xDebugSession.stop();

        CidrRemoteGDBDebugProcess debugProcess = new CidrRemoteGDBDebugProcess(gdbDriverConfiguration,
                remoteDebugParameters,
                xDebugSession,
                commandLineState.getConsoleBuilder(),
                project1 -> new Filter[0]);

        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                if (gdbServerProcess != null) {
                    gdbServerProcess.destroyProcess();
                }
            }
        });

        return debugProcess;
    }

    @Override
    protected @NotNull Project getProject() {
        return sgrdConfiguration.getProject();
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
