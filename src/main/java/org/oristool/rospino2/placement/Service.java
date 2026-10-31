package org.oristool.rospino2.placement;

import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Simple;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Wraps a workflow activity and tracks the resource requirement of each simple activity. */
public class Service {
    private final Activity activity;
    private final Map<Simple, Integer> resourcesPerSimple = new LinkedHashMap<>();
    private final Map<Simple, Integer> readOnlyResourcesPerSimple = Collections.unmodifiableMap(resourcesPerSimple);

    public Service(Activity activity) {
        this.activity = Objects.requireNonNull(activity, "Activity cannot be null");
        collectSimpleActivities(activity, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public Activity getActivity() {
        return activity;
    }

    /** Returns an unmodifiable map in activity traversal order. */
    public Map<Simple, Integer> getResourcesPerSimple() {
        return readOnlyResourcesPerSimple;
    }

    /** Sets the non-negative resource requirement for a simple contained in this service. */
    public void setResourcesRequired(Simple simple, int resourcesRequired) {
        if (resourcesRequired < 0) {
            throw new IllegalArgumentException("Resource requirement cannot be negative");
        }
        if (!resourcesPerSimple.containsKey(simple)) {
            throw new IllegalArgumentException("Simple activity is not part of this service");
        }
        resourcesPerSimple.put(simple, resourcesRequired);
    }

    private void collectSimpleActivities(Activity current, Set<Activity> visited) {
        if (!visited.add(current)) {
            return;
        }
        if (current instanceof Simple simple) {
            resourcesPerSimple.putIfAbsent(simple, 1);
        }

        List<Activity> children = current.activities();
        if (children == null) {
            return;
        }
        for (Activity child : children) {
            if (child != null) {
                collectSimpleActivities(child, visited);
            }
        }
    }
}
