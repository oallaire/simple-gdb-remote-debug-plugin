package ca.olivier.clion;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class RemoteProcessHandler extends ProcessHandler {

    private Future<?> runningProcess;
    private SimpleGdbRemoteDebugConfiguration sgrdConfig;
    private File fileToRun;
    private Process debugProcess;

    private final DelegateOrDropOutputStream delegateOrDropOutput = new DelegateOrDropOutputStream();

    RemoteProcessHandler(SimpleGdbRemoteDebugConfiguration sgrdConfig, File fileToRun) {
        this.sgrdConfig = sgrdConfig;
        this.fileToRun = fileToRun;
    }

    @Override
    public void startNotify() {
        super.startNotify();

        if (sgrdConfig.getHost().isEmpty()) {
            notifyTextAvailable("Host not configured\n", ProcessOutputTypes.STDERR);
            notifyProcessTerminated(-1);
            return;
        }

        runningProcess = Executors.newSingleThreadExecutor().submit(() -> {
            notifyTextAvailable("Going to run on " + sgrdConfig.getHost() + " with " + fileToRun.getName() + "\n",
                    ProcessOutputTypes.STDERR);
            List<VirtualFile> filesToSync = new ArrayList<>();
            filesToSync.add(VfsUtil.findFileByIoFile(fileToRun, true));
            int result = executeCommand(syncCommand(filesToSync));
            if (result != 0) {
                notifyTextAvailable("Sync failed: " + result + "\n", ProcessOutputTypes.STDERR);
                throw new RuntimeException("Sync failed");
            }
            int debugResult;
            debugProcess = execute(gdbServerCommand(), ProcessOutputTypes.STDOUT);
            try {
                debugResult = debugProcess.waitFor();
            } catch (InterruptedException x) {
                try {
                    debugResult = debugProcess.destroyForcibly().waitFor();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            result = executeCommand(killProcessCommand());
            if (result != 0) {
                notifyTextAvailable("Kill failed: " + result + "\n", ProcessOutputTypes.STDOUT);
            }
            destroySshTunnelIfPresent();
            notifyProcessTerminated(debugResult);
        });
    }

    @Override
    protected void destroyProcessImpl() {
        runningProcess.cancel(true);
        destroySshTunnelIfPresent();
    }

    @Override
    protected void detachProcessImpl() {
        notifyTextAvailable("Detatch is not implemented, destroying instead\n", ProcessOutputTypes.STDERR);
        destroyProcessImpl();
    }

    @Override
    public boolean detachIsDefault() {
        return false;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return delegateOrDropOutput;
    }

    private int executeCommand(String[] cmd) {
        Process process = execute(cmd, ProcessOutputTypes.SYSTEM);

        try {
            return process.waitFor();
        } catch (InterruptedException x) {
            try {
                return process.destroyForcibly().waitFor();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Process execute(String[] cmd, Key outType) {
        try {
            notifyTextAvailable(String.join(" ", cmd) + "\n",
                    ProcessOutputTypes.SYSTEM);
            Process process = new ProcessBuilder(cmd).start();

            delegateOrDropOutput.setOutput(process.getOutputStream());

            ExecutorService io = Executors.newCachedThreadPool();
            io.execute(textNotifierOf(process.getInputStream(), outType));
            io.execute(textNotifierOf(process.getErrorStream(), ProcessOutputTypes.STDERR));
            return process;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Runnable textNotifierOf(InputStream in, Key outputType) {
        return () -> {
            InputStreamReader reader = new InputStreamReader(in);
            try {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    String chars = new String(buffer, 0, read);
                    notifyTextAvailable(chars, outputType);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @NotNull
    private String[] syncCommand(List<VirtualFile> requiredFiles) {
        List<String> cmdLine = requiredFiles.stream().map(f -> {
            if (!f.isInLocalFileSystem()) {
                throw new RuntimeException("Cannot sync file " + f.getPath() + " it is not local");
            }

            return f.getPath();
        }).collect(Collectors.toList());
        cmdLine.add(0, sgrdConfig.getSelectedSync());
        if (sgrdConfig.getSelectedSync().equals("rsync")) {
            cmdLine.add(1, "-avh");
        }
        cmdLine.add(sgrdConfig.getUser() + "@" + sgrdConfig.getHost() + ":" + sgrdConfig.getRemoteFolder());
        if (sgrdConfig.getSelectedSync().equals("rsync")) {
            cmdLine.add("--delete");
        }

        return cmdLine.toArray(new String[0]);
    }

    @NotNull
    private String[] gdbServerCommand() {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add("ssh");
        cmdLine.add(sgrdConfig.getUser() + "@" + sgrdConfig.getHost());
        cmdLine.add("gdbserver");
        cmdLine.add(":" + sgrdConfig.getGdbPort());
        cmdLine.add(sgrdConfig.getRemoteFolder() + "/" + fileToRun.getName());
        if (sgrdConfig.getRemoteArguments() != null && !sgrdConfig.getRemoteArguments().isEmpty()) {
            cmdLine.add(sgrdConfig.getRemoteArguments());
        }
        return cmdLine.toArray(new String[0]);
    }

    @NotNull
    private String[] killProcessCommand() {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add("ssh");
        cmdLine.add(sgrdConfig.getUser() + "@" + sgrdConfig.getHost());
        cmdLine.add("killall");
        cmdLine.add("-9");
        cmdLine.add(fileToRun.getName());
        return cmdLine.toArray(new String[0]);
    }

    private void destroySshTunnelIfPresent() {
        try {
            if (debugProcess != null) {
                debugProcess.destroyForcibly().waitFor();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class DelegateOrDropOutputStream extends OutputStream {
        private volatile OutputStream delegate;

        interface OutputStreamWork {
            void accept(OutputStream out) throws IOException;
        }

        private void setOutput(OutputStream output) {
            delegate = output;
        }

        private void ifPresent(OutputStreamWork work) throws IOException {
            OutputStream d = delegate;
            if (d != null) {
                work.accept(d);
            }
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            ifPresent(o -> o.write(b));
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            ifPresent(o -> o.write(b, off, len));
        }

        @Override
        public void flush() throws IOException {
            ifPresent(OutputStream::flush);
        }

        @Override
        public void close() throws IOException {
            ifPresent(OutputStream::close);
        }

        @Override
        public void write(int b) throws IOException {
            ifPresent(o -> o.write(b));
        }
    }
}
