package grupo.unoeste.simuladorlfa;


import com.mifmif.common.regex.Generex;
import com.mifmif.common.regex.util.Iterator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Simulador {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite uma expressão regular: ");
        String teste = scanner.nextLine(); //aac para teste -> b(bb(c|b))
        teste = teste.replace(" ", "");
        //System.out.println(teste);
        int tam = 0;
        List<String> aux = new ArrayList<>();
        for (int i = 0; i < teste.length(); i++) {
            char c = teste.charAt(i);
            if(c == '(')
                tam ++;
            if (String.valueOf(c).matches("[a-zA-Z0-9]")) {
                aux.add(String.valueOf(c));
            }
        }
        String expressao = "";
        boolean vazio = false;
        if(teste.charAt(0) == '&') {
            teste = teste.replace("&|", "");
            vazio = true;
        }

        //b(bb(c|b)) -> b(bb[cb]) ((aa|b)aa)* -> ((aa|b)aa)*

        boolean flagP = false;
        int cont = 0;


        for (int i = 0; i < teste.length(); i++) {
            char j = teste.charAt(i);
            if(j == '(') {
                expressao += "(";
            } else if(aux.contains(j + "")) { //ca.(a|b)
                expressao += j;
            } else if(j == ')') {
                expressao += ")";
            } else if(j =='|') {
                expressao += '|';
            } else if(j == '*') {
                expressao += "*";
            } else if(j == '+') {
                expressao += "+";
            }
            if(flagP && j == '|') {
                expressao += "|";
            }
        }
        if(vazio) {
            expressao += "*";
        }
        System.out.println(expressao);
        System.out.println("OPÇÃO 1 - VALIDAR PALAVRA PARA EXPRESSÃO REGULAR DIGITADA");
        System.out.println("OPÇÃO 2 - GERAR 10 PALAVRAS PARA EXPRESSÃO REGULAR DIGITADA");
        System.out.println("OPÇÃO 0 - SAIR DO PROGRAMA");
        int op = scanner.nextInt();
        scanner.nextLine();
        while(op != 0) {
            switch (op) {
                case 1: {
                    System.out.println("Digite uma palavra para validação da expresssão: ");
                    String palavra = scanner.nextLine();
                    Pattern pattern = Pattern.compile(expressao);
                    Matcher matcher = pattern.matcher(palavra);
                    if(matcher.matches()) {
                        System.out.println("Palavra válida para a expressão regular: " + teste);
                    } else {
                        System.out.println("Palavra não é válida para a expressão regular: " + teste);
                    }
                    break;
                }
                case 2: {
                    try {
                        // Ponto positivo: Você já está usando 'expressao', o que é ótimo!
                        Generex generex = new Generex(expressao);

                        // Obtenha o iterador, que gera as palavras em ordem de comprimento e alfabética.
                        Iterator iterator = generex.iterator();

                        System.out.println("As 10 primeiras palavras geradas em ordem:");
                        List<String> palavrasGeradas = new ArrayList<>();
                        final int palavrasParaGerar = 10;
                        int palavrasGeradasContador = 0;

                        // Loop simples para pegar as 'palavrasParaGerar' primeiras palavras do iterador.
                        while (iterator.hasNext() && palavrasGeradasContador < palavrasParaGerar) {
                            String palavra = iterator.next(); // Sem conflito de nome agora
                            palavrasGeradas.add(palavra);
                            palavrasGeradasContador++;
                        }

                        // Exibe o resultado aplicando a limpeza
                        String alfabetoPermitido = String.join("", aux);
                        String regexParaRemover = "[^" + alfabetoPermitido + "]";

                        for (String p : palavrasGeradas) {
                            p = p.replaceAll(regexParaRemover, "");
                            System.out.println(p);
                        }

                        // Mensagem útil caso a expressão gere menos de 10 palavras no total.
                        if (palavrasGeradasContador < palavrasParaGerar && palavrasGeradasContador > 0) {
                            System.out.println("\n(Foram geradas apenas " + palavrasGeradasContador + " palavras, pois a expressão não pode gerar mais.)");
                        }
                        if (palavrasGeradasContador == 0 && !iterator.hasNext()) {
                            System.out.println("\n(A expressão não gera nenhuma palavra ou é muito complexa para o iterador.)");
                        }

                    } catch (Exception e) {
                        System.out.println("\nERRO: A expressão regular '" + expressao + "' é muito complexa ou usa recursos não suportados pelo gerador de palavras.");
                        System.out.println("O gerador em ordem (iterator) é mais restrito que o validador.");
                        // System.out.println("Detalhe do erro: " + e.getMessage()); // Descomente para depuração
                    }
                    break;
                }
                case 3: {
                    System.out.println("Digite uma expressão regular: ");
                    teste = scanner.nextLine();
                    teste = teste.replace(" ", "");
                    aux.clear();
                    for (int i = 0; i < teste.length(); i++) {
                        char c = teste.charAt(i);
                        if (c == '(')
                            tam++;
                        if (String.valueOf(c).matches("[a-zA-Z0-9]")) {
                            aux.add(String.valueOf(c));
                        }
                    }
                    expressao = "";
                    vazio = false;
                    if (teste.length() > 0 && teste.charAt(0) == '&') { // Adicionado teste de tamanho para evitar erro
                        teste = teste.replace("&|", "");
                        vazio = true;
                    }

                    for (int i = 0; i < teste.length(); i++) {
                        char j = teste.charAt(i);
                        if (j == '(') {
                            expressao += "(";
                        } else if (aux.contains(j + "")) {
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
    }

}