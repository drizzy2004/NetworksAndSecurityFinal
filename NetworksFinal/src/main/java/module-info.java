module com.example.networksfinal {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.networksfinal to javafx.fxml, javafx.graphics;
}
