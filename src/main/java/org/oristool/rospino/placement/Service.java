package org.oristool.rospino.placement;

import org.oristool.eulero.modeling.Activity;

public class Service {
   private int demand;
   private int jobSize;
   private int ioTime;
   private Activity activity;

    public Service(Activity activity, int demand, int ioTime, int jobSize) {
        this.activity = activity;
        this.demand = demand;
        this.ioTime = ioTime;
        this.jobSize = jobSize;
    }
}