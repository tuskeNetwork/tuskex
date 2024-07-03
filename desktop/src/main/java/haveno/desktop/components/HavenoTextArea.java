package tuskex.desktop.components;

import com.jfoenix.controls.JFXTextArea;
import javafx.scene.control.Skin;

public class TuskexTextArea extends JFXTextArea {
    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextAreaSkinTuskexStyle(this);
    }
}
