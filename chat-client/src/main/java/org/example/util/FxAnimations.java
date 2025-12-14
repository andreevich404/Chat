package org.example.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Утилиты анимаций JavaFX для плавного UI.
 */
public final class FxAnimations {

    private FxAnimations() {
    }

    public static void fadeInUp(Node node, int fromY, int toY, int ms) {
        node.setOpacity(0);
        node.setTranslateY(fromY);

        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), node);
        tt.setFromY(fromY);
        tt.setToY(toY);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.play();
    }

    public static void fade(Node node, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(from);
        ft.setToValue(to);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.play();
    }

    public static void shake(Node node) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(node.translateXProperty(), -6)),
                new KeyFrame(Duration.millis(100), new KeyValue(node.translateXProperty(), 6)),
                new KeyFrame(Duration.millis(150), new KeyValue(node.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), 5)),
                new KeyFrame(Duration.millis(250), new KeyValue(node.translateXProperty(), 0))
        );
        tl.play();
    }
}