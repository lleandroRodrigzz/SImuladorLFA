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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.util.Duration;

import java.util.*;

public class AutomatonController {

    @FXML private Pane drawingPane;
    @FXML private TextField wordTextField;
    @FXML private Button simulateButton;
    @FXML private Button stepByStepButton;
    @FXML private Button clearButton;
    @FXML private Button nextStepButton;
    @FXML private Button prevStepButton;
    @FXML private Button resetStepButton;
    @FXML private Label resultLabel;
    @FXML private Label stepInfoLabel;
    @FXML private TextArea logTextArea;
    @FXML private VBox controlPanel;
    @FXML private HBox stepControlPanel;
    @FXML private CheckBox showEpsilonTransitionsCheckBox;
    @FXML private Label automatonTypeLabel;

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

    // --- Para simulação passo a passo ---
    private boolean stepByStepMode = false;
    private String currentWord = "";
    private int currentStep = 0;
    private List<Estado> currentPath = new ArrayList<>();
    private List<String> currentSymbolsUsed = new ArrayList<>();
    private AutomatonSimulator.SimulationResult lastResult = null;

    // --- Constantes para melhorar a usabilidade ---
    private static final double STATE_RADIUS = 25.0;
    private static final double HIT_RADIUS = 35.0;

    @FXML
    public void initialize() {
        setupDrawingPaneEvents();
        setupControlButtons();

        logTextArea.setEditable(false);
        logTextArea.setFont(Font.font("Consolas", 12));
        wordTextField.setOnAction(e -> simulateWord());

        // Inicializar controles de passo a passo
        stepControlPanel.setVisible(false);
        stepInfoLabel.setText("");

        // Configurar checkbox para mostrar transições epsilon
        showEpsilonTransitionsCheckBox.setSelected(true);
        showEpsilonTransitionsCheckBox.setOnAction(e -> updateAutomatonVisualization());

        logMessage("Simulador de Autômatos Finitos iniciado!");
        logMessage("- Clique no painel para criar estados.");
        logMessage("- Arraste de um estado para outro para criar transições.");
        logMessage("- Clique direito nos elementos para mais opções.");
        logMessage("- Use vírgulas para múltiplos símbolos: 'a,b,c'");
        logMessage("- Use 'Passo a Passo' para ver a simulação detalhadamente.");

        updateAutomatonTypeDisplay();
    }

    private void setupDrawingPaneEvents() {
        drawingPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == drawingPane) {
                criarNovoEstado(event.getX(), event.getY());
                updateAutomatonTypeDisplay();
            }
        });

        drawingPane.setOnMouseDragged(event -> {
            if (linhaDeTransicaoTemporaria != null) {
                linhaDeTransicaoTemporaria.setEndX(event.getX());
                linhaDeTransicaoTemporaria.setEndY(event.getY());
            }
        });

        drawingPane.setOnMouseReleased(event -> {
            if (estadoOrigemView != null) {
                Group estadoDestinoView = findNearestStateGroup(event.getX(), event.getY());

                if (estadoDestinoView != null) {
                    Estado origem = (Estado) estadoOrigemView.getUserData();
                    Estado destino = (Estado) estadoDestinoView.getUserData();
                    criarNovaTransicao(origem, destino);
                    updateAutomatonTypeDisplay();
                }

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
        stepByStepButton.setOnAction(e -> startStepByStepSimulation());
        clearButton.setOnAction(e -> clearAll());
        nextStepButton.setOnAction(e -> nextStep());
        prevStepButton.setOnAction(e -> prevStep());
        resetStepButton.setOnAction(e -> resetStepByStep());
    }

    // --- SIMULAÇÃO PASSO A PASSO ---

    @FXML
    private void startStepByStepSimulation() {
        String word = wordTextField.getText().trim();

        if (word.contains(",")) {
            showAlert("Aviso", "Simulação passo a passo funciona apenas com uma palavra por vez.\nPrimeira palavra será usada: '" + word.split(",")[0].trim() + "'");
            word = word.split(",")[0].trim();
        }

        clearHighlights();

        if (estados.isEmpty()) {
            showResult("Erro: Nenhum autômato para simular!", false);
            return;
        }

        currentWord = word;
        currentStep = 0;
        stepByStepMode = true;

        // Executar simulação completa para obter o caminho
        lastResult = AutomatonSimulator.simulate(estados, transicoes, currentWord);
        currentPath = lastResult.getPath();
        currentSymbolsUsed = lastResult.getSymbolsUsed();

        // Mostrar controles de passo a passo
        stepControlPanel.setVisible(true);
        simulateButton.setDisable(true);
        stepByStepButton.setDisable(true);

        logMessage("\n=== INICIANDO SIMULAÇÃO PASSO A PASSO ===");
        logMessage("Palavra: '" + (currentWord.isEmpty() ? "ε" : currentWord) + "'");
        logMessage("Caminho encontrado: " + currentPath.size() + " estados");

        // Mostrar primeiro passo
        updateStepDisplay();
        updateStepControls();
    }

    @FXML
    private void nextStep() {
        if (currentStep < currentPath.size() - 1) {
            currentStep++;
            updateStepDisplay();
            updateStepControls();
        }
    }

    @FXML
    private void prevStep() {
        if (currentStep > 0) {
            currentStep--;
            updateStepDisplay();
            updateStepControls();
        }
    }

    @FXML
    private void resetStepByStep() {
        stepByStepMode = false;
        currentStep = 0;
        clearHighlights();

        stepControlPanel.setVisible(false);
        simulateButton.setDisable(false);
        stepByStepButton.setDisable(false);
        stepInfoLabel.setText("");
        resultLabel.setText("");

        logMessage("=== SIMULAÇÃO PASSO A PASSO FINALIZADA ===\n");
    }

    private void updateStepDisplay() {
        clearHighlights();

        if (currentPath.isEmpty() || currentStep >= currentPath.size()) {
            return;
        }

        Estado currentState = currentPath.get(currentStep);
        Group stateView = estadoViews.get(currentState);

        if (stateView != null) {
            boolean isLastStep = (currentStep == currentPath.size() - 1);
            boolean isAccepted = isLastStep && lastResult.isAccepted();
            highlightState(stateView, true, isAccepted);
        }

        // Atualizar informações do passo
        StringBuilder stepInfo = new StringBuilder();
        stepInfo.append("Passo ").append(currentStep + 1).append(" de ").append(currentPath.size());
        stepInfo.append(" | Estado: ").append(currentState.getNome());

        if (currentStep == 0) {
            stepInfo.append(" (inicial)");
        } else {
            String symbolUsed = currentStep - 1 < currentSymbolsUsed.size() ?
                    currentSymbolsUsed.get(currentStep - 1) : "?";
            stepInfo.append(" | Símbolo consumido: '").append(symbolUsed).append("'");
        }

        if (currentStep == currentPath.size() - 1) {
            stepInfo.append(" | Estado ").append(currentState.isFinal() ? "FINAL" : "NÃO-FINAL");
        }

        stepInfoLabel.setText(stepInfo.toString());

        // Mostrar progresso da palavra
        StringBuilder wordProgress = new StringBuilder();
        if (currentWord.isEmpty()) {
            wordProgress.append("Palavra: ε");
        } else {
            wordProgress.append("Palavra: ");
            for (int i = 0; i < currentWord.length(); i++) {
                if (i < currentStep && currentStep > 0 && i < currentSymbolsUsed.size()) {
                    wordProgress.append("[").append(currentWord.charAt(i)).append("]");
                } else if (i == currentStep - 1 && currentStep > 0) {
                    wordProgress.append("→").append(currentWord.charAt(i)).append("←");
                } else {
                    wordProgress.append(currentWord.charAt(i));
                }
            }
        }

        logMessage("Passo " + (currentStep + 1) + ": " + stepInfo.toString());
        if (currentStep == currentPath.size() - 1) {
            showResult(lastResult.getMessage(), lastResult.isAccepted());
        }
    }

    private void updateStepControls() {
        nextStepButton.setDisable(currentStep >= currentPath.size() - 1);
        prevStepButton.setDisable(currentStep <= 0);
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

        Circle hitArea = new Circle(0, 0, HIT_RADIUS, Color.TRANSPARENT);
        hitArea.setId("hit-area");

        Circle circle = new Circle(0, 0, STATE_RADIUS, Color.LIGHTBLUE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2);
        circle.setId("main-circle");

        Circle innerCircle = new Circle(0, 0, STATE_RADIUS - 5, Color.TRANSPARENT);
        innerCircle.setStroke(Color.BLACK);
        innerCircle.setStrokeWidth(1.5);
        innerCircle.setVisible(false);
        innerCircle.setId("inner-circle");

        Polygon arrow = new Polygon(-STATE_RADIUS - 12, -7.0, -STATE_RADIUS, 0.0, -STATE_RADIUS - 12, 7.0);
        arrow.setFill(Color.BLACK);
        arrow.setVisible(false);
        arrow.setId("initial-arrow");

        Text text = new Text(estado.getNome());
        text.setBoundsType(TextBoundsType.VISUAL);
        text.setFont(Font.font(14));
        text.setX(-text.getLayoutBounds().getWidth() / 2);
        text.setY(text.getLayoutBounds().getHeight() / 4);
        text.setId("state-name-text");

        group.getChildren().addAll(hitArea, circle, innerCircle, arrow, text);

        estado.isInicialProperty().addListener((obs, oldVal, newVal) -> {
            arrow.setVisible(newVal);
            if (newVal) {
                estados.stream()
                        .filter(e -> e != estado && e.isInicial())
                        .forEach(e -> e.setInicial(false));
                logMessage("Estado " + estado.getNome() + " marcado como inicial.");
                updateAutomatonTypeDisplay();
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

        List<Transicao> transicoesParaRemover = new ArrayList<>();
        transicoes.forEach(t -> {
            if (t.getOrigem() == estadoParaDeletar || t.getDestino() == estadoParaDeletar) {
                transicoesParaRemover.add(t);
            }
        });
        transicoesParaRemover.forEach(this::deletarTransicao);

        drawingPane.getChildren().remove(estadoView);
        estadoViews.remove(estadoParaDeletar);
        estados.remove(estadoParaDeletar);

        logMessage("Estado " + estadoParaDeletar.getNome() + " e suas transições foram deletados.");
        updateAutomatonTypeDisplay();
    }

    // --- MÉTODOS DE CRIAÇÃO E MANIPULAÇÃO DE TRANSIÇÕES ---

    private void criarNovaTransicao(Estado origem, Estado destino) {
        String header = "Digite o símbolo(s) para a transição de " +
                origem.getNome() + " para " + destino.getNome() + ":";

        Optional<String> result = showTransitionSymbolDialog("Símbolo da Transição", header, "");

        result.ifPresent(simboloInput -> {
            String simbolo = simboloInput.trim();
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
            Circle loop = new Circle(origem.getX(), origem.getY() - (STATE_RADIUS + 15), 15);
            loop.setFill(Color.TRANSPARENT);
            loop.setStroke(Color.BLACK);
            loop.setStrokeWidth(1.5);

            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(
                    origem.getX() + 10, origem.getY() - (STATE_RADIUS + 15) - 15,
                    origem.getX() + 15, origem.getY() - (STATE_RADIUS + 15) - 5,
                    origem.getX() + 5, origem.getY() - (STATE_RADIUS + 15) - 5
            );
            arrow.setFill(Color.BLACK);

            Text texto = new Text(origem.getX() - 10, origem.getY() - (STATE_RADIUS + 35), transicao.getSimbolo());
            texto.setFont(Font.font(12));

            group.getChildren().addAll(loop, arrow, texto);
        } else {
            double angle = Math.atan2(destino.getY() - origem.getY(), destino.getX() - origem.getX());

            double startX = origem.getX() + STATE_RADIUS * Math.cos(angle);
            double startY = origem.getY() + STATE_RADIUS * Math.sin(angle);
            double endX = destino.getX() - STATE_RADIUS * Math.cos(angle);
            double endY = destino.getY() - STATE_RADIUS * Math.sin(angle);

            Line linha = new Line(startX, startY, endX, endY);
            linha.setStrokeWidth(2.0);

            double arrowLength = 12;
            double arrowAngle = Math.toRadians(25);
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(
                    endX, endY,
                    endX - arrowLength * Math.cos(angle - arrowAngle), endY - arrowLength * Math.sin(angle - arrowAngle),
                    endX - arrowLength * Math.cos(angle + arrowAngle), endY - arrowLength * Math.sin(angle + arrowAngle)
            );
            arrow.setFill(Color.BLACK);

            Text texto = new Text(transicao.getSimbolo());
            texto.setFont(Font.font("Arial", 12));
            double textX = (startX + endX) / 2;
            double textY = (startY + endY) / 2 - 8;
            texto.setX(textX - texto.getLayoutBounds().getWidth() / 2);
            texto.setY(textY);

            group.getChildren().addAll(linha, arrow, texto);
        }

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

    private void editarTransicao(Transicao transicao) {
        String header = "Edite o símbolo(s) da transição de " +
                transicao.getOrigem().getNome() + " para " + transicao.getDestino().getNome() + ":";

        Optional<String> result = showTransitionSymbolDialog("Editar Transição", header, transicao.getSimbolo());

        result.ifPresent(novoSimboloInput -> {
            String novoSimbolo = novoSimboloInput.trim();

            if (novoSimbolo.isEmpty()) {
                novoSimbolo = "ε";
            }

            if (novoSimbolo.equals(transicao.getSimbolo())) {
                return;
            }

            String oldTransitionStr = transicao.toString();

            Node oldView = transicaoViews.remove(transicao);
            if (oldView != null) {
                drawingPane.getChildren().remove(oldView);
            }

            transicao.setSimbolo(novoSimbolo);

            Node newView = criarVisualizacaoTransicao(transicao);
            transicaoViews.put(transicao, newView);
            drawingPane.getChildren().add(newView);
            newView.toBack();

            logMessage("Transição " + oldTransitionStr + " editada para: " + transicao);
            updateAutomatonTypeDisplay();
        });
    }

    private void deletarTransicao(Transicao transicao) {
        Node view = transicaoViews.remove(transicao);
        if (view != null) {
            drawingPane.getChildren().remove(view);
        }
        transicoes.remove(transicao);
        logMessage("Transição deletada: " + transicao);
        updateAutomatonTypeDisplay();
    }

    // --- SIMULAÇÃO NORMAL ---

    @FXML
    private void simulateWord() {
        if (stepByStepMode) {
            return; // Não permitir simulação normal durante passo a passo
        }

        String fullInput = wordTextField.getText();
        clearHighlights();

        if (estados.isEmpty()) {
            showResult("Erro: Nenhum autômato para simular!", false);
            return;
        }

        String[] wordsToTest = fullInput.split(",");
        boolean isBatchProcessing = wordsToTest.length > 1;

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

            if (!result.getPath().isEmpty()) {
                logMessage("Caminho percorrido:");
                StringBuilder pathLog = new StringBuilder("  " + result.getPath().get(0).getNome());
                List<String> symbolsUsed = result.getSymbolsUsed();
                for (int i = 1; i < result.getPath().size(); i++) {
                    String symbol = (i - 1 < symbolsUsed.size()) ? symbolsUsed.get(i - 1) : "?";
                    pathLog.append(" --(").append(symbol).append(")--> ").append(result.getPath().get(i).getNome());
                }
                logMessage(pathLog.toString());

                SequentialTransition pathAnimation = createPathAnimation(result.getPath(), result.isAccepted());
                masterAnimation.getChildren().add(pathAnimation);

                PauseTransition separatorPause = new PauseTransition(Duration.seconds(1.5));
                separatorPause.setOnFinished(e -> clearHighlights());
                masterAnimation.getChildren().add(separatorPause);
            }
        }

        if (isBatchProcessing) {
            String summary = String.format("Lote concluído: %d de %d palavras aceitas.", acceptedCount, wordsToTest.length);
            showResult(summary, acceptedCount > 0);
            logMessage("\n=== FIM DA SIMULAÇÃO EM LOTE ===");
        } else if (wordsToTest.length == 1) {
            AutomatonSimulator.SimulationResult singleResult = AutomatonSimulator.simulate(estados, transicoes, wordsToTest[0].trim());
            showResult(singleResult.getMessage(), singleResult.isAccepted());
        }

        masterAnimation.play();
    }

    private SequentialTransition createPathAnimation(List<Estado> path, boolean accepted) {
        SequentialTransition animation = new SequentialTransition();

        for (int i = 0; i < path.size(); i++) {
            final int index = i;
            Estado estado = path.get(i);
            Group stateView = estadoViews.get(estado);

            if (stateView != null) {
                PauseTransition pause = new PauseTransition(Duration.seconds(0.7));
                pause.setOnFinished(e -> {
                    if (index > 0) {
                        highlightState(estadoViews.get(path.get(index - 1)), false, false);
                    }
                    boolean isFinalStep = (index == path.size() - 1);
                    highlightState(stateView, true, isFinalStep && accepted);
                });
                animation.getChildren().add(pause);
            }
        }
        return animation;
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateAutomatonTypeDisplay() {
        if (estados.isEmpty()) {
            automatonTypeLabel.setText("Tipo: Nenhum autômato");
            return;
        }

        boolean isDeterministic = AutomatonSimulator.isDeterministicPublic(transicoes);
        boolean hasInitialState = estados.stream().anyMatch(Estado::isInicial);
        boolean hasFinalState = estados.stream().anyMatch(Estado::isFinal);

        StringBuilder typeInfo = new StringBuilder();
        typeInfo.append("Tipo: ").append(isDeterministic ? "AFD" : "AFND");

        if (!hasInitialState) {
            typeInfo.append(" [SEM ESTADO INICIAL]");
        }
        if (!hasFinalState) {
            typeInfo.append(" [SEM ESTADO FINAL]");
        }

        automatonTypeLabel.setText(typeInfo.toString());
        automatonTypeLabel.setTextFill(hasInitialState && hasFinalState ? Color.BLACK : Color.RED);
    }

    private void updateAutomatonVisualization() {
        // Atualizar visibilidade de transições epsilon se necessário
        boolean showEpsilon = showEpsilonTransitionsCheckBox.isSelected();

        transicaoViews.forEach((transicao, view) -> {
            if (transicao.isEpsilon() && !showEpsilon) {
                view.setVisible(false);
            } else {
                view.setVisible(true);
            }
        });
    }

    @FXML
    private void clearAll() {
        // Resetar modo passo a passo se estiver ativo
        if (stepByStepMode) {
            resetStepByStep();
        }

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

        updateAutomatonTypeDisplay();
        logMessage("Tudo limpo! Simulador reiniciado.");
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logTextArea.appendText(message + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // --- MÉTODOS PARA EXPORTAR/IMPORTAR AUTÔMATO ---

    @FXML
    private void exportAutomaton() {
        if (estados.isEmpty()) {
            showAlert("Aviso", "Nenhum autômato para exportar!");
            return;
        }

        StringBuilder export = new StringBuilder();
        export.append("=== AUTÔMATO EXPORTADO ===\n");
        export.append("Estados: ").append(estados.size()).append("\n");
        export.append("Transições: ").append(transicoes.size()).append("\n");
        export.append("Tipo: ").append(AutomatonSimulator.isDeterministicPublic(transicoes) ? "AFD" : "AFND").append("\n\n");

        export.append("ESTADOS:\n");
        for (Estado estado : estados) {
            export.append("- ").append(estado.getNome());
            if (estado.isInicial()) export.append(" (inicial)");
            if (estado.isFinal()) export.append(" (final)");
            export.append(" [").append(estado.getX()).append(",").append(estado.getY()).append("]\n");
        }

        export.append("\nTRANSIÇÕES:\n");
        for (Transicao transicao : transicoes) {
            export.append("- ").append(transicao.getOrigem().getNome())
                    .append(" --[").append(transicao.getSimbolo()).append("]--> ")
                    .append(transicao.getDestino().getNome()).append("\n");
        }

        logMessage("\n" + export.toString());
        logMessage("Autômato exportado para o log!");
    }

    @FXML
    private void showAutomatonInfo() {
        if (estados.isEmpty()) {
            showAlert("Informações do Autômato", "Nenhum autômato criado ainda!");
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append("=== INFORMAÇÕES DO AUTÔMATO ===\n\n");

        // Informações gerais
        info.append("Número de estados: ").append(estados.size()).append("\n");
        info.append("Número de transições: ").append(transicoes.size()).append("\n");

        boolean isDeterministic = AutomatonSimulator.isDeterministicPublic(transicoes);
        info.append("Tipo: ").append(isDeterministic ? "Autômato Finito Determinístico (AFD)" : "Autômato Finito Não-Determinístico (AFND)").append("\n");

        // Estados especiais
        long initialStates = estados.stream().filter(Estado::isInicial).count();
        long finalStates = estados.stream().filter(Estado::isFinal).count();

        info.append("Estados iniciais: ").append(initialStates).append("\n");
        info.append("Estados finais: ").append(finalStates).append("\n\n");

        // Alfabeto
        Set<String> alfabeto = new HashSet<>();
        for (Transicao t : transicoes) {
            for (String simbolo : t.getSimbolos()) {
                if (!simbolo.equals("ε")) {
                    alfabeto.add(simbolo);
                }
            }
        }
        info.append("Alfabeto: {").append(String.join(", ", alfabeto)).append("}\n");

        // Transições epsilon
        long epsilonTransitions = transicoes.stream().filter(Transicao::isEpsilon).count();
        if (epsilonTransitions > 0) {
            info.append("Transições ε: ").append(epsilonTransitions).append("\n");
        }

        // Verificações de completude
        info.append("\n=== ANÁLISE DE COMPLETUDE ===\n");
        if (initialStates == 0) {
            info.append("⚠ Aviso: Nenhum estado inicial definido\n");
        } else if (initialStates > 1) {
            info.append("⚠ Aviso: Múltiplos estados iniciais (característico de AFND)\n");
        }

        if (finalStates == 0) {
            info.append("⚠ Aviso: Nenhum estado final definido\n");
        }

        if (isDeterministic && alfabeto.size() > 0) {
            // Verificar se AFD é completo
            boolean isComplete = true;
            for (Estado estado : estados) {
                for (String simbolo : alfabeto) {
                    boolean hasTransition = transicoes.stream()
                            .anyMatch(t -> t.getOrigem().equals(estado) && t.aceitaSimbolo(simbolo));
                    if (!hasTransition) {
                        isComplete = false;
                        break;
                    }
                }
                if (!isComplete) break;
            }
            info.append(isComplete ? "✓ AFD completo" : "○ AFD incompleto").append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informações do Autômato");
        alert.setHeaderText("Análise do Autômato Atual");
        alert.setContentText(info.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }
}