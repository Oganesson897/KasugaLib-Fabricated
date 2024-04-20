package kasuga.lib.core.client.gui.layout.yoga;
import static org.lwjgl.util.yoga.Yoga.*;

public enum YogaWrap {
    NO_WRAP(YGWrapNoWrap),
    WRAP(YGWrapWrap),
    REVERSE(YGWrapReverse),
    ;

    private final int value;

    YogaWrap(int value) {
        this.value = value;
    }

    int getValue(){
        return value;
    }
}
