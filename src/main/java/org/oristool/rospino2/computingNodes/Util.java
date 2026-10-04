package org.oristool.rospino2.computingNodes;

import org.oristool.eulero.modeling.stochastictime.StochasticTime;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

public class Util {

    public static StochasticTime getUniformZero() {
        return new UniformTime(0, 0);
    }
}