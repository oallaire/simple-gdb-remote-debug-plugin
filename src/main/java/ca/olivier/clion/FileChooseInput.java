package ca.olivier.clion;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.valueEditors.TextFieldValueEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FileChooseInput extends TextFieldWithBrowseButton {
    protected final TextFieldValueEditor<VirtualFile> editor;
    private final FileChooserDescriptor fileDescriptor;

    protected FileChooseInput(String valueName, VirtualFile defValue) {
        super(new JBTextField());
        editor = new FileTextFieldValueEditor(valueName, defValue);
        fileDescriptor = createFileChooserDescriptor().withFileFilter(this::validateFile);
        installPathCompletion(fileDescriptor);
        addActionListener(e -> {
            VirtualFile virtualFile = null;
            String text = getTextField().getText();
            if(text != null && !text.isEmpty()) {
                try {
                    virtualFile = parseTextToFile(text);
                } catch (InvalidDataException ignored) {
                    virtualFile = LocalFileSystem.getInstance().findFileByPath(text);
                }
            }
            if(virtualFile == null) {
                virtualFile = getDefaultLocation();
            }
            VirtualFile chosenFile = FileChooser.chooseFile(fileDescriptor, null, virtualFile);
            if (chosenFile != null) {
                getTextField().setText(fileToTextValue(chosenFile));
            }
        });
    }

    protected abstract boolean validateFile(VirtualFile virtualFile);

    protected abstract FileChooserDescriptor createFileChooserDescriptor();

    protected VirtualFile getDefaultLocation() {
        return VfsUtil.getUserHomeDir();
    }

    protected String fileToTextValue(VirtualFile file) {
        return file.getCanonicalPath();
    }

    @NotNull
    protected VirtualFile parseTextToFile(@Nullable String text) {
        VirtualFile file = text == null ? editor.getDefaultValue() :
                LocalFileSystem.getInstance().findFileByPath(text);
        if (file == null || !validateFile(file)) {
            throw new InvalidDataException("is invalid");
        }
        return file;
    }

    private class FileTextFieldValueEditor extends TextFieldValueEditor<VirtualFile> {
        FileTextFieldValueEditor(String valueName, VirtualFile defValue) {
            super(FileChooseInput.this.getTextField(), valueName, defValue);
        }

        @NotNull
        @Override
        public VirtualFile parseValue(@Nullable String text) {
            return parseTextToFile(text);
        }

        @Override
        public String valueToString(@NotNull VirtualFile value) {
            return value.getPath();
        }

        @Override
        public boolean isValid(@NotNull VirtualFile virtualFile) {
            return FileChooseInput.this.validateFile(virtualFile);
        }
    }

}
