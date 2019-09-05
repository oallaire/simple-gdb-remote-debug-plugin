package ca.olivier.clion;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleGdbRemoteDebugConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {

    static final String CUSTOM_GDB_NAME = "Custom GDB";
    static final String BUNDLED_GDB_NAME = "Bundled GDB";

    private static final String HOST_PASSWORD_SERVICE_NAME = "ca.olivier : SGRDRegistration";

    private static final String TAG_SGRD = "sgrd";
    private static final String ATTR_SELECTED_GDB = "selected-gdb";
    private static final String ATTR_CUSTOM_GDB = "custom-gdb";
    private static final String ATTR_GDB_PORT = "gdb-port";
    private static final String ATTR_SYSROOT_FOLDER = "sysroot-folder";
    private static final String ATTR_HOST = "host";
    private static final String ATTR_USER = "user";
    private static final String ATTR_REMOTE_FOLDER = "remote-folder";

    private String selectedGdb;
    private String customGdbBin;
    private String gdbPort;
    private String sysrootFolder;
    private String host;
    private String user;
    private String remoteFolder;

    SimpleGdbRemoteDebugConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Nullable
    @Override
    public CidrCommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new CidrCommandLineState(environment, new SimpleGdbRemoteDebugLauncher(this));
    }

    @Override
    public void readExternal(@NotNull Element parentElement) throws InvalidDataException {
        super.readExternal(parentElement);
        Element element = parentElement.getChild(TAG_SGRD);
        if(element!=null) {
            selectedGdb = element.getAttributeValue(ATTR_SELECTED_GDB);
            customGdbBin = element.getAttributeValue(ATTR_CUSTOM_GDB);
            gdbPort = element.getAttributeValue(ATTR_GDB_PORT);
            sysrootFolder = element.getAttributeValue(ATTR_SYSROOT_FOLDER);
            host = element.getAttributeValue(ATTR_HOST);
            user = element.getAttributeValue(ATTR_USER);
            remoteFolder = element.getAttributeValue(ATTR_REMOTE_FOLDER);
        }
    }

    @Override
    public void writeExternal(@NotNull Element parentElement) throws WriteExternalException {
        super.writeExternal(parentElement);
        Element element = new Element(TAG_SGRD);
        parentElement.addContent(element);
        if (selectedGdb != null) {
            element.setAttribute(ATTR_SELECTED_GDB, selectedGdb);
        }
        if (customGdbBin != null) {
            element.setAttribute(ATTR_CUSTOM_GDB, customGdbBin);
        }
        if (gdbPort != null) {
            element.setAttribute(ATTR_GDB_PORT, gdbPort);
        }
        if (sysrootFolder != null) {
            element.setAttribute(ATTR_SYSROOT_FOLDER, sysrootFolder);
        }
        if (host != null) {
            element.setAttribute(ATTR_HOST, host);
        }
        if (user != null) {
            element.setAttribute(ATTR_USER, user);
        }
        if (remoteFolder != null) {
            element.setAttribute(ATTR_REMOTE_FOLDER, remoteFolder);
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();
        if (selectedGdb == null || selectedGdb.isEmpty()) {
            throw  new RuntimeConfigurationException("GDB should be selected.");
        }
        if (selectedGdb.equals(SimpleGdbRemoteDebugConfiguration.CUSTOM_GDB_NAME)) {
            if (customGdbBin == null || customGdbBin.isEmpty()) {
                throw new RuntimeConfigurationException("Custom GDB path not set.");
            }
        }
        if (gdbPort == null || gdbPort.isEmpty()) {
            throw  new RuntimeConfigurationException("GDB port not set.");
        }
        if (sysrootFolder == null || sysrootFolder.isEmpty()) {
            throw  new RuntimeConfigurationException("Sysroot folder not set.");
        }
        if (host == null || host.isEmpty()) {
            throw  new RuntimeConfigurationException("Host not set.");
        }
        if (user == null || user.isEmpty()) {
            throw  new RuntimeConfigurationException("User not set.");
        }
        if (remoteFolder == null || remoteFolder.isEmpty()) {
            throw  new RuntimeConfigurationException("Remote folder not set.");
        }
    }

    String getSelectedGdb() {
        return selectedGdb;
    }

    void setSelectedGdb(String selectedGdb) {
        this.selectedGdb = selectedGdb;
    }

    String getCustomGdbBin() {
        return customGdbBin;
    }

    void setCustomGdbBin(String customGdbBin) {
        this.customGdbBin = customGdbBin;
    }

    String getSysrootFolder() {
        return sysrootFolder;
    }

    void setSysrootFolder(String sysrootFolder) {
        this.sysrootFolder = sysrootFolder;
    }

    String getHost() {
        return host;
    }

    void setHost(String host) {
        this.host = host;
    }

    String getHostPassword() {
        CredentialAttributes ca = new CredentialAttributes(SimpleGdbRemoteDebugConfiguration.HOST_PASSWORD_SERVICE_NAME,
                getUser(), null, false);
        return PasswordSafe.getInstance().getPassword(ca);
    }

    void setHostPassword(String password) {
        CredentialAttributes ca = new CredentialAttributes(SimpleGdbRemoteDebugConfiguration.HOST_PASSWORD_SERVICE_NAME,
                getUser(), null, false);
        Credentials c = new Credentials(ca.getUserName(), password);
        PasswordSafe.getInstance().set(ca, c);
    }

    String getUser() {
        return user;
    }

    void setUser(String user) {
        this.user = user;
    }

    String getRemoteFolder() {
        return remoteFolder;
    }

    void setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
    }

    String getGdbPort() {
        return gdbPort;
    }

    void setGdbPort(String gdbPort) {
        this.gdbPort = gdbPort;
    }
}
