package org.oristool.rospino.computingNodes;

import java.util.*;

/**
 * Grafo non orientato con archi pesati (double).
 * I nodi sono identificati da un tipo generico T (tipicamente String o Integer).
 * La struttura interna è una mappa di adiacenza: nodo → lista di archi.
 */
public class WeightedGraph<T, K> {

    // -------------------------------------------------------------------------
    // Classi interne
    // -------------------------------------------------------------------------

    /**
     * Mappa nodo → lista di archi uscenti (bidirezionale: ogni arco è presente in entrambe le direzioni).
     */
    private final Map<T, List<Edge<T, K>>> adjacency = new LinkedHashMap<>();

    public Set<T> keySet(){
        return adjacency.keySet();
    }
    /**
     * Aggiunge un nodo al grafo (senza archi).
     * Se il nodo esiste già, non fa nulla.
     *
     * @param id identificativo del nodo
     */
    public void addNode(T id) {
        adjacency.putIfAbsent(id, new ArrayList<>());
    }

    /**
     * Rimuove un nodo e tutti gli archi ad esso collegati.
     *
     * @param id identificativo del nodo
     * @throws NoSuchElementException se il nodo non esiste
     */
    public void removeNode(T id) {
        requireNode(id);
        adjacency.remove(id);
        // rimuove tutti gli archi che puntano al nodo eliminato
        for (List<Edge<T, K>> edges : adjacency.values()) {
            edges.removeIf(e -> e.to.equals(id));
        }
    }

    /**
     * Restituisce {@code true} se il nodo è presente nel grafo.
     */
    public boolean hasNode(T id) {
        return adjacency.containsKey(id);
    }

    /**
     * Restituisce una vista non modificabile dell'insieme dei nodi.
     */
    public Set<T> nodes() {
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
    public void addEdge(T from, T to, K weight) {
        if (weight == null) {
            throw new IllegalArgumentException("Non puoi impostare un peso a null!");
        }
        addNode(from);
        addNode(to);
        setEdgeInList(adjacency.get(from), from, to, weight);
        setEdgeInList(adjacency.get(to), to, from, weight);
    }

    /**
     * Rimuove l'arco bidirezionale tra {@code from} e {@code to}.
     *
     * @return {@code true} se l'arco era presente e viene rimosso
     */
    public boolean removeEdge(T from, T to) {
        if (!hasNode(from) || !hasNode(to)) return false;
        boolean removed = adjacency.get(from).removeIf(e -> e.to.equals(to));
        adjacency.get(to).removeIf(e -> e.to.equals(from));
        return removed;
    }

    /**
     * Restituisce {@code true} se esiste un arco tra {@code from} e {@code to}.
     */
    public boolean hasEdge(T from, T to) {
        return hasNode(from) && adjacency.get(from).stream().anyMatch(e -> e.to.equals(to));
    }

    /**
     * Restituisce il peso dell'arco tra {@code from} e {@code to}.
     *
     * @return il peso, oppure {@link OptionalDouble#empty()} se l'arco non esiste
     */
    public Optional<K> getWeight(T from, T to) {
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
    public List<Edge<T,K>> edges() {
        Set<String> seen = new HashSet<>();
        List<Edge<T,K>> result = new ArrayList<>();
        for (List<Edge<T,K>> list : adjacency.values()) {
            for (Edge<T,K> e : list) {
                String key = edgeKey(e.from, e.to);
                if (seen.add(key)) result.add(e);
            }
        }
        return result;
    }

    /**
     * Restituisce la lista degli archi uscenti dal nodo {@code id}.
     */
    public List<Edge<T,K>> neighbors(T id) {
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
    public int degree(T id) {
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
    public List<T> bfs(T start) {
        requireNode(start);
        List<T> order = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            T current = queue.poll();
            order.add(current);
            for (Edge<T,K> e : adjacency.get(current)) {
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
    public List<T> dfs(T start) {
        requireNode(start);
        List<T> order = new ArrayList<>();
        dfsHelper(start, new HashSet<>(), order);
        return order;
    }

    // -------------------------------------------------------------------------
    // Utilità private
    // -------------------------------------------------------------------------

    private void requireNode(T id) {
        if (!adjacency.containsKey(id)) {
            throw new NoSuchElementException("Nodo non trovato: " + id);
        }
    }

    /**
     * Inserisce o aggiorna un arco all'interno di una lista.
     */
    private void setEdgeInList(List<Edge<T, K>> list, T from, T to, K weight) {
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
    private String edgeKey(T a, T b) {
        int cmp = a.toString().compareTo(b.toString());
        return cmp <= 0 ? a + "~" + b : b + "~" + a;
    }

    private void dfsHelper(T node, Set<T> visited, List<T> order) {
        visited.add(node);
        order.add(node);
        for (Edge<T,K> e : adjacency.get(node)) {
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
        for (Map.Entry<T, List<Edge<T, K>>> entry : adjacency.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" → ");
            StringJoiner sj = new StringJoiner(", ");
            for (Edge<T, K> e : entry.getValue()) {
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
    public static class Edge<T, K> {
        public final T from;
        public final T to;
        public final K weight;

        public Edge(T from, T to, K weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return from + " --[" + weight + "]--> " + to;
        }
    }
}