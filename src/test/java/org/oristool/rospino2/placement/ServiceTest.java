package org.oristool.rospino2.placement;

import org.junit.jupiter.api.Test;
import org.oristool.eulero.modeling.Activity;
import org.oristool.eulero.modeling.Composite;
import org.oristool.eulero.modeling.Simple;
import org.oristool.eulero.modeling.stochastictime.UniformTime;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceTest {
    @Test
    void includesSimpleRootWithDefaultResourceRequirement() {
        Simple simple = simple("root");
        Service service = new Service(simple);

        assertSame(simple, service.getActivity());
        assertEquals(Map.of(simple, 1), service.getResourcesPerSimple());
    }

    @Test
    void recursivelyCollectsSimpleActivitiesInTraversalOrder() {
        Simple first = simple("first");
        Simple nested = simple("nested");
        Simple last = simple("last");
        Composite inner = composite(nested);
        Composite root = composite(first, inner, last);

        Service service = new Service(root);

        assertSame(root, service.getActivity());
        assertEquals(List.of(first, nested, last), List.copyOf(service.getResourcesPerSimple().keySet()));
        assertEquals(List.of(1, 1, 1), List.copyOf(service.getResourcesPerSimple().values()));
    }

    @Test
    void allowsUpdatingRequirementsIncludingZero() {
        Simple simple = simple("simple");
        Service service = new Service(simple);

        service.setResourcesRequired(simple, 0);

        assertEquals(0, service.getResourcesPerSimple().get(simple));
    }

    @Test
    void rejectsNegativeRequirementsAndSimplesOutsideActivity() {
        Simple simple = simple("member");
        Service service = new Service(simple);

        assertThrows(IllegalArgumentException.class, () -> service.setResourcesRequired(simple, -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.setResourcesRequired(simple("outsider"), 2));
    }

    @Test
    void exposesResourceRequirementsAsReadOnlyMap() {
        Simple simple = simple("simple");
        Service service = new Service(simple);

        assertThrows(UnsupportedOperationException.class,
                () -> service.getResourcesPerSimple().put(simple, 3));
    }

    private static Composite composite(Activity... children) {
        Composite composite = new Composite();
        composite.setActivities(List.of(children));
        return composite;
    }

    private static Simple simple(String name) {
        return new Simple(name, new UniformTime(1, 1));
    }
}
