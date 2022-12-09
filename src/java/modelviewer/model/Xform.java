package modelviewer.model;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class Xform extends Group {

    Translate t = new Translate();
    Translate p = new Translate();
    Rotate rx = new Rotate();
    Rotate ry = new Rotate();
    Rotate rz = new Rotate();
    Scale s = new Scale();

    public Xform() {
        rx.setAxis(Rotate.X_AXIS);
        ry.setAxis(Rotate.Y_AXIS);
        rz.setAxis(Rotate.Z_AXIS);
        getTransforms().addAll(t, rz, ry, rx, s);
    }
}
