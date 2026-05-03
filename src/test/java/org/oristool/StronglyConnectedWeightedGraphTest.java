package org.oristool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.oristool.rospino.computingNodes.StronglyConnectedWeightedGraph;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità per WeightedGraph<T>.
 *
 * Dipendenze (JUnit 5):
 *   junit-jupiter-api:5.10+
 *   junit-jupiter-engine:5.10+  (runtime)
 *
 * Maven:
 *   <dependency>
 *     <groupId>org.junit.jupiter</groupId>
 *     <artifactId>junit-jupiter</artifactId>
 *     <version>5.10.2</version>
 *     <scope>test</scope>
 *   </dependency>
 */
@DisplayName("WeightedGraph")
class StronglyConnectedWeightedGraphTest {

    // =========================================================================
    // Fixture
    // =========================================================================

    private StronglyConnectedWeightedGraph<String, Double> g;

    @BeforeEach
    void setUp() {
        g = new StronglyConnectedWeightedGraph<>();
    }

    /** Grafo triangolo A-B-C riutilizzato in più suite. */
    private void buildTriangle() {
        g.addEdge("A", "B", 1.0);
        g.addEdge("B", "C", 2.0);
        g.addEdge("A", "C", 3.0);
    }

    // =========================================================================
    // Suite: gestione nodi
    // =========================================================================

    @Nested
    @DisplayName("Gestione nodi")
    class NodeTests {

        @Test
        @DisplayName("Grafo vuoto non contiene nodi")
        void emptyGraphHasNoNodes() {
            assertEquals(0, g.nodeCount());
            assertTrue(g.nodes().isEmpty());
        }

        @Test
        @DisplayName("addNode aggiunge il nodo")
        void addNodeInsertsNode() {
            g.addNode("X", (double) 0);
            assertTrue(g.hasNode("X"));
            assertEquals(1, g.nodeCount());
        }

        @Test
        @DisplayName("addNode è idempotente")
        void addNodeIdempotent() {
            g.addNode("X", (double) 0);
            g.addNode("X", (double) 0);
            assertEquals(1, g.nodeCount());
        }

        @Test
        @DisplayName("hasNode restituisce false per nodo inesistente")
        void hasNodeUnknown() {
            assertFalse(g.hasNode("Z"));
        }

        @Test
        @DisplayName("removeNode elimina il nodo")
        void removeNodeDeletesNode() {
            g.addNode("A", (double) 0);
            g.removeNode("A");
            assertFalse(g.hasNode("A"));
            assertEquals(0, g.nodeCount());
        }

        @Test
        @DisplayName("removeNode su nodo inesistente lancia NoSuchElementException")
        void removeNodeMissing() {
            assertThrows(NoSuchElementException.class, () -> g.removeNode("X"));
        }

        @Test
        @DisplayName("removeNode elimina anche gli archi incidenti")
        void removeNodeCascadesEdges() {
            buildTriangle();
            g.removeNode("B");

            assertFalse(g.hasEdge("A", "B"));
            assertFalse(g.hasEdge("B", "C"));
            // L'arco A-C deve sopravvivere
            assertTrue(g.hasEdge("A", "C"));
            assertEquals(1, g.edgeCount());
        }

        @Test
        @DisplayName("nodes() restituisce tutti i nodi presenti")
        void nodesReturnsAllNodes() {
            g.addNode("A", (double) 0 );
            g.addNode("B", (double) 0 );
            g.addNode("C", (double) 0 );
            assertEquals(Set.of("A", "B", "C"), g.nodes());
        }

        @Test
        @DisplayName("nodes() è una vista non modificabile")
        void nodesViewIsUnmodifiable() {
            g.addNode("A", (double) 0);
            Set<String> view = g.nodes();
            assertThrows(UnsupportedOperationException.class, () -> view.add("X"));
        }

        @Test
        @DisplayName("addNode con tipo Integer")
        void addNodeWithIntegerType() {
            StronglyConnectedWeightedGraph<Integer, Double> gi = new StronglyConnectedWeightedGraph<>();
            gi.addNode(42, (double) 0);
            assertTrue(gi.hasNode(42));
        }
    }

    // =========================================================================
    // Suite: gestione archi
    // =========================================================================

    @Nested
    @DisplayName("Gestione archi")
    class EdgeTests {

        @Test
        @DisplayName("addEdge crea i nodi automaticamente")
        void addEdgeCreatesNodes() {
            g.addEdge("A", "B", 5.0);
            assertTrue(g.hasNode("A"));
            assertTrue(g.hasNode("B"));
        }

        @Test
        @DisplayName("addEdge registra l'arco in entrambe le direzioni")
        void addEdgeBidirectional() {
            g.addEdge("A", "B", 5.0);
            assertTrue(g.hasEdge("A", "B"));
            assertTrue(g.hasEdge("B", "A"));
        }

        @Test
        @DisplayName("hasEdge restituisce false per arco inesistente")
        void hasEdgeUnknown() {
            g.addNode("A", (double) 0);
            g.addNode("B", (double) 0);
            assertFalse(g.hasEdge("A", "B"));
        }

        @Test
        @DisplayName("hasEdge restituisce false se il nodo sorgente non esiste")
        void hasEdgeUnknownSourceNode() {
            g.addNode("B", (double) 0);
            assertFalse(g.hasEdge("X", "B"));
        }

        @Test
        @DisplayName("getWeight restituisce il peso corretto")
        void getWeightReturnsCorrectValue() {
            g.addEdge("A", "B", 3.14);
            Optional<Double> w = g.getWeight("A", "B");
            assertTrue(w.isPresent());
            assertEquals(3.14, w.get(), 1e-9);
        }

        @Test
        @DisplayName("getWeight è simmetrico")
        void getWeightSymmetric() {
            g.addEdge("A", "B", 3.14);
            assertEquals(g.getWeight("A", "B").get(),
                    g.getWeight("B", "A").get(), 1e-9);
        }

        @Test
        @DisplayName("getWeight restituisce empty per arco inesistente")
        void getWeightMissingEdge() {
            g.addNode("A", (double) 0 );
            g.addNode("B", (double) 0 );
            assertTrue(g.getWeight("A", "B").isEmpty());
        }

        @Test
        @DisplayName("getWeight restituisce empty per nodo sorgente inesistente")
        void getWeightMissingSourceNode() {
            assertTrue(g.getWeight("X", "Y").isEmpty());
        }

        @Test
        @DisplayName("addEdge aggiorna il peso se l'arco esiste già")
        void addEdgeUpdatesWeight() {
            g.addEdge("A", "B", 1.0);
            g.addEdge("A", "B", 9.9);
            assertEquals(9.9, g.getWeight("A", "B").get(), 1e-9);
            assertEquals(9.9, g.getWeight("B", "A").get(), 1e-9);
            // L'arco non deve essere duplicato
            assertEquals(1, g.edgeCount());
        }

        @Test
        @DisplayName("addEdge accetta peso negativo")
        void addEdgeNegativeWeight() {
            assertDoesNotThrow(() -> g.addEdge("A", "B", -5.0));
            assertEquals(-5.0, g.getWeight("A", "B").get(), 1e-9);
        }

        @Test
        @DisplayName("addEdge accetta peso zero")
        void addEdgeZeroWeight() {
            assertDoesNotThrow(() -> g.addEdge("A", "B", 0.0));
            assertEquals(0.0, g.getWeight("A", "B").get(), 1e-9);
        }

        @Test
        @DisplayName("addEdge rifiuta pesi non finiti")
        void addEdgeInvalidWeight() {
            assertThrows(IllegalArgumentException.class, () -> g.addEdge("A", "B", null));
        }

        @Test
        @DisplayName("removeEdge rimuove l'arco in entrambe le direzioni")
        void removeEdgeBidirectional() {
            g.addEdge("A", "B", 1.0);
            boolean result = g.removeEdge("A", "B");
            assertTrue(result);
            assertFalse(g.hasEdge("A", "B"));
            assertFalse(g.hasEdge("B", "A"));
        }

        @Test
        @DisplayName("removeEdge restituisce false se l'arco non esiste")
        void removeEdgeMissing() {
            g.addNode("A", (double) 0 );
            g.addNode("B", (double) 0 );
            assertFalse(g.removeEdge("A", "B"));
        }

        @Test
        @DisplayName("removeEdge non altera altri archi")
        void removeEdgeDoesNotAffectOthers() {
            buildTriangle();
            g.removeEdge("A", "B");
            assertTrue(g.hasEdge("B", "C"));
            assertTrue(g.hasEdge("A", "C"));
        }

        @Test
        @DisplayName("removeEdge con nodo sorgente inesistente restituisce false")
        void removeEdgeMissingSourceNode() {
            g.addNode("B", (double) 0);
            assertFalse(g.removeEdge("X", "B"));
        }

        @Test
        @DisplayName("edges() non contiene duplicati")
        void edgesNoDuplicates() {
            buildTriangle();
            List<StronglyConnectedWeightedGraph.Edge<String, Double>> edgeList = g.edges();
            assertEquals(3, edgeList.size());
        }

        @Test
        @DisplayName("neighbors() è non modificabile")
        void neighborsUnmodifiable() {
            g.addEdge("A", "B", 1.0);
            List<StronglyConnectedWeightedGraph.Edge<String, Double>> nb = g.neighbors("A");
            assertThrows(UnsupportedOperationException.class,
                    () -> nb.remove(0));
        }

        @Test
        @DisplayName("neighbors() lancia NoSuchElementException per nodo inesistente")
        void neighborsMissingNode() {
            assertThrows(NoSuchElementException.class, () -> g.neighbors("X"));
        }
    }

    // =========================================================================
    // Suite: statistiche
    // =========================================================================

    @Nested
    @DisplayName("Statistiche")
    class StatsTests {

        @Test
        @DisplayName("nodeCount è corretto dopo inserimenti e rimozioni")
        void nodeCountAfterMutations() {
            g.addNode("A", (double) 0);
            g.addNode("B", (double) 0);
            assertEquals(2, g.nodeCount());
            g.removeNode("A");
            assertEquals(1, g.nodeCount());
        }

        @Test
        @DisplayName("edgeCount è corretto dopo inserimenti")
        void edgeCountAfterAdditions() {
            buildTriangle();
            assertEquals(3, g.edgeCount());
        }

        @Test
        @DisplayName("edgeCount è corretto dopo rimozione arco")
        void edgeCountAfterRemoval() {
            buildTriangle();
            g.removeEdge("A", "B");
            assertEquals(2, g.edgeCount());
        }

        @Test
        @DisplayName("degree è corretto")
        void degreeCorrect() {
            buildTriangle();
            assertEquals(2, g.degree("A")); // collegato a B e C
            assertEquals(2, g.degree("B")); // collegato a A e C
        }

        @Test
        @DisplayName("degree di nodo isolato è 0")
        void degreeIsolatedNode() {
            g.addNode("X", (double) 0);
            assertEquals(0, g.degree("X"));
        }

        @Test
        @DisplayName("degree lancia NoSuchElementException per nodo inesistente")
        void degreeMissingNode() {
            assertThrows(NoSuchElementException.class, () -> g.degree("X"));
        }
    }

    // =========================================================================
    // Suite: BFS
    // =========================================================================

    @Nested
    @DisplayName("BFS")
    class BfsTests {

        @Test
        @DisplayName("BFS su grafo con un solo nodo restituisce solo quel nodo")
        void bfsSingleNode() {
            g.addNode("A", (double) 0);
            assertEquals(List.of("A"), g.bfs("A"));
        }

        @Test
        @DisplayName("BFS visita tutti i nodi connessi")
        void bfsVisitsAllConnected() {
            buildTriangle();
            List<String> result = g.bfs("A");
            assertEquals(3, result.size());
            assertTrue(result.containsAll(List.of("A", "B", "C")));
        }

        @Test
        @DisplayName("BFS parte dal nodo sorgente")
        void bfsStartsFromSource() {
            buildTriangle();
            assertEquals("A", g.bfs("A").get(0));
        }

        @Test
        @DisplayName("BFS non visita nodi irraggiungibili")
        void bfsDisconnectedComponent() {
            g.addEdge("A", "B", 1.0);
            g.addNode("C", (double) 0); // componente isolata
            List<String> result = g.bfs("A");
            assertEquals(2, result.size());
            assertFalse(result.contains("C"));
        }

        @Test
        @DisplayName("BFS non cicla in presenza di cicli")
        void bfsNoCycleLoop() {
            g.addEdge("A", "B", 1.0);
            g.addEdge("B", "C", 1.0);
            g.addEdge("C", "A", 1.0);
            List<String> result = g.bfs("A");
            assertEquals(3, result.size()); // ogni nodo visitato una sola volta
        }

        @Test
        @DisplayName("BFS lancia NoSuchElementException per nodo inesistente")
        void bfsMissingNode() {
            assertThrows(NoSuchElementException.class, () -> g.bfs("X"));
        }
    }

    // =========================================================================
    // Suite: DFS
    // =========================================================================

    @Nested
    @DisplayName("DFS")
    class DfsTests {

        @Test
        @DisplayName("DFS su grafo con un solo nodo restituisce solo quel nodo")
        void dfsSingleNode() {
            g.addNode("A", (double) 0);
            assertEquals(List.of("A"), g.dfs("A"));
        }

        @Test
        @DisplayName("DFS visita tutti i nodi connessi")
        void dfsVisitsAllConnected() {
            buildTriangle();
            List<String> result = g.dfs("A");
            assertEquals(3, result.size());
            assertTrue(result.containsAll(List.of("A", "B", "C")));
        }

        @Test
        @DisplayName("DFS parte dal nodo sorgente")
        void dfsStartsFromSource() {
            buildTriangle();
            assertEquals("A", g.dfs("A").get(0));
        }

        @Test
        @DisplayName("DFS non visita nodi irraggiungibili")
        void dfsDisconnectedComponent() {
            g.addEdge("A", "B", 1.0);
            g.addNode("C", (double) 0);
            List<String> result = g.dfs("A");
            assertEquals(2, result.size());
            assertFalse(result.contains("C"));
        }

        @Test
        @DisplayName("DFS non cicla in presenza di cicli")
        void dfsNoCycleLoop() {
            g.addEdge("A", "B", 1.0);
            g.addEdge("B", "C", 1.0);
            g.addEdge("C", "A", 1.0);
            List<String> result = g.dfs("A");
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("DFS lancia NoSuchElementException per nodo inesistente")
        void dfsMissingNode() {
            assertThrows(NoSuchElementException.class, () -> g.dfs("X"));
        }
    }

    // =========================================================================
    // Suite: scenari integrati
    // =========================================================================

    @Nested
    @DisplayName("Scenari integrati")
    class IntegrationTests {

        @Test
        @DisplayName("Grafo completo K4: nodi, archi, gradi")
        void completeGraphK4() {
            String[] nodes = {"A", "B", "C", "D"};
            for (int i = 0; i < nodes.length; i++) {
                for (int j = i + 1; j < nodes.length; j++) {
                    g.addEdge(nodes[i], nodes[j], (double) (i + j));
                }
            }
            assertEquals(4, g.nodeCount());
            assertEquals(6, g.edgeCount());   // C(4,2) = 6
            for (String n : nodes) {
                assertEquals(3, g.degree(n)); // ogni nodo è collegato agli altri 3
            }
        }

        @Test
        @DisplayName("Grafo con tipo Integer funziona correttamente")
        void integerNodeType() {
            StronglyConnectedWeightedGraph<Integer, Double> gi = new StronglyConnectedWeightedGraph<>();
            gi.addEdge(1, 2, 10.0);
            gi.addEdge(2, 3, 20.0);
            assertTrue(gi.hasEdge(1, 2));
            assertTrue(gi.hasEdge(2, 1));
            assertEquals(2, gi.edgeCount());
            assertEquals(10.0, gi.getWeight(1, 2).get(), 1e-9);
        }

        @Test
        @DisplayName("Reset completo: rimuovere tutti i nodi svuota il grafo")
        void removeAllNodes() {
            buildTriangle();
            for (String n : new ArrayList<>(g.nodes())) {
                g.removeNode( n);
            }
            assertEquals(0, g.nodeCount());
            assertEquals(0, g.edgeCount());
        }

        @Test
        @DisplayName("Aggiunta e rimozione ripetuta mantiene la consistenza")
        void repeatedAddRemove() {
            g.addEdge("A", "B", 1.0);
            g.removeEdge("A", "B");
            g.addEdge("A", "B", 2.0);

            assertTrue(g.hasEdge("A", "B"));
            assertEquals(2.0, g.getWeight("A", "B").get(), 1e-9);
            assertEquals(1, g.edgeCount());
        }

        @Test
        @DisplayName("BFS e DFS coprono gli stessi nodi su grafo connesso")
        void bfsDfsSameCoverageOnConnected() {
            buildTriangle();
            List<String> bfsResult = g.bfs("A");
            List<String> dfsResult = g.dfs("A");
            assertEquals(new HashSet<>(bfsResult), new HashSet<>(dfsResult));
        }

        @Test
        @DisplayName("Catena lineare: BFS e DFS producono ordini diversi")
        void linearChainBfsDfsOrder() {
            // A - B - C - D
            g.addEdge("A", "B", 1.0);
            g.addEdge("B", "C", 1.0);
            g.addEdge("C", "D", 1.0);

            List<String> bfs = g.bfs("A");
            List<String> dfs = g.dfs("A");

            // Entrambi partono da A e coprono tutti i nodi
            assertEquals("A", bfs.get(0));
            assertEquals("A", dfs.get(0));
            assertEquals(4, bfs.size());
            assertEquals(4, dfs.size());

            // Su una catena lineare BFS e DFS producono lo stesso ordine
            assertEquals(bfs, dfs);
        }
    }
}