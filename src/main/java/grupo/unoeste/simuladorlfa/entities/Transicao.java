package grupo.unoeste.simuladorlfa.entities;

import java.util.*;
import java.util.stream.Collectors;

public class Transicao {
    private final Estado origem;
    private final Estado destino;
    private List<String> simbolos; // Lista de símbolos que a transição "lê"
    private String simboloOriginal; // String original para visualização

    public Transicao(Estado origem, Estado destino, String simbolos) {
        this.origem = origem;
        this.destino = destino;
        this.simboloOriginal = simbolos == null ? "" : simbolos.trim();
        this.simbolos = new ArrayList<>(parseSimbolos(this.simboloOriginal));
    }

    private List<String> parseSimbolos(String input) {
        if (input.isEmpty() || input.equals("ε")) {
            return Arrays.asList("ε");
        }

        // Suporte para formato "a,b,c" - separa por vírgula e remove espaços
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Getters
    public Estado getOrigem() { return origem; }
    public Estado getDestino() { return destino; }
    public String getSimbolo() { return simboloOriginal; } // Para compatibilidade
    public List<String> getSimbolos() { return new ArrayList<>(simbolos); }

    // Método para atualizar os símbolos da transição
    public void setSimbolo(String novoSimbolo) {
        this.simboloOriginal = novoSimbolo == null ? "" : novoSimbolo.trim();
        // Criar nova lista mutável
        List<String> novosSimbolos = parseSimbolos(this.simboloOriginal);
        this.simbolos.clear();
        this.simbolos.addAll(novosSimbolos);
    }

    // Método para verificar se um símbolo específico é aceito por esta transição
    public boolean aceitaSimbolo(String simbolo) {
        return simbolos.contains(simbolo);
    }

    // Método para verificar se é uma transição epsilon
    public boolean isEpsilon() {
        return simbolos.contains("ε") || simbolos.contains("") || simboloOriginal.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transicao transicao = (Transicao) obj;
        return origem.equals(transicao.origem) &&
                destino.equals(transicao.destino) &&
                simboloOriginal.equals(transicao.simboloOriginal);
    }

    @Override
    public int hashCode() {
        return origem.hashCode() + destino.hashCode() + simboloOriginal.hashCode();
    }

    @Override
    public String toString() {
        return origem.getNome() + " --(" + simboloOriginal + ")--> " + destino.getNome();
    }

    public String getSimboloOriginal() {
        return simboloOriginal;
    }
}