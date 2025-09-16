package grupo.unoeste.simuladorlfa.entities;

public class Transicao {
    private final Estado origem;
    private final Estado destino;
    private final String simbolo; // O que a transição "lê"

    public Transicao(Estado origem, Estado destino, String simbolo) {
        this.origem = origem;
        this.destino = destino;
        this.simbolo = simbolo == null ? "" : simbolo.trim();
    }

    // Getters
    public Estado getOrigem() { return origem; }
    public Estado getDestino() { return destino; }
    public String getSimbolo() { return simbolo; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transicao transicao = (Transicao) obj;
        return origem.equals(transicao.origem) &&
                destino.equals(transicao.destino) &&
                simbolo.equals(transicao.simbolo);
    }

    @Override
    public int hashCode() {
        return origem.hashCode() + destino.hashCode() + simbolo.hashCode();
    }

    @Override
    public String toString() {
        return origem.getNome() + " --(" + simbolo + ")--> " + destino.getNome();
    }
}