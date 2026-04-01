package org.oristool.rospino.placement;

import org.oristool.eulero.modeling.deprecated.Activity;

public class Service {
   private int demand;
   private int jobSize;
   private int ioTime;
   private Activity activity;
   private final String id;

    public int getDemand() {
        return demand;
    }

    public Service setDemand(int demand) {
        this.demand = demand;
        return this;
    }

    public int getJobSize() {
        return jobSize;
    }

    public Service setJobSize(int jobSize) {
        this.jobSize = jobSize;
        return this;
    }

    public int getIoTime() {
        return ioTime;
    }

    public Service setIoTime(int ioTime) {
        this.ioTime = ioTime;
        return this;
    }

    public Activity getActivity() {
        return activity;
    }

    public Service setActivity(Activity activity) {
        this.activity = activity;
        return this;
    }

    public String getId() {
        return id;
    }

    public Service(Activity activity, int demand, int ioTime, int jobSize, String id) {
        this.activity = activity;
        this.demand = demand;
        this.ioTime = ioTime;
        this.jobSize = jobSize;
        this.id = id;
    }


    /**
     * t(a,r) := j(a)/r + i(a) tempo di risposta
     * r sono le risorse fornite
     * j <code>jobSize</code>, i <code>ioTime</code>
     */
    public double responseTime() {
        return (double) jobSize / demand + ioTime;
    }
}