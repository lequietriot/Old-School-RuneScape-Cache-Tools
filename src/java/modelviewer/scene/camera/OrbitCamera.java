package modelviewer.scene.camera;

import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import modelviewer.model.Xform;

public class OrbitCamera {

    private final SubScene subScene;
    private final Group root3D;

    private final double MAX_ZOOM = 300.0;

    public OrbitCamera(SubScene subScene, Group root) {
        this.subScene = subScene;
        this.root3D = root;
        init();
    }

    private void init() {
        camera.setNearClip(0.1D);
        camera.setFarClip(MAX_ZOOM * 1.15D);
        camera.getTransforms().addAll(
                yUpRotate,
                cameraPosition,
                cameraLookXRotate,
                cameraLookZRotate
        );

        Group rotateGroup = new Group();
        rotateGroup.getChildren().addAll(cameraXform);
        cameraXform.ry.setAngle(180);
        cameraXform.rx.setAngle(-18);
        cameraXform.getChildren().add(cameraXform2);
        cameraXform2.getChildren().add(cameraXform3);
        cameraXform3.getChildren().add(camera);
        cameraPosition.setZ(-cameraDistance);

        root3D.getChildren().addAll(rotateGroup);

        subScene.setCamera(camera);
        subScene.setOnScroll(event -> {

            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();

            if (deltaY < 0) {
                zoomFactor = 2.0 - zoomFactor;
            }
            double z = cameraPosition.getZ() / zoomFactor;
            z = Math.max(z, -MAX_ZOOM);
            z = Math.min(z, 10.0);
            cameraPosition.setZ(z);
        });

        subScene.setOnMousePressed(event -> {
            if (!event.isAltDown()) {
                mousePosX = event.getSceneX();
                mousePosY = event.getSceneY();
                mouseOldX = event.getSceneX();
                mouseOldY = event.getSceneY();
            }
        });

        subScene.setOnMouseDragged(event -> {
            if (!event.isAltDown()) {
                double modifier = 1.0;
                double modifierFactor = 0.3;

                if (event.isControlDown()) modifier = 0.1;
                if (event.isSecondaryButtonDown()) modifier = 0.035;

                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = event.getSceneX();
                mousePosY = event.getSceneY();
                mouseDeltaX = mousePosX - mouseOldX;
                mouseDeltaY = mousePosY - mouseOldY;

                double flip = -1.0;

                if (event.isSecondaryButtonDown()) {
                    double newX = cameraXform2.t.getX() + flip * mouseDeltaX * modifierFactor * modifier * 2.0;
                    double newY = cameraXform2.t.getY() + 1.0 * -mouseDeltaY * modifierFactor * modifier * 2.0;
                    cameraXform2.t.setX(newX);
                    cameraXform2.t.setY(newY);
                } else if (event.isPrimaryButtonDown()) {
                    double yAngle = cameraXform.ry.getAngle() - 1.0 * -mouseDeltaX * modifierFactor * modifier * 2.0;
                    double xAngle = cameraXform.rx.getAngle() + flip * mouseDeltaY * modifierFactor * modifier * 2.0;
                    cameraXform.ry.setAngle(yAngle);
                    cameraXform.rx.setAngle(xAngle);
                }
            }
        });
    }


    private final Camera camera = new PerspectiveCamera(true);
    private final Rotate cameraXRotate = new Rotate(-20.0, 0.0, 0.0, 0.0, Rotate.X_AXIS);
    private final Rotate cameraYRotate = new Rotate(-20.0, 0.0, 0.0, 0.0, Rotate.Y_AXIS);
    private final Rotate cameraLookXRotate = new Rotate(0.0, 0.0, 0.0, 0.0, Rotate.X_AXIS);
    private final Rotate cameraLookZRotate = new Rotate(0.0, 0.0, 0.0, 0.0, Rotate.Z_AXIS);
    private final Translate cameraPosition = new Translate(0.0, 0.0, 0.0);
    private Xform cameraXform = new Xform();
    private Xform cameraXform2 = new Xform();
    private Xform cameraXform3 = new Xform();
    private double cameraDistance = 25.0;
    private double mousePosX = 0;
    private double mousePosY = 0;
    private double mouseOldX = 0;
    private double mouseOldY = 0;
    private double mouseDeltaX = 0;
    private double mouseDeltaY = 0;
    private final Rotate yUpRotate = new Rotate(0.0, 0.0, 0.0, 0.0, Rotate.X_AXIS);
}
