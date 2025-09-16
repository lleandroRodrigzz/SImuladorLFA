package grupo.unoeste.simuladorlfa;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Simulador {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite uma expressão regular: ");
        String teste = scanner.nextLine();
        teste = teste.replace(" ", "");

        List<String> aux = new ArrayList<>();
        for (int i = 0; i < teste.length(); i++) {
            char c = teste.charAt(i);
            if (String.valueOf(c).matches("[a-zA-Z0-9]")) {
                aux.add(String.valueOf(c));
            }
        }

        String expressao = "";
        boolean vazio = false;

        if (teste.length() > 0 && teste.charAt(0) == '&') {
            teste = teste.replace("&|", "");
            vazio = true;
        }

        for (int i = 0; i < teste.length(); i++) {
            char j = teste.charAt(i);
            if (j == '(') {
                expressao += "(";
            } else if (aux.contains(String.valueOf(j))) {
                expressao += j;
            } else if (j == ')') {
                expressao += ")";
            } else if (j == '|') {
                expressao += '|';
            } else if (j == '*') {
                expressao += "*";
            } else if (j == '+') {
                expressao += "+";
            }
        }

        if (vazio) {
            expressao += "*";
        }

        System.out.println("Expressão processada: " + expressao);
        System.out.println("OPÇÃO 1 - VALIDAR PALAVRA PARA EXPRESSÃO REGULAR DIGITADA");
        System.out.println("OPÇÃO 2 - GERAR 10 PALAVRAS PARA EXPRESSÃO REGULAR DIGITADA");
        System.out.println("OPÇÃO 3 - INSERIR OUTRA EXPRESSÃO REGULAR");
        System.out.println("OPÇÃO 0 - SAIR DO PROGRAMA");

        int op = scanner.nextInt();
        scanner.nextLine();

        while (op != 0) {
            switch (op) {
                case 1: {
                    System.out.println("Digite uma palavra para validação da expressão: ");
                    String palavra = scanner.nextLine();
                    Pattern pattern = Pattern.compile(expressao);
                    Matcher matcher = pattern.matcher(palavra);
                    if (matcher.matches()) {
                        System.out.println("Palavra válida para a expressão regular: " + teste);
                    } else {
                        System.out.println("Palavra não é válida para a expressão regular: " + teste);
                    }
                    break;
                }
                case 2: {
                    System.out.println("Funcionalidade de geração de palavras não implementada nesta versão.");
                    System.out.println("Para implementar, seria necessário incluir a biblioteca Generex.");
                    break;
                }
                case 3: {
                    System.out.println("Digite uma expressão regular: ");
                    teste = scanner.nextLine();
                    teste = teste.replace(" ", "");
                    aux.clear();

                    for (int i = 0; i < teste.length(); i++) {
                        char c = teste.charAt(i);
                        if (String.valueOf(c).matches("[a-zA-Z0-9]")) {
                            aux.add(String.valueOf(c));
                        }
                    }

                    expressao = "";
                    vazio = false;

                    if (teste.length() > 0 && teste.charAt(0) == '&') {
                        teste = teste.replace("&|", "");
                        vazio = true;
                    }

                    for (int i = 0; i < teste.length(); i++) {
                        char j = teste.charAt(i);
                        if (j == '(') {
                            expressao += "(";
                        } else if (aux.contains(String.valueOf(j))) {
                            expressao += j;
                        } else if (j == ')') {
                            expressao += ")";
                        } else if (j == '|') {
                            expressao += '|';
                        } else if (j == '*') {
                            expressao += "*";
                        } else if (j == '+') {
                            expressao += "+";
                        }
                    }

                    if (vazio) {
                        expressao += "*";
                    }

                    System.out.println("Nova expressão configurada: " + expressao);
                    break;
                }
            }

            System.out.println("\nOPÇÃO 1 - VALIDAR PALAVRA PARA EXPRESSÃO REGULAR DIGITADA");
            System.out.println("OPÇÃO 2 - GERAR 10 PALAVRAS PARA EXPRESSÃO REGULAR DIGITADA");
            System.out.println("OPÇÃO 3 - INSERIR OUTRA EXPRESSÃO REGULAR");
            System.out.println("OPÇÃO 0 - SAIR DO PROGRAMA");
            op = scanner.nextInt();
            scanner.nextLine();
        }

        scanner.close();
        System.out.println("Programa encerrado.");
    }
}