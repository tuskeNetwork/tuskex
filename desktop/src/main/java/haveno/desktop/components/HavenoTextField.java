package tuskex.desktop.components;

import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Skin;

public class TuskexTextField extends JFXTextField {

    public TuskexTextField(String value) {
        super(value);
    }

    public TuskexTextField() {
        super();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextFieldSkinTuskexStyle<>(this, 0);
    }
}
