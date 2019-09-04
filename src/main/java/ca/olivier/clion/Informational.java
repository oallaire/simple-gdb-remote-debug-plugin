package ca.olivier.clion;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class Informational {

    public static final String HELP_URL = "https://github.com/oallaire/simple-gdb-remote-debug-plugin";

    public static void showPluginError(Project project, ConfigurationException e) {
        int optionNo = Messages.showDialog(project, e.getLocalizedMessage(), e.getTitle(),
                new String[]{Messages.OK_BUTTON, CommonBundle.getHelpButtonText()}
                , 0, Messages.getErrorIcon());
        //nothing to do
        if (optionNo == 1) {
            BrowserUtil.browse(HELP_URL);
        }
    }

}
