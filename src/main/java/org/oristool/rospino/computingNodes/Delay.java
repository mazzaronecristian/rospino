package org.oristool.rospino.computingNodes;

import java.math.BigDecimal;

public class Delay {
    private final BigDecimal eft;
    private final BigDecimal lft;

    public Delay(BigDecimal eft, BigDecimal lft) {
        this.eft = eft;
        this.lft = lft;
    }
}