package grupo.unoeste.simuladorlfa;

import grupo.unoeste.simuladorlfa.entities.Variavel;

import java.util.List;
import java.util.Scanner;

public class Gramatica {
    private List<String> alfabeto;

    public static void main(String[] args) {
        System.out.println("Defina o numero de variáveis.");
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        scanner.nextLine();
        int i = 0;
        List<Variavel> var = new java.util.ArrayList<>();
        while (i < n) {
            System.out.println("Digite o nome da variável:");
            String nome = scanner.nextLine();
            var.add(new Variavel(nome));
            System.out.println("Defina o número de produções.");
            int p = scanner.nextInt();
            scanner.nextLine();
            int j = 0;
            while (j < p) {
                System.out.println("Digite o simbolo que lê:");
                String simbolo = scanner.nextLine();
                System.out.println("Digite a variável de destino:");
                String destino = scanner.nextLine();
                var.get(i).adicionarTransicao(simbolo, new Variavel(destino));
                j++;
            }
            i++;
        }
        int k = 0;
        while (k < var.size()) {
            Variavel variavelAtual = var.get(k);
            System.out.println("Transições da variável " + variavelAtual.getNome() + ":");
            for (String simbolo : variavelAtual.getTransicoes().keySet()) {
                Variavel destino = variavelAtual.getTransicoes().get(simbolo);
                System.out.println("  " + variavelAtual.getNome() + " --" + simbolo + "--> " + destino.getNome());
            }
            k++;
        }
    }
}
