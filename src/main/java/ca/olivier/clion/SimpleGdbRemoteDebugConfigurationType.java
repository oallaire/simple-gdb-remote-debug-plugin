package ca.olivier.clion;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.ide.ui.ProductIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NotNullLazyValue;
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SimpleGdbRemoteDebugConfigurationType extends CMakeRunConfigurationType {

    private static final String FACTORY_ID = "ca.olivier.simplegdbremotedebug.conf.factory";
    public static final String TYPE_ID = "ca.olivier.simplegdbremotedebug.conf.type";
    public static final NotNullLazyValue<Icon> ICON = new NotNullLazyValue<Icon>() {

        @NotNull
        @Override
        protected Icon compute() {
            final Icon icon = IconLoader.findIcon("icons8-code-13.png",
                    SimpleGdbRemoteDebugConfigurationType.class);
            return icon == null ? ProductIcons.getInstance().getProductIcon() : icon;
        }

    };
    private final ConfigurationFactory factory;

    public SimpleGdbRemoteDebugConfigurationType() {
        super(TYPE_ID,
                FACTORY_ID,
                "Simple GDB Remote Debug",
                "Uploads file to target, starts gdbserver and debug session.",
                ICON
        );
        factory = new ConfigurationFactory(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new SimpleGdbRemoteDebugConfiguration(project, factory, "");
            }

            @NotNull
            @Override
            public RunConfigurationSingletonPolicy getSingletonPolicy() {
                return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY;
            }

            @NotNull
            @Override
            public String getId() {
                return FACTORY_ID;
            }
        };
    }

    @Override
    public SimpleGdbRemoteDebugConfigurationEditor createEditor(@NotNull Project project) {
        return new SimpleGdbRemoteDebugConfigurationEditor(project, getHelper(project));
    }

    @NotNull
    @Override
    protected SimpleGdbRemoteDebugConfiguration createRunConfiguration(@NotNull Project project,
                                                                       @NotNull ConfigurationFactory configurationFactory) {
        return new SimpleGdbRemoteDebugConfiguration(project, factory, "");
    }
}
