package grupo.unoeste.simuladorlfa;

import grupo.unoeste.simuladorlfa.entities.Estado;
import grupo.unoeste.simuladorlfa.entities.Transicao;

import java.util.*;

public class AutomatonSimulator {

    public static class SimulationResult {
        private final boolean accepted;
        private final List<Estado> path;
        private final String word;
        private final String message;

        public SimulationResult(boolean accepted, List<Estado> path, String word, String message) {
            this.accepted = accepted;
            this.path = new ArrayList<>(path);
            this.word = word;
            this.message = message;
        }

        public boolean isAccepted() { return accepted; }
        public List<Estado> getPath() { return new ArrayList<>(path); }
        public String getWord() { return word; }
        public String getMessage() { return message; }
    }

    public static SimulationResult simulate(List<Estado> estados, List<Transicao> transicoes, String word) {
        if (word == null) word = "";

        // Encontrar estado inicial
        Estado estadoInicial = null;
        for (Estado estado : estados) {
            if (estado.isInicial()) {
                estadoInicial = estado;
                break;
            }
        }

        if (estadoInicial == null) {
            return new SimulationResult(false, new ArrayList<>(), word,
                    "Erro: Nenhum estado inicial definido!");
        }

        // Verificar se é determinístico ou não-determinístico
        boolean isDeterministic = isDeterministic(transicoes);

        if (isDeterministic) {
            return simulateDFA(estadoInicial, transicoes, word);
        } else {
            return simulateNFA(estadoInicial, transicoes, word);
        }
    }

    private static boolean isDeterministic(List<Transicao> transicoes) {
        Map<String, Set<String>> transitions = new HashMap<>();

        for (Transicao t : transicoes) {
            String key = t.getOrigem().getNome() + ":" + t.getSimbolo();
            transitions.computeIfAbsent(key, k -> new HashSet<>()).add(t.getDestino().getNome());

            // Se há epsilon transições, é NFA
            if (t.getSimbolo().equals("ε") || t.getSimbolo().equals("")) {
                return false;
            }
        }

        // Se algum estado tem mais de uma transição para o mesmo símbolo, é NFA
        for (Set<String> destinations : transitions.values()) {
            if (destinations.size() > 1) {
                return false;
            }
        }

        return true;
    }

    private static SimulationResult simulateDFA(Estado estadoInicial, List<Transicao> transicoes, String word) {
        Estado estadoAtual = estadoInicial;
        List<Estado> caminho = new ArrayList<>();
        caminho.add(estadoAtual);

        for (int i = 0; i < word.length(); i++) {
            String simbolo = String.valueOf(word.charAt(i));
            Estado proximoEstado = null;

            // Buscar transição
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(estadoAtual) && t.getSimbolo().equals(simbolo)) {
                    proximoEstado = t.getDestino();
                    break;
                }
            }

            if (proximoEstado == null) {
                return new SimulationResult(false, caminho, word,
                        String.format("Transição não encontrada do estado %s com símbolo '%s'",
                                estadoAtual.getNome(), simbolo));
            }

            estadoAtual = proximoEstado;
            caminho.add(estadoAtual);
        }

        boolean aceito = estadoAtual.isFinal();
        String mensagem = aceito ? "Palavra aceita!" :
                String.format("Palavra rejeitada - terminou no estado %s (não final)", estadoAtual.getNome());

        return new SimulationResult(aceito, caminho, word, mensagem);
    }

    private static SimulationResult simulateNFA(Estado estadoInicial, List<Transicao> transicoes, String word) {
        Set<Estado> estadosAtuais = new HashSet<>();
        estadosAtuais.add(estadoInicial);

        // Aplicar epsilon closure
        estadosAtuais = epsilonClosure(estadosAtuais, transicoes);

        List<Estado> caminhoExemplo = new ArrayList<>();
        caminhoExemplo.add(estadoInicial);

        for (int i = 0; i < word.length(); i++) {
            String simbolo = String.valueOf(word.charAt(i));
            Set<Estado> novosEstados = new HashSet<>();

            for (Estado estado : estadosAtuais) {
                for (Transicao t : transicoes) {
                    if (t.getOrigem().equals(estado) && t.getSimbolo().equals(simbolo)) {
                        novosEstados.add(t.getDestino());
                    }
                }
            }

            if (novosEstados.isEmpty()) {
                return new SimulationResult(false, caminhoExemplo, word,
                        String.format("Nenhuma transição encontrada para o símbolo '%s'", simbolo));
            }

            estadosAtuais = epsilonClosure(novosEstados, transicoes);

            // Adicionar um exemplo de estado ao caminho
            if (!estadosAtuais.isEmpty()) {
                caminhoExemplo.add(estadosAtuais.iterator().next());
            }
        }

        // Verificar se algum estado final está no conjunto de estados atuais
        boolean aceito = estadosAtuais.stream().anyMatch(Estado::isFinal);

        String mensagem = aceito ? "Palavra aceita pelo NFA!" : "Palavra rejeitada pelo NFA!";

        return new SimulationResult(aceito, caminhoExemplo, word, mensagem);
    }

    private static Set<Estado> epsilonClosure(Set<Estado> estados, List<Transicao> transicoes) {
        Set<Estado> closure = new HashSet<>(estados);
        Queue<Estado> fila = new LinkedList<>(estados);

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();

            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(atual) &&
                        (t.getSimbolo().equals("ε") || t.getSimbolo().equals(""))) {

                    if (!closure.contains(t.getDestino())) {
                        closure.add(t.getDestino());
                        fila.offer(t.getDestino());
                    }
                }
            }
        }

        return closure;
    }
}