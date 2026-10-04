package org.oristool.rospino2.placement;

public class PlacementManager {
    private static PlacementManager instance;

    public static synchronized PlacementManager getInstance() {
        if (instance == null) {
            instance = new PlacementManager();
        }
        return instance;
    }

    public Placement constructPlacement(PlacementBuilder builder){
        //? Serve questa classe intermedia?
        return builder.build();
    }


}