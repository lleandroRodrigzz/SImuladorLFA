package grupo.unoeste.simuladorlfa;

import grupo.unoeste.simuladorlfa.entities.Estado;
import grupo.unoeste.simuladorlfa.entities.Transicao;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AutomatonController {

    @FXML private Pane drawingPane;
    @FXML private TextField wordTextField;
    @FXML private Button simulateButton;
    @FXML private Button clearButton;
    @FXML private Label resultLabel;
    @FXML private TextArea logTextArea;
    @FXML private VBox controlPanel;

    // --- Modelo de Dados ---
    private final List<Estado> estados = new ArrayList<>();
    private final List<Transicao> transicoes = new ArrayList<>();

    // --- Mapeamento para a Visão ---
    private final Map<Estado, Group> estadoViews = new HashMap<>();
    private final Map<Transicao, Node> transicaoViews = new HashMap<>();

    // --- Lógica para criação de transições ---
    private Group estadoOrigemView = null;
    private Line linhaDeTransicaoTemporaria = null;

    // --- Para animação da simulação ---
    private final List<Group> highlightedStates = new ArrayList<>();

    // --- Constantes para melhorar a usabilidade ---
    private static final double STATE_RADIUS = 25.0;
    private static final double HIT_RADIUS = 35.0; // Área de clique maior que o círculo visível

    @FXML
    public void initialize() {
        setupDrawingPaneEvents();
        setupControlButtons();

        logTextArea.setEditable(false);
        logTextArea.setFont(Font.font("Consolas", 12));
        wordTextField.setOnAction(e -> simulateWord());

        logMessage("Simulador de Autômatos Finitos iniciado!");
        logMessage("- Clique no painel para criar estados.");
        logMessage("- Arraste de um estado para outro para criar transições.");
        logMessage("- Clique direito nos elementos para mais opções.");
        logMessage("- Use vírgulas para múltiplos símbolos: 'a,b,c'");
    }

    private void setupDrawingPaneEvents() {
        // Criar estado com clique primário no painel
        drawingPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == drawingPane) {
                criarNovoEstado(event.getX(), event.getY());
            }
        });

        // Desenhar linha temporária ao arrastar para criar transição
        drawingPane.setOnMouseDragged(event -> {
            if (linhaDeTransicaoTemporaria != null) {
                linhaDeTransicaoTemporaria.setEndX(event.getX());
                linhaDeTransicaoTemporaria.setEndY(event.getY());
            }
        });

        // Finalizar criação da transição ao soltar o mouse
        drawingPane.setOnMouseReleased(event -> {
            if (estadoOrigemView != null) {
                Group estadoDestinoView = findNearestStateGroup(event.getX(), event.getY());

                // Se soltou sobre um estado válido
                if (estadoDestinoView != null) {
                    Estado origem = (Estado) estadoOrigemView.getUserData();
                    Estado destino = (Estado) estadoDestinoView.getUserData();
                    criarNovaTransicao(origem, destino);
                }

                // Limpa a linha temporária e reseta a origem
                if (linhaDeTransicaoTemporaria != null) {
                    drawingPane.getChildren().remove(linhaDeTransicaoTemporaria);
                }
                linhaDeTransicaoTemporaria = null;
                estadoOrigemView = null;
            }
        });
    }

    private void setupControlButtons() {
        simulateButton.setOnAction(e -> simulateWord());
        clearButton.setOnAction(e -> clearAll());
    }

    // --- MÉTODOS DE CRIAÇÃO E MANIPULAÇÃO DE ESTADOS ---

    private void criarNovoEstado(double x, double y) {
        Estado novoEstado = new Estado(x, y);
        estados.add(novoEstado);

        Group estadoView = criarVisualizacaoEstado(novoEstado);
        estadoViews.put(novoEstado, estadoView);
        drawingPane.getChildren().add(estadoView);

        logMessage("Estado " + novoEstado.getNome() + " criado.");
    }

    private Group criarVisualizacaoEstado(Estado estado) {
        Group group = new Group();
        group.setUserData(estado);
        group.setLayoutX(estado.getX());
        group.setLayoutY(estado.getY());

        // Círculo invisível para área de clique (hitbox)
        Circle hitArea = new Circle(0, 0, HIT_RADIUS, Color.TRANSPARENT);
        hitArea.setId("hit-area");

        // Círculo principal (visual)
        Circle circle = new Circle(0, 0, STATE_RADIUS, Color.LIGHTBLUE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2);
        circle.setId("main-circle");

        // Círculo interno para estado final
        Circle innerCircle = new Circle(0, 0, STATE_RADIUS - 5, Color.TRANSPARENT);
        innerCircle.setStroke(Color.BLACK);
        innerCircle.setStrokeWidth(1.5);
        innerCircle.setVisible(false);
        innerCircle.setId("inner-circle");

        // Seta para estado inicial
        Polygon arrow = new Polygon(-STATE_RADIUS - 12, -7.0, -STATE_RADIUS, 0.0, -STATE_RADIUS - 12, 7.0);
        arrow.setFill(Color.BLACK);
        arrow.setVisible(false);
        arrow.setId("initial-arrow");

        // Texto do nome
        Text text = new Text(estado.getNome());
        text.setBoundsType(TextBoundsType.VISUAL);
        text.setFont(Font.font(14));
        text.setX(-text.getLayoutBounds().getWidth() / 2);
        text.setY(text.getLayoutBounds().getHeight() / 4);
        text.setId("state-name-text");

        group.getChildren().addAll(hitArea, circle, innerCircle, arrow, text);

        // Listeners para atualizar a view quando o modelo (Estado) muda
        estado.isInicialProperty().addListener((obs, oldVal, newVal) -> {
            arrow.setVisible(newVal);
            if (newVal) {
                // Garante que apenas um estado seja inicial
                estados.stream()
                        .filter(e -> e != estado && e.isInicial())
                        .forEach(e -> e.setInicial(false));
                logMessage("Estado " + estado.getNome() + " marcado como inicial.");
            }
        });

        estado.isFinalProperty().addListener((obs, oldVal, newVal) -> {
            innerCircle.setVisible(newVal);
            logMessage("Estado " + estado.getNome() + " " + (newVal ? "marcado como final." : "desmarcado como final."));
        });

        setupStateEvents(group);
        return group;
    }

    private void setupStateEvents(Group group) {
        Estado estado = (Estado) group.getUserData();
        Circle circle = (Circle) group.lookup("#main-circle");

        // Menu de contexto (clique direito)
        ContextMenu contextMenu = new ContextMenu();
        MenuItem marcarInicialItem = new MenuItem();
        MenuItem marcarFinalItem = new MenuItem();
        MenuItem renomearItem = new MenuItem("Renomear");
        MenuItem deletarItem = new MenuItem("Deletar");

        marcarInicialItem.setOnAction(e -> estado.setInicial(!estado.isInicial()));
        marcarFinalItem.setOnAction(e -> estado.setFinal(!estado.isFinal()));
        renomearItem.setOnAction(e -> renomearEstado(estado));
        deletarItem.setOnAction(e -> deletarEstado(group));

        contextMenu.getItems().addAll(marcarInicialItem, marcarFinalItem, new SeparatorMenuItem(), renomearItem, deletarItem);

        group.setOnContextMenuRequested(event -> {
            marcarInicialItem.setText(estado.isInicial() ? "✓ Desmarcar como Inicial" : "Marcar como Inicial");
            marcarFinalItem.setText(estado.isFinal() ? "✓ Desmarcar como Final" : "Marcar como Final");
            contextMenu.show(group, event.getScreenX(), event.getScreenY());
        });

        // Iniciar criação de transição (arrastar)
        group.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                estadoOrigemView = group;
                linhaDeTransicaoTemporaria = new Line(
                        estado.getX(), estado.getY(),
                        event.getSceneX(), event.getSceneY()
                );
                linhaDeTransicaoTemporaria.getStrokeDashArray().addAll(5d, 5d);
                linhaDeTransicaoTemporaria.setStroke(Color.GRAY);
                drawingPane.getChildren().add(linhaDeTransicaoTemporaria);
                event.consume();
            }
        });

        // Efeito de highlight ao passar o mouse
        group.setOnMouseEntered(e -> {
            if (!highlightedStates.contains(group)) circle.setStroke(Color.ROYALBLUE);
        });
        group.setOnMouseExited(e -> {
            if (!highlightedStates.contains(group)) circle.setStroke(Color.BLACK);
        });
    }

    private void renomearEstado(Estado estado) {
        TextInputDialog dialog = new TextInputDialog(estado.getNome());
        dialog.setTitle("Renomear Estado");
        dialog.setHeaderText("Digite o novo nome para o estado '" + estado.getNome() + "'.");
        dialog.setContentText("Novo nome:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                String oldName = estado.getNome();
                estado.setNome(nome.trim());
                atualizarVisualizacaoEstado(estado);
                logMessage("Estado " + oldName + " renomeado para " + estado.getNome());
            }
        });
    }

    private void atualizarVisualizacaoEstado(Estado estado) {
        Group group = estadoViews.get(estado);
        if (group != null) {
            Text text = (Text) group.lookup("#state-name-text");
            text.setText(estado.getNome());
            text.setX(-text.getLayoutBounds().getWidth() / 2);
        }
    }

    private void deletarEstado(Group estadoView) {
        Estado estadoParaDeletar = (Estado) estadoView.getUserData();

        // Remove transições conectadas (modelo e view)
        List<Transicao> transicoesParaRemover = new ArrayList<>();
        transicoes.forEach(t -> {
            if (t.getOrigem() == estadoParaDeletar || t.getDestino() == estadoParaDeletar) {
                transicoesParaRemover.add(t);
            }
        });
        transicoesParaRemover.forEach(this::deletarTransicao);

        // Remove o estado (modelo e view)
        drawingPane.getChildren().remove(estadoView);
        estadoViews.remove(estadoParaDeletar);
        estados.remove(estadoParaDeletar);

        logMessage("Estado " + estadoParaDeletar.getNome() + " e suas transições foram deletados.");
    }

    // --- MÉTODOS DE CRIAÇÃO E MANIPULAÇÃO DE TRANSIÇÕES ---

    private void criarNovaTransicao(Estado origem, Estado destino) {
        String header = "Digite o símbolo(s) para a transição de " +
                origem.getNome() + " para " + destino.getNome() + ":";

        Optional<String> result = showTransitionSymbolDialog("Símbolo da Transição", header, "");

        result.ifPresent(simboloInput -> {
            String simbolo = simboloInput.trim();
            // Se vazio, usar epsilon
            if (simbolo.isEmpty()) {
                simbolo = "ε";
            }

            Transicao novaTransicao = new Transicao(origem, destino, simbolo);
            transicoes.add(novaTransicao);

            Node transicaoView = criarVisualizacaoTransicao(novaTransicao);
            transicaoViews.put(novaTransicao, transicaoView);
            drawingPane.getChildren().add(transicaoView);
            transicaoView.toBack();

            logMessage("Transição criada: " + novaTransicao);
            if (novaTransicao.getSimbolos().size() > 1) {
                logMessage("  -> Aceita símbolos: " + String.join(", ", novaTransicao.getSimbolos()));
            }
        });
    }

    private Node criarVisualizacaoTransicao(Transicao transicao) {
        Estado origem = transicao.getOrigem();
        Estado destino = transicao.getDestino();
        Group group = new Group();

        if (origem == destino) {
            // Círculo para o loop (sem alterações aqui)
            Circle loop = new Circle(origem.getX(), origem.getY() - (STATE_RADIUS + 15), 15);
            loop.setFill(Color.TRANSPARENT);
            loop.setStroke(Color.BLACK);
            loop.setStrokeWidth(1.5);

            // Seta para o loop - CORREÇÃO AQUI
            // Ajusta os pontos para que a seta aponte para a parte superior do círculo do loop
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(
                    origem.getX() + 10, origem.getY() - (STATE_RADIUS + 15) - 15, // Ponto inicial (topo do loop)
                    origem.getX() + 15, origem.getY() - (STATE_RADIUS + 15) - 5,  // Ponto inferior direito
                    origem.getX() + 5, origem.getY() - (STATE_RADIUS + 15) - 5    // Ponto inferior esquerdo
            );
            arrow.setFill(Color.BLACK);

            // Texto (também vamos ajustar a posição para ficar melhor)
            Text texto = new Text(origem.getX() - 10, origem.getY() - (STATE_RADIUS + 35), transicao.getSimbolo());
            texto.setFont(Font.font(12));

            group.getChildren().addAll(loop, arrow, texto);
        }
        else {
            // Transição normal
            double angle = Math.atan2(destino.getY() - origem.getY(), destino.getX() - origem.getX());

            double startX = origem.getX() + STATE_RADIUS * Math.cos(angle);
            double startY = origem.getY() + STATE_RADIUS * Math.sin(angle);
            double endX = destino.getX() - STATE_RADIUS * Math.cos(angle);
            double endY = destino.getY() - STATE_RADIUS * Math.sin(angle);

            Line linha = new Line(startX, startY, endX, endY);
            linha.setStrokeWidth(2.0);

            // Seta na ponta da linha
            double arrowLength = 12;
            double arrowAngle = Math.toRadians(25);
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(
                    endX, endY,
                    endX - arrowLength * Math.cos(angle - arrowAngle), endY - arrowLength * Math.sin(angle - arrowAngle),
                    endX - arrowLength * Math.cos(angle + arrowAngle), endY - arrowLength * Math.sin(angle + arrowAngle)
            );
            arrow.setFill(Color.BLACK);

            // Texto no meio da linha
            Text texto = new Text(transicao.getSimbolo());
            texto.setFont(Font.font("Arial", 12));
            double textX = (startX + endX) / 2;
            double textY = (startY + endY) / 2 - 8;
            texto.setX(textX - texto.getLayoutBounds().getWidth() / 2);
            texto.setY(textY);

            group.getChildren().addAll(linha, arrow, texto);
        }

        // Menu de contexto para a transição
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editarItem = new MenuItem("Editar Símbolo(s)");
        MenuItem deletarItem = new MenuItem("Deletar Transição");

        editarItem.setOnAction(e -> editarTransicao(transicao));
        deletarItem.setOnAction(e -> deletarTransicao(transicao));

        contextMenu.getItems().addAll(editarItem, deletarItem);
        group.setOnContextMenuRequested(event ->
                contextMenu.show(group, event.getScreenX(), event.getScreenY()));

        return group;
    }

    // Em grupo.unoeste.simuladorlfa.AutomatonController

    private void editarTransicao(Transicao transicao) {
        String header = "Edite o símbolo(s) da transição de " +
                transicao.getOrigem().getNome() + " para " + transicao.getDestino().getNome() + ":";

        Optional<String> result = showTransitionSymbolDialog("Editar Transição", header, transicao.getSimbolo());

        result.ifPresent(novoSimboloInput -> {
            String novoSimbolo = novoSimboloInput.trim();

            // 1. Padroniza a entrada: qualquer entrada vazia vira o símbolo "ε"
            if (novoSimbolo.isEmpty()) {
                novoSimbolo = "ε";
            }

            // 2. Se o símbolo resultante for o mesmo que já existia, não faz nada.
            if (novoSimbolo.equals(transicao.getSimbolo())) {
                return; // Sai do método pois não houve mudança.
            }

            // Se houve mudança, executa a atualização correta:
            String oldTransitionStr = transicao.toString();

            // 3. (Correção anterior) PRIMEIRO, remove a view antiga usando o hashCode antigo.
            Node oldView = transicaoViews.remove(transicao);
            if (oldView != null) {
                drawingPane.getChildren().remove(oldView);
            }

            // 4. AGORA, atualiza o modelo de dados.
            transicao.setSimbolo(novoSimbolo);

            // 5. E por fim, cria e adiciona a nova visualização.
            Node newView = criarVisualizacaoTransicao(transicao);
            transicaoViews.put(transicao, newView);
            drawingPane.getChildren().add(newView);
            newView.toBack();

            logMessage("Transição " + oldTransitionStr + " editada para: " + transicao);
        });
    }

    private void deletarTransicao(Transicao transicao) {
        Node view = transicaoViews.remove(transicao);
        if (view != null) {
            drawingPane.getChildren().remove(view);
        }
        transicoes.remove(transicao);
        logMessage("Transição deletada: " + transicao);
    }

    // --- LÓGICA DA SIMULAÇÃO ---

    @FXML
    private void simulateWord() {
        String fullInput = wordTextField.getText();
        clearHighlights();

        if (estados.isEmpty()) {
            showResult("Erro: Nenhum autômato para simular!", false);
            return;
        }

        String[] wordsToTest = fullInput.split(",");
        boolean isBatchProcessing = wordsToTest.length > 1;

        // Animação mestre que vai conter todas as outras em sequência
        SequentialTransition masterAnimation = new SequentialTransition();

        logMessage(isBatchProcessing ? "\n=== INICIANDO SIMULAÇÃO EM LOTE ===" : "\n=== SIMULANDO PALAVRA ===");

        int acceptedCount = 0;
        for (String word : wordsToTest) {
            String currentWord = word.trim();
            String wordForLog = currentWord.isEmpty() ? "ε" : currentWord;

            logMessage("\n--- Testando '" + wordForLog + "' ---");

            AutomatonSimulator.SimulationResult result = AutomatonSimulator.simulate(estados, transicoes, currentWord);
            logMessage("Resultado: " + result.getMessage());

            if (result.isAccepted()) {
                acceptedCount++;
            }

            // Se um caminho foi encontrado, cria e adiciona sua animação à fila
            if (!result.getPath().isEmpty()) {
                // Log do caminho
                logMessage("Caminho percorrido:");
                StringBuilder pathLog = new StringBuilder("  " + result.getPath().get(0).getNome());
                List<String> symbolsUsed = result.getSymbolsUsed();
                for (int i = 1; i < result.getPath().size(); i++) {
                    String symbol = (i - 1 < symbolsUsed.size()) ? symbolsUsed.get(i - 1) : "?";
                    pathLog.append(" --(").append(symbol).append(")--> ").append(result.getPath().get(i).getNome());
                }
                logMessage(pathLog.toString());

                // Cria a animação para esta palavra
                SequentialTransition pathAnimation = createPathAnimation(result.getPath(), result.isAccepted());
                masterAnimation.getChildren().add(pathAnimation);

                // Adiciona uma pausa maior entre as palavras para limpar a tela e dar um respiro
                PauseTransition separatorPause = new PauseTransition(Duration.seconds(1.5));
                separatorPause.setOnFinished(e -> clearHighlights());
                masterAnimation.getChildren().add(separatorPause);
            }
        }

        // Atualiza o label de resultado com um resumo
        if (isBatchProcessing) {
            String summary = String.format("Lote concluído: %d de %d palavras aceitas.", acceptedCount, wordsToTest.length);
            showResult(summary, acceptedCount > 0);
            logMessage("\n=== FIM DA SIMULAÇÃO EM LOTE ===");
        } else if (wordsToTest.length == 1) {
            // Se for apenas uma palavra, mostra o resultado dela
            AutomatonSimulator.SimulationResult singleResult = AutomatonSimulator.simulate(estados, transicoes, wordsToTest[0].trim());
            showResult(singleResult.getMessage(), singleResult.isAccepted());
        }

        // Inicia a execução de TODAS as animações em sequência
        masterAnimation.play();
    }

    private SequentialTransition createPathAnimation(List<Estado> path, boolean accepted) {
        SequentialTransition animation = new SequentialTransition();

        for (int i = 0; i < path.size(); i++) {
            final int index = i;
            Estado estado = path.get(i);
            Group stateView = estadoViews.get(estado);

            if (stateView != null) {
                // Pausa entre a coloração de cada estado no caminho
                PauseTransition pause = new PauseTransition(Duration.seconds(0.7));
                pause.setOnFinished(e -> {
                    // Remove o highlight do estado anterior (se não for o primeiro)
                    if (index > 0) {
                        highlightState(estadoViews.get(path.get(index - 1)), false, false);
                    }
                    // Adiciona highlight no estado atual
                    boolean isFinalStep = (index == path.size() - 1);
                    highlightState(stateView, true, isFinalStep && accepted);
                });
                animation.getChildren().add(pause);
            }
        }
        return animation; // Retorna a animação em vez de dar .play()
    }

    private void highlightState(Group stateView, boolean highlight, boolean isFinalAndAccepted) {
        if (stateView == null) return;
        Circle circle = (Circle) stateView.lookup("#main-circle");

        if (highlight) {
            if (isFinalAndAccepted) {
                circle.setStroke(Color.LIMEGREEN);
            } else {
                circle.setStroke(Color.ORANGE);
            }
            circle.setStrokeWidth(4);
            highlightedStates.add(stateView);
        } else {
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(2);
            highlightedStates.remove(stateView);
        }
    }

    private void clearHighlights() {
        estadoViews.values().forEach(group -> {
            Circle circle = (Circle) group.lookup("#main-circle");
            if (circle != null) {
                circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(2);
            }
        });
        highlightedStates.clear();
    }

    // --- MÉTODOS UTILITÁRIOS ---

    private Group findNearestStateGroup(double x, double y) {
        return estadoViews.values().stream()
                .filter(group -> {
                    Estado estado = (Estado) group.getUserData();
                    double distance = Math.sqrt(Math.pow(x - estado.getX(), 2) + Math.pow(y - estado.getY(), 2));
                    return distance <= HIT_RADIUS;
                })
                .findFirst()
                .orElse(null);
    }

    private Optional<String> showTransitionSymbolDialog(String title, String header, String initialValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        VBox vbox = new VBox(10);
        Label label = new Label("Símbolo(s) - use vírgula para múltiplos (ex: a,b,c)");
        TextField textField = new TextField(initialValue);
        textField.setPromptText("Digite os símbolos...");

        Button epsilonButton = new Button("Adicionar ε (Epsilon)");
        epsilonButton.setOnAction(e -> {
            String currentText = textField.getText().trim();
            if (currentText.isEmpty()) {
                textField.setText("ε");
            } else if (!List.of(currentText.split(",")).contains("ε")) {
                textField.setText(currentText + ",ε");
            }
        });

        vbox.getChildren().addAll(label, textField, epsilonButton);
        dialog.getDialogPane().setContent(vbox);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return textField.getText();
            }
            return null;
        });

        Platform.runLater(() -> {
            textField.requestFocus();
            textField.selectAll();
        });

        return dialog.showAndWait();
    }

    private void showResult(String message, boolean accepted) {
        resultLabel.setText(message);
        resultLabel.setTextFill(accepted ? Color.GREEN : Color.RED);
        resultLabel.setStyle("-fx-font-weight: bold;");
    }

    @FXML
    private void clearAll() {
        drawingPane.getChildren().clear();
        estados.clear();
        transicoes.clear();
        estadoViews.clear();
        transicaoViews.clear();
        highlightedStates.clear();
        Estado.resetContador();

        wordTextField.clear();
        resultLabel.setText("");
        logTextArea.clear();

        logMessage("Tudo limpo! Simulador reiniciado.");
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logTextArea.appendText(message + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}