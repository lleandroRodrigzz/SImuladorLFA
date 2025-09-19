package grupo.unoeste.simuladorlfa;

import grupo.unoeste.simuladorlfa.entities.Variavel;

import java.util.*;

public class Gramatica {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Defina o número de variáveis:");
        int n = scanner.nextInt();
        scanner.nextLine();
        List<Variavel> var = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            System.out.println("Digite o nome da variável:");
            String nome = scanner.nextLine();
            Variavel novaVar = new Variavel(nome);

            System.out.println("Defina o número de produções:");
            int p = scanner.nextInt();
            scanner.nextLine();

            for (int j = 0; j < p; j++) {
                System.out.println("Digite o símbolo que lê:");
                String simbolo = scanner.nextLine();
                System.out.println("Digite a variável de destino (ENTER para vazio):");
                String destino = scanner.nextLine();

                if (destino.equals("")) {
                    novaVar.adicionarTransicao(simbolo, null);
                } else {
                    novaVar.adicionarTransicao(simbolo, new Variavel(destino));
                }
            }
            var.add(novaVar);
        }

        int opcao = -1;
        while (opcao != 6) {
            System.out.println("\n==== MENU ====");
            System.out.println("1 - Validar palavra (direto)");
            System.out.println("2 - Validar palavra (step by step)");
            System.out.println("3 - Mostrar transições");
            System.out.println("4 - Gerar GVTPS");
            System.out.println("5 - Gerar 10 primeiras palavras da linguagem");
            System.out.println("6 - Sair");
            System.out.print("Escolha: ");
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1 -> validarDireto(var, scanner);
                case 2 -> validarStepByStep(var, scanner);
                case 3 -> mostrarTransicoes(var);
                case 4 -> gerarGVTPS(var);
                case 5 -> gerar10PrimeirasPalavras(var);
                case 6 -> System.out.println("Encerrando...");
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    public static void gerarGVTPS(List<Variavel> var) {
        LinkedHashSet<String> alfabeto = gerarAlfabeto(var);
        String gvtps = "G = ({";
        String alfabeto2 = alfabeto.toString().replace("[", "{").replace("]", "}");
        for (int i = 0; i < var.size(); i++) {
            gvtps += var.get(i).getNome();
            if (i < var.size() - 1) {
                gvtps += ",";
            }
        }
        gvtps += "}, " + alfabeto2 + ", P, " + var.get(0).getNome() + ")\n";
        System.out.println(gvtps);
    }

    private static LinkedHashSet<String> gerarAlfabeto(List<Variavel> var) {
        LinkedHashSet<String> alfabeto = new LinkedHashSet<>();
        for (Variavel variavel : var) {
            for (String simbolo : variavel.getTransicoes().keySet()) {
                alfabeto.add(simbolo);
            }
        }
        return alfabeto;
    }

    public static void mostrarTransicoes(List<Variavel> var) {
        for (Variavel variavelAtual : var) {
            System.out.println("Transições da variável " + variavelAtual.getNome() + ":");
            for (String simbolo : variavelAtual.getTransicoes().keySet()) {
                Variavel destino = variavelAtual.getTransicoes().get(simbolo);
                System.out.println("  " + variavelAtual.getNome() + " --" + simbolo + "--> " +
                        (destino != null ? destino.getNome() : "ε"));
            }
        }
    }

    public static void validarDireto(List<Variavel> var, Scanner scanner) {
        System.out.println("Digite a palavra para validação:");
        String palavra = scanner.nextLine();
        while (!palavra.isEmpty()) {
            Variavel atual = var.get(0);
            boolean aceita = true;

            for (int idx = 0; idx < palavra.length() && aceita; idx++) {
                String simbolo = String.valueOf(palavra.charAt(idx));
                atual = buscarTransicao(simbolo, var, atual);

                if (atual == null && idx < palavra.length() - 1) {
                    aceita = false;
                }
                if (atual != null && idx == palavra.length() - 1) {
                    aceita = false;
                }
            }

            System.out.println(aceita ? "Palavra aceita." : "Palavra rejeitada.");
            System.out.println("Digite outra palavra (ENTER para voltar ao menu):");
            palavra = scanner.nextLine();
        }
    }

    public static void validarStepByStep(List<Variavel> var, Scanner scanner) {
        System.out.println("Digite a palavra para validação:");
        String palavra = scanner.nextLine();
        while (!palavra.isEmpty()) {
            Variavel atual = var.get(0);
            boolean aceita = true;

            for (int idx = 0; idx < palavra.length() && aceita; idx++) {
                String simbolo = String.valueOf(palavra.charAt(idx));
                System.out.println("Variável atual: " + atual.getNome() + ", Símbolo lido: " + simbolo);
                atual = buscarTransicao(simbolo, var, atual);

                if (atual == null && idx < palavra.length() - 1) {
                    aceita = false;
                }
                if (atual != null && idx == palavra.length() - 1) {
                    aceita = false;
                }

                System.out.println("Variável após transição: " + (atual != null ? atual.getNome() : "ε"));
                System.out.println("Pressione ENTER para continuar...");
                scanner.nextLine();
            }

            System.out.println(aceita ? "Palavra aceita." : "Palavra rejeitada.");
            System.out.println("Digite outra palavra (ENTER para voltar ao menu):");
            palavra = scanner.nextLine();
        }
    }

    public static Variavel buscarTransicao(String simbolo, List<Variavel> var, Variavel atual) {
        int i = 0;
        while (!Objects.equals(var.get(i).getNome(), atual.getNome()))
            i++;

        return var.get(i).getDestino(simbolo);
    }

    public static void gerar10PrimeirasPalavras(List<Variavel> var) {
        if (var.isEmpty()) {
            System.out.println("Nenhuma variável criada!");
            return;
        }

        List<String> alfabeto = new ArrayList<>(gerarAlfabeto(var));
        List<String> palavrasGeradas = new ArrayList<>();
        List<String> testadas = new ArrayList<>();
        testadas.add("");

        int cont = 0;
        int ini = 0;
        boolean flag = true;

        if (palavraAceita("", var)) {
            palavrasGeradas.add("");
            cont++;
        }

        while (cont < 10 && flag) {
            for (; flag && ini < testadas.size() && cont < 10; ini++) {
                for (int j = 0; flag && j < alfabeto.size() && cont < 10; j++) {
                    String palavra = testadas.get(ini) + alfabeto.get(j);
                    if (!testadas.contains(palavra)) {
                        testadas.add(palavra);
                        if (palavraAceita(palavra, var)) {
                            palavrasGeradas.add(palavra);
                            cont++;
                        } else if (palavra.length() > 10) { // limite de profundidade
                            flag = false;
                        }
                    }
                }
            }
            if (ini >= testadas.size()) {
                flag = false;
            }
        }

        if (!palavrasGeradas.isEmpty() && palavrasGeradas.get(0).isEmpty()) {
            palavrasGeradas.set(0, "ε");
        }

        System.out.println("Primeiras " + palavrasGeradas.size() + " palavras da linguagem:");
        for (String p : palavrasGeradas) {
            System.out.println("  " + p);
        }
    }

    public static boolean palavraAceita(String palavra, List<Variavel> var) {
        if (palavra.isEmpty()) {
            return var.get(0).getTransicoes().containsValue(null);
        }

        Variavel atual = var.get(0);
        for (int i = 0; i < palavra.length(); i++) {
            String simbolo = String.valueOf(palavra.charAt(i));
            atual = buscarTransicao(simbolo, var, atual);
            if (atual == null && i < palavra.length() - 1) return false;
            if (atual != null && i == palavra.length() - 1)
                return atual.getTransicoes().containsValue(null);
        }
        return true;
    }
}
