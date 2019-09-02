package ca.olivier.clion;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleGdbRemoteDebugConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {

    private static final String hostPasswordServiceName = "ca.olivier : SGRDRegistration";

    private String selectedGdb;
    private String customGdbBin;
    private String sysrootFolder;
    private String host;
    private String user;
    private String remoteFolder;

    public SimpleGdbRemoteDebugConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public @Nullable CidrCommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
//        return new CidrCommandLineState(env, );
        return null;
    }

    public String getSelectedGdb() {
        return selectedGdb;
    }

    public void setSelectedGdb(String selectedGdb) {
        this.selectedGdb = selectedGdb;
    }

    public String getCustomGdbBin() {
        return customGdbBin;
    }

    public void setCustomGdbBin(String customGdbBin) {
        this.customGdbBin = customGdbBin;
    }

    public String getSysrootFolder() {
        return sysrootFolder;
    }

    public void setSysrootFolder(String sysrootFolder) {
        this.sysrootFolder = sysrootFolder;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHostPassword() {
        CredentialAttributes ca = new CredentialAttributes(SimpleGdbRemoteDebugConfiguration.hostPasswordServiceName,
                getUser(), null, false);
        return PasswordSafe.getInstance().getPassword(ca);
    }

    public void setHostPassword(String password) {
        CredentialAttributes ca = new CredentialAttributes(SimpleGdbRemoteDebugConfiguration.hostPasswordServiceName,
                getUser(), null, false);
        Credentials c = new Credentials(ca.getUserName(), password);
        PasswordSafe.getInstance().set(ca, c);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRemoteFolder() {
        return remoteFolder;
    }

    public void setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
    }
}
