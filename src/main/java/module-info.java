module grupo.unoeste.simuladorlfa {
    requires javafx.controls;
    requires javafx.fxml;
    requires generex;

    opens grupo.unoeste.simuladorlfa to javafx.fxml;
    exports grupo.unoeste.simuladorlfa;
    exports grupo.unoeste.simuladorlfa.entities;
}