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

    // Classe para representar uma configuração do autômato durante a simulação
    public static class Configuration {
        private final Estado estado;
        private final int posicao;
        private final List<Estado> caminho;
        private final List<String> simbolosUsados;

        public Configuration(Estado estado, int posicao, List<Estado> caminho, List<String> simbolosUsados) {
            this.estado = estado;
            this.posicao = posicao;
            this.caminho = new ArrayList<>(caminho);
            this.simbolosUsados = new ArrayList<>(simbolosUsados);
        }

        public Estado getEstado() { return estado; }
        public int getPosicao() { return posicao; }
        public List<Estado> getCaminho() { return new ArrayList<>(caminho); }
        public List<String> getSimbolosUsados() { return new ArrayList<>(simbolosUsados); }
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

    // Método público para verificar determinismo (usado pelo controller)
    public static boolean isDeterministicPublic(List<Transicao> transicoes) {
        return isDeterministic(transicoes);
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
        String mensagem = aceito ? "Palavra aceita pelo AFD!" :
                String.format("Palavra rejeitada - terminou no estado %s (não final)", estadoAtual.getNome());

        return new SimulationResult(aceito, caminho, simbolosUsados, word, mensagem);
    }

    private static SimulationResult simulateNFA(Estado estadoInicial, List<Transicao> transicoes, String word) {
        // Implementação melhorada do NFA com busca em largura
        return simulateNFABreadthFirst(estadoInicial, transicoes, word);
    }

    private static SimulationResult simulateNFABreadthFirst(Estado estadoInicial, List<Transicao> transicoes, String word) {
        Queue<Configuration> fila = new LinkedList<>();
        Set<String> visitados = new HashSet<>();

        // Aplicar epsilon closure no estado inicial
        Set<Estado> estadosIniciais = epsilonClosure(Set.of(estadoInicial), transicoes);

        // Adicionar todas as configurações iniciais na fila
        for (Estado estado : estadosIniciais) {
            List<Estado> caminhoInicial = new ArrayList<>();
            List<String> simbolosInicial = new ArrayList<>();
            caminhoInicial.add(estadoInicial);

            // Se chegou por epsilon transition, adicionar ao caminho
            if (!estado.equals(estadoInicial)) {
                caminhoInicial.add(estado);
                simbolosInicial.add("ε");
            }

            fila.offer(new Configuration(estado, 0, caminhoInicial, simbolosInicial));
        }

        while (!fila.isEmpty()) {
            Configuration config = fila.poll();

            // Evitar loops infinitos
            String chave = config.getEstado().getNome() + ":" + config.getPosicao();
            if (visitados.contains(chave)) {
                continue;
            }
            visitados.add(chave);

            // Se processou toda a palavra
            if (config.getPosicao() >= word.length()) {
                if (config.getEstado().isFinal()) {
                    return new SimulationResult(true, config.getCaminho(), config.getSimbolosUsados(), word,
                            "Palavra aceita pelo AFND!");
                }
                continue;
            }

            String simboloAtual = String.valueOf(word.charAt(config.getPosicao()));

            // Processar transições normais
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(config.getEstado()) && t.aceitaSimbolo(simboloAtual)) {
                    List<Estado> novoCaminho = new ArrayList<>(config.getCaminho());
                    List<String> novosSimbolos = new ArrayList<>(config.getSimbolosUsados());

                    novoCaminho.add(t.getDestino());
                    novosSimbolos.add(simboloAtual);

                    // Aplicar epsilon closure no estado destino
                    Set<Estado> closure = epsilonClosure(Set.of(t.getDestino()), transicoes);
                    for (Estado estadoClosure : closure) {
                        List<Estado> caminhoFinal = new ArrayList<>(novoCaminho);
                        List<String> simbolosFinal = new ArrayList<>(novosSimbolos);

                        if (!estadoClosure.equals(t.getDestino())) {
                            caminhoFinal.add(estadoClosure);
                            simbolosFinal.add("ε");
                        }

                        fila.offer(new Configuration(estadoClosure, config.getPosicao() + 1,
                                caminhoFinal, simbolosFinal));
                    }
                }
            }

            // Processar epsilon transições sem consumir símbolo
            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(config.getEstado()) && t.isEpsilon()) {
                    List<Estado> novoCaminho = new ArrayList<>(config.getCaminho());
                    List<String> novosSimbolos = new ArrayList<>(config.getSimbolosUsados());

                    novoCaminho.add(t.getDestino());
                    novosSimbolos.add("ε");

                    fila.offer(new Configuration(t.getDestino(), config.getPosicao(),
                            novoCaminho, novosSimbolos));
                }
            }
        }

        // Se não encontrou caminho de aceitação, tentar mostrar um caminho parcial
        List<Estado> caminhoFalha = new ArrayList<>();
        List<String> simbolosFalha = new ArrayList<>();
        simulateNFAPartial(estadoInicial, transicoes, word, caminhoFalha, simbolosFalha);

        return new SimulationResult(false, caminhoFalha, simbolosFalha, word, "Palavra rejeitada pelo AFND!");
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

    // --- MÉTODOS PARA ANÁLISE DO AUTÔMATO ---

    public static Set<String> getAlphabet(List<Transicao> transicoes) {
        Set<String> alfabeto = new HashSet<>();
        for (Transicao t : transicoes) {
            for (String simbolo : t.getSimbolos()) {
                if (!simbolo.equals("ε")) {
                    alfabeto.add(simbolo);
                }
            }
        }
        return alfabeto;
    }

    public static boolean isComplete(List<Estado> estados, List<Transicao> transicoes) {
        if (!isDeterministic(transicoes)) {
            return false; // Completude só faz sentido para AFDs
        }

        Set<String> alfabeto = getAlphabet(transicoes);
        if (alfabeto.isEmpty()) {
            return true; // Autômato vazio é tecnicamente completo
        }

        // Verificar se cada estado tem transição para cada símbolo do alfabeto
        for (Estado estado : estados) {
            for (String simbolo : alfabeto) {
                boolean hasTransition = transicoes.stream()
                        .anyMatch(t -> t.getOrigem().equals(estado) && t.aceitaSimbolo(simbolo));
                if (!hasTransition) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<Estado> getUnreachableStates(List<Estado> estados, List<Transicao> transicoes) {
        Estado estadoInicial = estados.stream()
                .filter(Estado::isInicial)
                .findFirst()
                .orElse(null);

        if (estadoInicial == null) {
            return new ArrayList<>(estados); // Todos são inalcançáveis se não há inicial
        }

        Set<Estado> alcancaveis = new HashSet<>();
        Queue<Estado> fila = new LinkedList<>();
        fila.offer(estadoInicial);
        alcancaveis.add(estadoInicial);

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();

            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(atual) && !alcancaveis.contains(t.getDestino())) {
                    alcancaveis.add(t.getDestino());
                    fila.offer(t.getDestino());
                }
            }
        }

        List<Estado> inalcancaveis = new ArrayList<>();
        for (Estado estado : estados) {
            if (!alcancaveis.contains(estado)) {
                inalcancaveis.add(estado);
            }
        }
        return inalcancaveis;
    }

    public static boolean hasDeadStates(List<Estado> estados, List<Transicao> transicoes) {
        // Estado morto: não consegue alcançar nenhum estado final
        for (Estado estado : estados) {
            if (!estado.isFinal() && !canReachFinalState(estado, estados, transicoes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canReachFinalState(Estado estado, List<Estado> estados, List<Transicao> transicoes) {
        Set<Estado> visitados = new HashSet<>();
        Queue<Estado> fila = new LinkedList<>();
        fila.offer(estado);
        visitados.add(estado);

        while (!fila.isEmpty()) {
            Estado atual = fila.poll();

            if (atual.isFinal()) {
                return true;
            }

            for (Transicao t : transicoes) {
                if (t.getOrigem().equals(atual) && !visitados.contains(t.getDestino())) {
                    visitados.add(t.getDestino());
                    fila.offer(t.getDestino());
                }
            }
        }
        return false;
    }

    // Método para validar se o autômato está bem formado
    public static List<String> validateAutomaton(List<Estado> estados, List<Transicao> transicoes) {
        List<String> problemas = new ArrayList<>();

        if (estados.isEmpty()) {
            problemas.add("Nenhum estado definido");
            return problemas;
        }

        // Verificar estado inicial
        long initialStates = estados.stream().filter(Estado::isInicial).count();
        if (initialStates == 0) {
            problemas.add("Nenhum estado inicial definido");
        } else if (initialStates > 1) {
            problemas.add("Múltiplos estados iniciais (característico de AFND)");
        }

        // Verificar estados finais
        if (estados.stream().noneMatch(Estado::isFinal)) {
            problemas.add("Nenhum estado final definido");
        }

        // Verificar estados inalcançáveis
        List<Estado> inalcancaveis = getUnreachableStates(estados, transicoes);
        if (!inalcancaveis.isEmpty()) {
            problemas.add("Estados inalcançáveis encontrados: " +
                    inalcancaveis.stream().map(Estado::getNome).reduce((a, b) -> a + ", " + b).orElse(""));
        }

        // Verificar estados mortos (apenas para AFD)
        if (isDeterministic(transicoes) && hasDeadStates(estados, transicoes)) {
            problemas.add("Estados mortos encontrados (não alcançam estado final)");
        }

        return problemas;
    }
}