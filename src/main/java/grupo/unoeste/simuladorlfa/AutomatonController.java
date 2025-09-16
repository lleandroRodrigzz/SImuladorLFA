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

import java.util.*;

public class AutomatonController {

    @FXML private Pane drawingPane;
    @FXML private TextField wordTextField;
    @FXML private Button simulateButton;
    @FXML private Button clearButton;
    @FXML private Label resultLabel;
    @FXML private TextArea logTextArea;
    @FXML private VBox controlPanel;

    // --- Modelo de Dados ---
    private List<Estado> estados = new ArrayList<>();
    private List<Transicao> transicoes = new ArrayList<>();

    // --- Mapeamento para a Visão ---
    private Map<Estado, Group> estadoViews = new HashMap<>();
    private Map<Transicao, Node> transicaoViews = new HashMap<>();

    // --- Lógica para criação de transições ---
    private Group estadoOrigemView = null;
    private Line linhaDeTransicaoTemporaria = null;

    // --- Para animação da simulação ---
    private List<Group> highlightedStates = new ArrayList<>();

    @FXML
    public void initialize() {
        setupDrawingPaneEvents();
        setupControlButtons();

        logTextArea.setEditable(false);
        logTextArea.setFont(Font.font("Consolas", 12));

        wordTextField.setOnAction(e -> simulateWord());

        logMessage("Simulador de Autômatos Finitos iniciado!");
        logMessage("- Clique no painel para criar estados");
        logMessage("- Arraste de um estado para outro para criar transições");
        logMessage("- Clique direito nos estados para opções");
    }

    private void setupDrawingPaneEvents() {
        drawingPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == drawingPane) {
                criarNovoEstado(event.getX(), event.getY());
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
                Node targetNode = event.getPickResult().getIntersectedNode();
                Group estadoDestinoView = findStateGroup(targetNode);

                if (estadoDestinoView != null && estadoOrigemView != estadoDestinoView) {
                    Estado origem = (Estado) estadoOrigemView.getUserData();
                    Estado destino = (Estado) estadoDestinoView.getUserData();
                    criarNovaTransicao(origem, destino);
                }

                // Limpar estado de arraste
                if (linhaDeTransicaoTemporaria != null) {
                    drawingPane.getChildren().remove(linhaDeTransicaoTemporaria);
                    linhaDeTransicaoTemporaria = null;
                }
                estadoOrigemView = null;
            }
        });
    }

    private void setupControlButtons() {
        simulateButton.setOnAction(e -> simulateWord());
        clearButton.setOnAction(e -> clearAll());
    }

    private Group findStateGroup(Node node) {
        while (node != null) {
            if (node instanceof Group && node.getUserData() instanceof Estado) {
                return (Group) node;
            }
            node = node.getParent();
        }
        return null;
    }

    private void criarNovoEstado(double x, double y) {
        Estado novoEstado = new Estado(x, y);
        estados.add(novoEstado);

        Group estadoView = criarVisualizacaoEstado(novoEstado);
        estadoViews.put(novoEstado, estadoView);
        drawingPane.getChildren().add(estadoView);

        logMessage("Estado " + novoEstado.getNome() + " criado");
    }

    private Group criarVisualizacaoEstado(Estado estado) {
        Group group = new Group();
        group.setUserData(estado);
        group.setLayoutX(estado.getX());
        group.setLayoutY(estado.getY());

        // Círculo principal
        Circle circle = new Circle(0, 0, 25, Color.LIGHTBLUE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2);

        // Círculo interno para estado final
        Circle innerCircle = new Circle(0, 0, 20, Color.TRANSPARENT);
        innerCircle.setStroke(Color.BLACK);
        innerCircle.setStrokeWidth(1);
        innerCircle.setVisible(false);

        // Seta para estado inicial
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[]{
                -25.0,  0.0,  // A ponta da seta, tocando a borda do círculo
                -35.0, -5.0,  // Ponto superior da base da seta
                -35.0,  5.0   // Ponto inferior da base da seta
        });
        arrow.setFill(Color.BLACK);
        arrow.setVisible(false);

        // Texto do nome
        Text text = new Text(estado.getNome());
        text.setBoundsType(TextBoundsType.VISUAL);
        text.setX(-text.getLayoutBounds().getWidth() / 2);
        text.setY(text.getLayoutBounds().getHeight() / 2);
        text.setFont(Font.font(14));

        group.getChildren().addAll(circle, innerCircle, arrow, text);

        // Atualizar visualização quando propriedades mudarem
        estado.isInicialProperty().addListener((obs, oldVal, newVal) -> {
            arrow.setVisible(newVal);
            if (newVal) {
                // Garantir que apenas um estado seja inicial
                for (Estado e : estados) {
                    if (e != estado && e.isInicial()) {
                        e.setInicial(false);
                    }
                }
                logMessage("Estado " + estado.getNome() + " marcado como inicial");
            }
        });

        estado.isFinalProperty().addListener((obs, oldVal, newVal) -> {
            innerCircle.setVisible(newVal);
            if (newVal) {
                logMessage("Estado " + estado.getNome() + " marcado como final");
            }
        });

        setupStateEvents(group, estado, circle);

        return group;
    }

    private void setupStateEvents(Group group, Estado estado, Circle circle) {
        // Menu de contexto
        ContextMenu contextMenu = new ContextMenu();

        // CORREÇÃO: Removido o binding problemático e criamos os MenuItems sem binding
        MenuItem marcarInicialItem = new MenuItem();
        MenuItem marcarFinalItem = new MenuItem();
        MenuItem renomearItem = new MenuItem("Renomear");
        MenuItem deletarItem = new MenuItem("Deletar");

        // Configurar as ações
        marcarInicialItem.setOnAction(e -> estado.setInicial(!estado.isInicial()));
        marcarFinalItem.setOnAction(e -> estado.setFinal(!estado.isFinal()));
        renomearItem.setOnAction(e -> renomearEstado(estado));
        deletarItem.setOnAction(e -> deletarEstado(group));

        contextMenu.getItems().addAll(marcarInicialItem, marcarFinalItem,
                new SeparatorMenuItem(), renomearItem, deletarItem);

        group.setOnContextMenuRequested(event -> {
            // CORREÇÃO: Agora definimos o texto dos MenuItems diretamente, sem conflito com binding
            marcarInicialItem.setText(estado.isInicial() ? "✓ Inicial" : "Marcar como Inicial");
            marcarFinalItem.setText(estado.isFinal() ? "✓ Final" : "Marcar como Final");
            contextMenu.show(group, event.getScreenX(), event.getScreenY());
        });

        // Iniciar criação de transição
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

        // Highlight on hover
        group.setOnMouseEntered(e -> circle.setStroke(Color.BLUE));
        group.setOnMouseExited(e -> {
            if (!highlightedStates.contains(group)) {
                circle.setStroke(Color.BLACK);
            }
        });
    }

    private void renomearEstado(Estado estado) {
        TextInputDialog dialog = new TextInputDialog(estado.getNome());
        dialog.setTitle("Renomear Estado");
        dialog.setHeaderText("Digite o novo nome para o estado:");
        dialog.setContentText("Nome:");

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
            Text text = (Text) group.getChildren().get(3); // O texto é o 4º elemento
            text.setText(estado.getNome());
            text.setX(-text.getLayoutBounds().getWidth() / 2);
        }
    }

    private void criarNovaTransicao(Estado origem, Estado destino) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Símbolo da Transição");
        dialog.setHeaderText("Digite o símbolo para a transição de " +
                origem.getNome() + " para " + destino.getNome() + ":");
        dialog.setContentText("Símbolo (use ε para epsilon):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(simbolo -> {
            simbolo = simbolo.trim();
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
        });
    }

    private Node criarVisualizacaoTransicao(Transicao transicao) {
        Estado origem = transicao.getOrigem();
        Estado destino = transicao.getDestino();

        Group group = new Group();

        if (origem == destino) {
            // Auto-transição (loop)
            Circle loop = new Circle(origem.getX(), origem.getY() - 40, 20);
            loop.setFill(Color.TRANSPARENT);
            loop.setStroke(Color.BLACK);
            loop.setStrokeWidth(1);

            // Seta
            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(new Double[]{
                    origem.getX() + 15, origem.getY() - 25,
                    origem.getX() + 20, origem.getY() - 30,
                    origem.getX() + 25, origem.getY() - 25,
                    origem.getX() + 20, origem.getY() - 20
            });
            arrow.setFill(Color.BLACK);

            Text texto = new Text(origem.getX() - 5, origem.getY() - 55, transicao.getSimbolo());
            texto.setFont(Font.font(12));

            group.getChildren().addAll(loop, arrow, texto);
        } else {
            // Transição normal
            double startX = origem.getX();
            double startY = origem.getY();
            double endX = destino.getX();
            double endY = destino.getY();

            // Calcular posições ajustadas para os círculos
            double angle = Math.atan2(endY - startY, endX - startX);
            double radius = 25;

            double adjustedStartX = startX + radius * Math.cos(angle);
            double adjustedStartY = startY + radius * Math.sin(angle);
            double adjustedEndX = endX - radius * Math.cos(angle);
            double adjustedEndY = endY - radius * Math.sin(angle);

            Line linha = new Line(adjustedStartX, adjustedStartY, adjustedEndX, adjustedEndY);
            linha.setStrokeWidth(1.5);

            // Seta
            double arrowLength = 10;
            double arrowAngle = Math.PI / 6;

            double arrowX1 = adjustedEndX - arrowLength * Math.cos(angle - arrowAngle);
            double arrowY1 = adjustedEndY - arrowLength * Math.sin(angle - arrowAngle);
            double arrowX2 = adjustedEndX - arrowLength * Math.cos(angle + arrowAngle);
            double arrowY2 = adjustedEndY - arrowLength * Math.sin(angle + arrowAngle);

            Polygon arrow = new Polygon();
            arrow.getPoints().addAll(new Double[]{
                    adjustedEndX, adjustedEndY,
                    arrowX1, arrowY1,
                    arrowX2, arrowY2
            });
            arrow.setFill(Color.BLACK);

            // Texto no meio da linha
            Text texto = new Text(transicao.getSimbolo());
            texto.setFont(Font.font(12));
            double textX = (adjustedStartX + adjustedEndX) / 2;
            double textY = (adjustedStartY + adjustedEndY) / 2 - 5;
            texto.setX(textX - texto.getLayoutBounds().getWidth() / 2);
            texto.setY(textY);

            group.getChildren().addAll(linha, arrow, texto);
        }

        // Menu de contexto para deletar transição
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deletarItem = new MenuItem("Deletar Transição");
        deletarItem.setOnAction(e -> deletarTransicao(transicao));
        contextMenu.getItems().add(deletarItem);

        group.setOnContextMenuRequested(event ->
                contextMenu.show(group, event.getScreenX(), event.getScreenY()));

        return group;
    }

    private void deletarTransicao(Transicao transicao) {
        Node view = transicaoViews.get(transicao);
        if (view != null) {
            drawingPane.getChildren().remove(view);
            transicaoViews.remove(transicao);
        }
        transicoes.remove(transicao);
        logMessage("Transição deletada: " + transicao);
    }

    private void deletarEstado(Group estadoView) {
        Estado estadoParaDeletar = (Estado) estadoView.getUserData();

        // Remove visualmente o estado
        drawingPane.getChildren().remove(estadoView);
        estadoViews.remove(estadoParaDeletar);

        // Remove do modelo
        estados.remove(estadoParaDeletar);

        // Remove transições conectadas
        List<Transicao> transicoesParaRemover = new ArrayList<>();
        for (Transicao t : transicoes) {
            if (t.getOrigem() == estadoParaDeletar || t.getDestino() == estadoParaDeletar) {
                transicoesParaRemover.add(t);
            }
        }

        for (Transicao t : transicoesParaRemover) {
            Node transicaoView = transicaoViews.get(t);
            if (transicaoView != null) {
                drawingPane.getChildren().remove(transicaoView);
                transicaoViews.remove(t);
            }
            transicoes.remove(t);
        }

        logMessage("Estado " + estadoParaDeletar.getNome() + " e suas transições foram deletados");
    }

    @FXML
    private void simulateWord() {
        String word = wordTextField.getText().trim();

        clearHighlights();

        if (estados.isEmpty()) {
            showResult("Erro: Adicione pelo menos um estado!", false);
            return;
        }

        logMessage("\n=== SIMULANDO PALAVRA: '" + word + "' ===");

        AutomatonSimulator.SimulationResult result =
                AutomatonSimulator.simulate(estados, transicoes, word);

        showResult(result.getMessage(), result.isAccepted());

        // Mostrar caminho percorrido
        if (!result.getPath().isEmpty()) {
            logMessage("Caminho percorrido:");
            for (int i = 0; i < result.getPath().size(); i++) {
                Estado estado = result.getPath().get(i);
                if (i == 0) {
                    logMessage("  Início: " + estado.getNome());
                } else {
                    String simbolo = i <= word.length() ?
                            String.valueOf(word.charAt(i-1)) : "";
                    logMessage("  --(" + simbolo + ")--> " + estado.getNome());
                }
            }

            // Animar caminho
            animateSimulation(result.getPath(), word);
        }

        logMessage("Resultado: " + (result.isAccepted() ? "ACEITA" : "REJEITADA"));
    }

    private void animateSimulation(List<Estado> path, String word) {
        SequentialTransition animation = new SequentialTransition();

        for (int i = 0; i < path.size(); i++) {
            Estado estado = path.get(i);
            Group stateView = estadoViews.get(estado);

            if (stateView != null) {
                final int index = i;
                PauseTransition pause = new PauseTransition(Duration.seconds(0.8));
                pause.setOnFinished(e -> {
                    highlightState(stateView, true);
                    if (index > 0) {
                        // Remove highlight do estado anterior
                        Estado prevState = path.get(index - 1);
                        Group prevView = estadoViews.get(prevState);
                        if (prevView != null) {
                            highlightState(prevView, false);
                        }
                    }
                });
                animation.getChildren().add(pause);
            }
        }

        // Pause final para mostrar o resultado
        PauseTransition finalPause = new PauseTransition(Duration.seconds(1.5));
        finalPause.setOnFinished(e -> {
            // Manter highlight no último estado
            if (!path.isEmpty()) {
                Estado lastState = path.get(path.size() - 1);
                Group lastView = estadoViews.get(lastState);
                if (lastView != null) {
                    Circle circle = (Circle) lastView.getChildren().get(0);
                    circle.setStroke(lastState.isFinal() ? Color.GREEN : Color.RED);
                    circle.setStrokeWidth(3);
                }
            }
        });
        animation.getChildren().add(finalPause);

        animation.play();
    }

    private void highlightState(Group stateView, boolean highlight) {
        Circle circle = (Circle) stateView.getChildren().get(0);
        if (highlight) {
            circle.setStroke(Color.ORANGE);
            circle.setStrokeWidth(4);
            highlightedStates.add(stateView);
        } else {
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(2);
            highlightedStates.remove(stateView);
        }
    }

    private void clearHighlights() {
        for (Group stateView : highlightedStates) {
            Circle circle = (Circle) stateView.getChildren().get(0);
            circle.setStroke(Color.BLACK);
            circle.setStrokeWidth(2);
        }
        highlightedStates.clear();
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
        logMessage("- Clique no painel para criar estados");
        logMessage("- Arraste de um estado para outro para criar transições");
        logMessage("- Clique direito nos estados para opções");
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logTextArea.appendText(message + "\n");
            logTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}