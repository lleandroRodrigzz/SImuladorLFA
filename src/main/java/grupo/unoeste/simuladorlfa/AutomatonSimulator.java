package grupo.unoeste.simuladorlfa;

import grupo.unoeste.simuladorlfa.entities.Estado;
import grupo.unoeste.simuladorlfa.entities.Transicao;

import java.util.*;

public class AutomatonSimulator {

    public static class SimulationResult {
        private final boolean accepted;
        private final List<Estado> path;
        private final List<String> symbolsUsed;
        private final String word;
        private final String message;

        public SimulationResult(boolean accepted, List<Estado> path, List<String> symbolsUsed, String word, String message) {
            this.accepted = accepted;
            this.path = new ArrayList<>(path);
            this.symbolsUsed = new ArrayList<>(symbolsUsed);
            this.word = word;
            this.message = message;
        }

        public boolean isAccepted() { return accepted; }
        public List<Estado> getPath() { return new ArrayList<>(path); }
        public List<String> getSymbolsUsed() { return new ArrayList<>(symbolsUsed); }
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
            return new SimulationResult(false, new ArrayList<>(), new ArrayList<>(), word,
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
            // Para cada símbolo da transição, verificar se há conflito
            for (String simbolo : t.getSimbolos()) {
                String key = t.getOrigem().getNome() + ":" + simbolo;
                transitions.computeIfAbsent(key, k -> new HashSet<>()).add(t.getDestino().getNome());

                // Se há epsilon transições, é NFA
                if (simbolo.equals("ε") || simbolo.equals("")) {
                    return false;
                }
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
        List<String> simbolosUsados = new ArrayList<>();
        caminho.add(estadoAtual);

        for (int i = 0; i < word.length(); i++) {
            String simbolo = String.valueOf(word.charAt(i));
            Estado proximoEstado = null;

            // Buscar transição que aceite este símbolo
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(estadoAtual) && t.aceitaSimbolo(simbolo)) {
                    proximoEstado = t.getDestino();
                    break;
                }
            }

            if (proximoEstado == null) {
                return new SimulationResult(false, caminho, simbolosUsados, word,
                        String.format("Transição não encontrada do estado %s com símbolo '%s'",
                                estadoAtual.getNome(), simbolo));
            }

            estadoAtual = proximoEstado;
            caminho.add(estadoAtual);
            simbolosUsados.add(simbolo);
        }

        boolean aceito = estadoAtual.isFinal();
        String mensagem = aceito ? "Palavra aceita!" :
                String.format("Palavra rejeitada - terminou no estado %s (não final)", estadoAtual.getNome());

        return new SimulationResult(aceito, caminho, simbolosUsados, word, mensagem);
    }

    private static SimulationResult simulateNFA(Estado estadoInicial, List<Transicao> transicoes, String word) {
        // Implementação correta de NFA com epsilon closure
        List<Estado> caminhoValido = new ArrayList<>();
        List<String> simbolosUsados = new ArrayList<>();

        // Aplicar epsilon closure no estado inicial
        Set<Estado> estadosIniciais = epsilonClosure(Set.of(estadoInicial), transicoes);

        boolean resultado = false;
        // Tentar encontrar um caminho válido a partir de qualquer estado no epsilon closure inicial
        for (Estado estadoInicio : estadosIniciais) {
            caminhoValido.clear();
            simbolosUsados.clear();
            if (simulateNFARecursive(estadoInicio, transicoes, word, 0,
                    new HashSet<>(), caminhoValido, simbolosUsados)) {
                resultado = true;
                break;
            }
        }

        if (!caminhoValido.isEmpty() && resultado) {
            return new SimulationResult(true, caminhoValido, simbolosUsados, word, "Palavra aceita pelo NFA!");
        } else {
            // Se não encontrou caminho válido, tenta mostrar um caminho parcial
            List<Estado> caminhoFalha = new ArrayList<>();
            List<String> simbolosFalha = new ArrayList<>();
            simulateNFAPartial(estadoInicial, transicoes, word, caminhoFalha, simbolosFalha);

            return new SimulationResult(false, caminhoFalha, simbolosFalha, word, "Palavra rejeitada pelo NFA!");
        }
    }

    private static boolean simulateNFARecursive(Estado estadoAtual, List<Transicao> transicoes, String word,
                                                int indice, Set<String> visitados,
                                                List<Estado> caminho, List<String> simbolosUsados) {
        // Evitar loops infinitos em epsilon transitions com limite de profundidade
        String chave = estadoAtual.getNome() + ":" + indice;
        if (visitados.contains(chave)) {
            return false;
        }
        visitados.add(chave);

        caminho.add(estadoAtual);

        // Se processou toda a palavra
        if (indice >= word.length()) {
            // Ainda pode usar epsilon transitions para chegar a um estado final
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(estadoAtual) && t.isEpsilon()) {
                    simbolosUsados.add("ε");
                    if (simulateNFARecursive(t.getDestino(), transicoes, word, indice,
                            new HashSet<>(visitados), caminho, simbolosUsados)) {
                        return true;
                    }
                    simbolosUsados.remove(simbolosUsados.size() - 1);
                }
            }
            // Verifica se é estado final
            return estadoAtual.isFinal();
        }

        String simboloAtual = String.valueOf(word.charAt(indice));

        // Primeiro, tenta transições normais (não-epsilon)
        for (Transicao t : transicoes) {
            if (t.getOrigem().equals(estadoAtual) && t.aceitaSimbolo(simboloAtual)) {
                simbolosUsados.add(simboloAtual);
                if (simulateNFARecursive(t.getDestino(), transicoes, word, indice + 1,
                        new HashSet<>(visitados), caminho, simbolosUsados)) {
                    return true;
                }
                simbolosUsados.remove(simbolosUsados.size() - 1);
            }
        }

        // Depois, tenta epsilon transitions sem consumir símbolo
        for (Transicao t : transicoes) {
            if (t.getOrigem().equals(estadoAtual) && t.isEpsilon()) {
                simbolosUsados.add("ε");
                if (simulateNFARecursive(t.getDestino(), transicoes, word, indice,
                        new HashSet<>(visitados), caminho, simbolosUsados)) {
                    return true;
                }
                simbolosUsados.remove(simbolosUsados.size() - 1);
            }
        }

        caminho.remove(caminho.size() - 1);
        return false;
    }

    private static Set<Estado> epsilonClosure(Set<Estado> estados, List<Transicao> transicoes) {
        Set<Estado> closure = new HashSet<>(estados);
        Queue<Estado> fila = new LinkedList<>(estados);

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();

            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(atual) && t.isEpsilon()) {
                    if (!closure.contains(t.getDestino())) {
                        closure.add(t.getDestino());
                        fila.offer(t.getDestino());
                    }
                }
            }
        }

        return closure;
    }

    private static void simulateNFAPartial(Estado estadoInicial, List<Transicao> transicoes, String word,
                                           List<Estado> caminho, List<String> simbolosUsados) {
        Estado estadoAtual = estadoInicial;
        caminho.add(estadoAtual);

        for (int i = 0; i < word.length(); i++) {
            String simbolo = String.valueOf(word.charAt(i));
            boolean encontrouTransicao = false;

            // Busca primeira transição válida
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(estadoAtual) && t.aceitaSimbolo(simbolo)) {
                    estadoAtual = t.getDestino();
                    caminho.add(estadoAtual);
                    simbolosUsados.add(simbolo);
                    encontrouTransicao = true;
                    break;
                }
            }

            if (!encontrouTransicao) {
                break;
            }
        }
    }
}