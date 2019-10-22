package ca.olivier.clion;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PasswordFieldPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.HorizontalBox;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SimpleGdbRemoteDebugConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private FileChooseInput sysrootFolderInput;
    private FileChooseInput customGdbBinInput;
    private JComboBox<GdbComboBoxItem> stringGdbComboBox;
    private JComboBox<String> stringSyncComboBox;
    private JBTextField gdbPort;
    private JBTextField hostText;
    private JBTextField userText;
    private PasswordFieldPanel hostPassword;
    private JBTextField remoteFolder;
    private JBTextField remoteArguments;

    @SuppressWarnings("WeakerAccess")
    public SimpleGdbRemoteDebugConfigurationEditor(Project project,
                                                   @NotNull CMakeBuildConfigurationHelper cMakeBuildConfigurationHelper) {
        super(project, cMakeBuildConfigurationHelper);
    }

    @Override
    protected void applyEditorTo(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) throws ConfigurationException {
        super.applyEditorTo(cMakeAppRunConfiguration);
        SimpleGdbRemoteDebugConfiguration sgrdConfig = (SimpleGdbRemoteDebugConfiguration) cMakeAppRunConfiguration;

        String selectedGdb = stringGdbComboBox.getItemAt(stringGdbComboBox.getSelectedIndex()).displayName;
        sgrdConfig.setSelectedGdb(selectedGdb);

        String customGdbBin = customGdbBinInput.getText().trim();
        sgrdConfig.setCustomGdbBin(customGdbBin.isEmpty() ? null : customGdbBin);

        String selectedSync = stringSyncComboBox.getItemAt(stringSyncComboBox.getSelectedIndex());
        sgrdConfig.setSelectedSync(selectedSync);

        sgrdConfig.setGdbPort(gdbPort.getText());

        String sysrootFolder = sysrootFolderInput.getText().trim();
        sgrdConfig.setSysrootFolder(sysrootFolder.isEmpty() ? null : sysrootFolder);

        sgrdConfig.setHost(hostText.getText().trim());

        sgrdConfig.setUser(userText.getText().trim());

        sgrdConfig.setHostPassword(hostPassword.getText());

        sgrdConfig.setRemoteFolder(remoteFolder.getText().trim());

        sgrdConfig.setRemoteArguments(remoteArguments.getText().trim());
    }

    @Override
    protected void resetEditorFrom(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration);
        SimpleGdbRemoteDebugConfiguration sgrdConfig = (SimpleGdbRemoteDebugConfiguration) cMakeAppRunConfiguration;
        // Check if the selected GDB still exists
        for (int i = 0; i < stringGdbComboBox.getItemCount(); i++) {
            GdbComboBoxItem item = stringGdbComboBox.getItemAt(i);
            if (item.displayName.equals(sgrdConfig.getSelectedGdb())) {
                stringGdbComboBox.setSelectedItem(item);
            }
        }

        customGdbBinInput.setText(sgrdConfig.getCustomGdbBin());

        gdbPort.setText(sgrdConfig.getGdbPort());

        // Check if the selected sync still exists
        for (int i = 0; i < stringSyncComboBox.getItemCount(); i++) {
            String item = stringSyncComboBox.getItemAt(i);
            if (item.equals(sgrdConfig.getSelectedSync())) {
                stringSyncComboBox.setSelectedItem(item);
            }
        }

        sysrootFolderInput.setText(sgrdConfig.getSysrootFolder());

        hostText.setText(sgrdConfig.getHost());

        userText.setText(sgrdConfig.getUser());

        hostPassword.setText(sgrdConfig.getHostPassword());

        remoteFolder.setText(sgrdConfig.getRemoteFolder());

        remoteArguments.setText(sgrdConfig.getRemoteArguments());
    }

    @Override
    protected void createEditorInner(JPanel panel, GridBag gridBag) {
        super.createEditorInner(panel, gridBag);

        for (Component component : panel.getComponents()) {
            if (component instanceof CommonProgramParametersPanel) {
                component.setVisible(false);//todo get rid of this hack
            }
        }

        JPanel gdbPanel = createGdbSelector(panel, gridBag);
        panel.add(gdbPanel, gridBag.next().coverLine());

        JPanel customGsbPanel = createCustomGdbSelector(panel, gridBag);
        panel.add(customGsbPanel, gridBag.next().coverLine());

        JPanel sysrootPanel = createSysrootSelector(panel, gridBag);
        panel.add(sysrootPanel, gridBag.next().coverLine());

        panel.add(new JLabel("GDB Port:"), gridBag.nextLine().next());
        gdbPort = new JBTextField();
        panel.add(gdbPort, gridBag.next().coverLine());

        JPanel syncPanel = createSyncSelector(panel, gridBag);
        panel.add(syncPanel, gridBag.next().coverLine());

        panel.add(new JLabel("Host:"), gridBag.nextLine().next());
        hostText = new JBTextField();
        panel.add(hostText, gridBag.next().coverLine());

        panel.add(new JLabel("User:"), gridBag.nextLine().next());
        userText = new JBTextField();
        panel.add(userText, gridBag.next().coverLine());

        panel.add(new JLabel("Password:"), gridBag.nextLine().next());
        hostPassword = new PasswordFieldPanel();
        panel.add(hostPassword, gridBag.next().coverLine());

        panel.add(new JLabel("Remote Folder:"), gridBag.nextLine().next());
        remoteFolder = new JBTextField();
        panel.add(remoteFolder, gridBag.next().coverLine());

        panel.add(new JLabel("Remote Arguments:"), gridBag.nextLine().next());
        remoteArguments = new JBTextField();
        panel.add(remoteArguments, gridBag.next().coverLine());
    }

    @NotNull
    private JPanel createSysrootSelector(JPanel panel, GridBag gridBag) {
        panel.add(new JLabel("Sysroot:"), gridBag.nextLine().next());
        JPanel sysrootPanel = new HorizontalBox();
        sysrootFolderInput = new FileChooseInput("Sysroot", VfsUtil.getUserHomeDir()) {
            @Override
            protected boolean validateFile(VirtualFile virtualFile) {
                return virtualFile.exists() && virtualFile.isDirectory();
            }

            @Override
            protected FileChooserDescriptor createFileChooserDescriptor() {
                return FileChooserDescriptorFactory.createSingleFolderDescriptor();
            }
        };
        sysrootPanel.add(sysrootFolderInput);
        return sysrootPanel;
    }

    private void updateCustomGdbState() {
        GdbComboBoxItem item = stringGdbComboBox.getItemAt(stringGdbComboBox.getSelectedIndex());
        if (item.toolchain == null) {
            customGdbBinInput.setEnabled(true);
        } else {
            customGdbBinInput.setEnabled(false);
        }
    }

    @NotNull
    private JPanel createGdbSelector(JPanel panel, GridBag gridBag) {
        panel.add(new JLabel("GDB:"), gridBag.nextLine().next());
        JPanel gdbPanel = new HorizontalBox();
        stringGdbComboBox = new ComboBox<>();
        boolean bundledAdded = false;
        for (CPPToolchains.Toolchain toolchain : CPPToolchains.getInstance().getToolchains() ) {
            GdbComboBoxItem item = new GdbComboBoxItem();
            item.toolchain = toolchain;
            if (toolchain.getDebugger().getKind() == CPPDebugger.Kind.BUNDLED_GDB) {
                if (bundledAdded) {
                    continue;
                }
                item.displayName = SimpleGdbRemoteDebugConfiguration.BUNDLED_GDB_NAME;
                stringGdbComboBox.addItem(item);
                bundledAdded = true;
            } else {
                item.displayName = toolchain.getName() + " GDB";
                stringGdbComboBox.addItem(item);
            }
        }
        GdbComboBoxItem customGdbItem = new GdbComboBoxItem();
        customGdbItem.toolchain = null;
        customGdbItem.displayName = SimpleGdbRemoteDebugConfiguration.CUSTOM_GDB_NAME;
        stringGdbComboBox.addItem(customGdbItem);
        gdbPanel.add(stringGdbComboBox);
        return gdbPanel;
    }

    @NotNull
    private JPanel createCustomGdbSelector(JPanel panel, GridBag gridBag) {
        panel.add(new JLabel("Custom GDB:"), gridBag.nextLine().next());
        JPanel customGdbPanel = new HorizontalBox();
        customGdbBinInput = new FileChooseInput("CustomGdb", VfsUtil.getUserHomeDir()) {
            @Override
            protected boolean validateFile(VirtualFile virtualFile) {
                return virtualFile.exists() && !virtualFile.isDirectory();
            }

            @Override
            protected FileChooserDescriptor createFileChooserDescriptor() {
                if (SystemInfo.isWindows) {
                    return FileChooserDescriptorFactory.createSingleFileDescriptor("exe");
                } else {
                    return FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
                }
            }
        };
        customGdbPanel.add(customGdbBinInput);
        updateCustomGdbState();
        stringGdbComboBox.addActionListener(e -> updateCustomGdbState());
        return customGdbPanel;
    }

    @NotNull
    private JPanel createSyncSelector(JPanel panel, GridBag gridBag) {
        panel.add(new JLabel("Sync:"), gridBag.nextLine().next());
        JPanel syncPanel = new HorizontalBox();
        stringSyncComboBox = new ComboBox<>();
        stringSyncComboBox.addItem("rsync");
        stringSyncComboBox.addItem("scp");
        syncPanel.add(stringSyncComboBox);
        return syncPanel;
    }

    private static class GdbComboBoxItem {
        private CPPToolchains.Toolchain toolchain;
        private String displayName;

        @Override
        public String toString() {
            return displayName;
        }
    }
}
