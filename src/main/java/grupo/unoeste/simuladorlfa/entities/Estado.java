package grupo.unoeste.simuladorlfa.entities;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class Estado {
    private final SimpleStringProperty nome;
    private final SimpleDoubleProperty x; // Posição X na tela
    private final SimpleDoubleProperty y; // Posição Y na tela
    private final SimpleBooleanProperty isInicial;
    private final SimpleBooleanProperty isFinal;

    private static int contador = 0;

    public Estado(double x, double y) {
        this.nome = new SimpleStringProperty("q" + contador++);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
        this.isInicial = new SimpleBooleanProperty(false);
        this.isFinal = new SimpleBooleanProperty(false);
    }

    public Estado(String nome, double x, double y) {
        this.nome = new SimpleStringProperty(nome);
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
        this.isInicial = new SimpleBooleanProperty(false);
        this.isFinal = new SimpleBooleanProperty(false);
    }

    // --- Getters e Setters ---
    public String getNome() { return nome.get(); }
    public void setNome(String nome) { this.nome.set(nome); }
    public SimpleStringProperty nomeProperty() { return nome; }

    public double getX() { return x.get(); }
    public double getY() { return y.get(); }
    public void setX(double x) { this.x.set(x); }
    public void setY(double y) { this.y.set(y); }
    public SimpleDoubleProperty xProperty() { return x; }
    public SimpleDoubleProperty yProperty() { return y; }

    public boolean isInicial() { return isInicial.get(); }
    public void setInicial(boolean isInicial) { this.isInicial.set(isInicial); }
    public SimpleBooleanProperty isInicialProperty() { return isInicial; }

    public boolean isFinal() { return isFinal.get(); }
    public void setFinal(boolean isFinal) { this.isFinal.set(isFinal); }
    public SimpleBooleanProperty isFinalProperty() { return isFinal; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Estado estado = (Estado) obj;
        return nome.get().equals(estado.nome.get());
    }

    @Override
    public int hashCode() {
        return nome.get().hashCode();
    }

    @Override
    public String toString() {
        return nome.get();
    }

    public static void resetContador() {
        contador = 0;
    }
}