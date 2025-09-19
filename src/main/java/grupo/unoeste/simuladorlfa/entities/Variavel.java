package grupo.unoeste.simuladorlfa.entities;

import java.util.HashMap;
import java.util.Map;

public class Variavel {
    private String nome;
    private Map<String, Variavel> transicoes;

    public Variavel(String nome) {
        this.nome = nome;
        this.transicoes = new HashMap<>();
    }

    public String getNome() {
        return nome;
    }

    public void adicionarTransicao(String leitura, Variavel destino) {
        transicoes.put(leitura, destino);
    }

    public Variavel getDestino(String leitura) {
        return transicoes.get(leitura);
    }

    public Map<String, Variavel> getTransicoes() {
        return transicoes;
    }
}
