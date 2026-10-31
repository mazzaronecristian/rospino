package org.oristool.rospino2.computingNodes;

import java.util.*;

/**
 * Grafo non orientato con archi pesati (double).
 * I nodi sono identificati da un tipo generico T (tipicamente String o Integer).
 * La struttura interna è una mappa di adiacenza: nodo → lista di archi.
 */
//TODO: Implementare interfaccia Graph<N>
public class StronglyConnectedWeightedGraph<N, K> {

    // -------------------------------------------------------------------------
    // Classi interne
    // -------------------------------------------------------------------------

    /**
     * Mappa nodo → lista di archi uscenti (bidirezionale: ogni arco è presente in entrambe le direzioni).
     */
    private final Map<N, List<Edge<N, K>>> adjacency = new LinkedHashMap<>();

    public Set<N> keySet(){
        return adjacency.keySet();
    }
    /**
     * Aggiunge un nodo al grafo (senza archi).
     * Se il nodo esiste già, non fa nulla.
     *
     * @param id identificativo del nodo
     */
    public void addNode(N id, K selfLoopWeight) {
        if (adjacency.get(id) == null) {
            adjacency.put(id, new ArrayList<>());
            this.addEdge(id, id, selfLoopWeight);
        }
    }

    /**
     * Rimuove un nodo e tutti gli archi ad esso collegati.
     *
     * @param id identificativo del nodo
     * @throws NoSuchElementException se il nodo non esiste
     */
    public void removeNode(N id) {
        requireNode(id);
        adjacency.remove(id);
        // rimuove tutti gli archi che puntano al nodo eliminato
        for (List<Edge<N, K>> edges : adjacency.values()) {
            edges.removeIf(e -> e.to.equals(id));
        }
    }

    /**
     * Restituisce {@code true} se il nodo è presente nel grafo.
     */
    public boolean hasNode(N id) {
        return adjacency.containsKey(id);
    }

    /**
     * Restituisce una vista non modificabile dell'insieme dei nodi.
     */
    public Set<N> nodes() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    // -------------------------------------------------------------------------
    // Gestione archi
    // -------------------------------------------------------------------------

    /**
     * Aggiunge un arco bidirezionale pesato tra {@code from} e {@code to}.
     * Se uno dei due nodi non esiste, viene creato automaticamente.
     * Se l'arco esiste già, il peso viene aggiornato.
     *
     * @param from   nodo sorgente
     * @param to     nodo destinazione
     * @param weight peso dell'arco
     * @throws IllegalArgumentException se weight è NaN o infinito
     */
    public void addEdge(N from, N to, K weight) {
        if (weight == null) {
            throw new IllegalArgumentException("Non puoi impostare un peso a null!");
        }
        if (hasNode(from) && hasNode(to)) {
            setEdgeInList(adjacency.get(from), from, to, weight);
            setEdgeInList(adjacency.get(to), to, from, weight);
        }
    }

    /**
     * Rimuove l'arco bidirezionale tra {@code from} e {@code to}.
     *
     * @return {@code true} se l'arco era presente e viene rimosso
     */
    public boolean removeEdge(N from, N to) {
        if (!hasNode(from) || !hasNode(to)) return false;
        boolean removed = adjacency.get(from).removeIf(e -> e.to.equals(to));
        adjacency.get(to).removeIf(e -> e.to.equals(from));
        return removed;
    }

    /**
     * Restituisce {@code true} se esiste un arco tra {@code from} e {@code to}.
     */
    public boolean hasEdge(N from, N to) {
        return hasNode(from) && adjacency.get(from).stream().anyMatch(e -> e.to.equals(to));
    }

    /**
     * Restituisce il peso dell'arco tra {@code from} e {@code to}.
     *
     * @return il peso, oppure {@link OptionalDouble#empty()} se l'arco non esiste
     */
    public Optional<K> getWeight(N from, N to) {
        if (!hasNode(from)) return Optional.empty();
        return adjacency.get(from).stream()
                .filter(e -> e.to.equals(to))
                .map(e -> e.weight)
                .findFirst();
    }

    /**
     * Restituisce tutti gli archi del grafo (ogni arco una sola volta,
     * con from ≤ to secondo il comparatore naturale se T è Comparable).
     */
    public List<Edge<N,K>> edges() {
        Set<String> seen = new HashSet<>();
        List<Edge<N,K>> result = new ArrayList<>();
        for (List<Edge<N,K>> list : adjacency.values()) {
            for (Edge<N,K> e : list) {
                String key = edgeKey(e.from, e.to);
                if (seen.add(key)) result.add(e);
            }
        }
        return result;
    }

    /**
     * Restituisce la lista degli archi uscenti dal nodo {@code id}.
     */
    public List<Edge<N,K>> neighbors(N id) {
        requireNode(id);
        return Collections.unmodifiableList(adjacency.get(id));
    }

    // -------------------------------------------------------------------------
    // Statistiche
    // -------------------------------------------------------------------------

    /**
     * Numero di nodi.
     */
    public int nodeCount() {
        return adjacency.size();
    }

    /**
     * Numero di archi (contati una sola volta).
     */
    public int edgeCount() {
        return edges().size();
    }

    /**
     * Grado del nodo (numero di archi collegati).
     */
    public int degree(N id) {
        requireNode(id);
        return adjacency.get(id).size();
    }

    // -------------------------------------------------------------------------
    // Visite
    // -------------------------------------------------------------------------

    /**
     * Visita in ampiezza (BFS) a partire da {@code start}.
     *
     * @return lista di nodi nell'ordine di visita
     */
    public List<N> bfs(N start) {
        requireNode(start);
        List<N> order = new ArrayList<>();
        Set<N> visited = new HashSet<>();
        Queue<N> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            N current = queue.poll();
            order.add(current);
            for (Edge<N,K> e : adjacency.get(current)) {
                if (visited.add(e.to)) {
                    queue.add(e.to);
                }
            }
        }
        return order;
    }

    /**
     * Visita in profondità (DFS) a partire da {@code start}.
     *
     * @return lista di nodi nell'ordine di visita
     */
    public List<N> dfs(N start) {
        requireNode(start);
        List<N> order = new ArrayList<>();
        dfsHelper(start, new HashSet<>(), order);
        return order;
    }

    // -------------------------------------------------------------------------
    // Utilità private
    // -------------------------------------------------------------------------

    private void requireNode(N id) {
        if (!adjacency.containsKey(id)) {
            throw new NoSuchElementException("Nodo non trovato: " + id);
        }
    }

    /**
     * Inserisce o aggiorna un arco all'interno di una lista.
     */
    private void setEdgeInList(List<Edge<N, K>> list, N from, N to, K weight) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).to.equals(to)) {
                list.set(i, new Edge<>(from, to, weight));
                return;
            }
        }
        list.add(new Edge<>(from, to, weight));
    }

    /**
     * Chiave canonica per un arco non orientato (evita duplicati in {@link #edges()}).
     */
    private String edgeKey(N a, N b) {
        int cmp = a.toString().compareTo(b.toString());
        return cmp <= 0 ? a + "~" + b : b + "~" + a;
    }

    private void dfsHelper(N node, Set<N> visited, List<N> order) {
        visited.add(node);
        order.add(node);
        for (Edge<N,K> e : adjacency.get(node)) {
            if (!visited.contains(e.to)) {
                dfsHelper(e.to, visited, order);
            }
        }
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WeightedGraph {\n");
        for (Map.Entry<N, List<Edge<N, K>>> entry : adjacency.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" → ");
            StringJoiner sj = new StringJoiner(", ");
            for (Edge<N, K> e : entry.getValue()) {
                sj.add(e.to + "(" + e.weight + ")");
            }
            sb.append(sj).append("\n");
        }
        return sb.append("}").toString();
    }

    // -------------------------------------------------------------------------
    // Main dimostrativo
    // -------------------------------------------------------------------------

    /**
         * Rappresenta un arco pesato tra due nodi.
         */
        public record Edge<T, K>(T from, T to, K weight) {

        @Override
            public String toString() {
                return from + " --[" + weight + "]--> " + to;
            }
        }
}