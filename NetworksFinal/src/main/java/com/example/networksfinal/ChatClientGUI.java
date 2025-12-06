package com.example.networksfinal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX GUI for the chat client
 */
public class ChatClientGUI extends Application {
    private ChatClient client;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button disconnectButton;
    private Label statusLabel;

    // Connection parameters
    private String host;
    private int port;
    private String username;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat Client");

        // Show connection dialog first
        showConnectionDialog(primaryStage);
    }

    private void showConnectionDialog(Stage primaryStage) {
        Dialog<ConnectionInfo> dialog = new Dialog<>();
        dialog.setTitle("Connect to Chat Server");
        dialog.setHeaderText("Enter connection details");

        // Set the button types
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("8000");
        TextField usernameField = new TextField();

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(usernameField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Request focus on username field by default
        Platform.runLater(usernameField::requestFocus);

        // Convert the result when connect button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                try {
                    return new ConnectionInfo(
                            hostField.getText(),
                            Integer.parseInt(portField.getText()),
                            usernameField.getText()
                    );
                } catch (NumberFormatException e) {
                    showError("Invalid port number");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(info -> {
            if (info != null && !info.username.trim().isEmpty()) {
                connectToServer(primaryStage, info);
            } else {
                Platform.exit();
            }
        });
    }

    private void connectToServer(Stage primaryStage, ConnectionInfo info) {
        this.host = info.host;
        this.port = info.port;
        this.username = info.username;

        try {
            client = new ChatClient(host, port, username);

            if (!client.join()) {
                showError("Failed to join: Username may be taken or server rejected connection");
                Platform.exit();
                return;
            }

            // Setup listener
            client.setMessageListener(new ChatClient.MessageListener() {
                @Override
                public void onMessageReceived(String from, String body) {
                    Platform.runLater(() -> {
                        chatArea.appendText(from + ": " + body + "\n");
                    });
                }

                @Override
                public void onUserJoined(String username) {
                    Platform.runLater(() -> {
                        chatArea.appendText("*** " + username + " joined the chat ***\n");
                    });
                }

                @Override
                public void onUserLeft(String username) {
                    Platform.runLater(() -> {
                        chatArea.appendText("*** " + username + " left the chat ***\n");
                    });
                }

                @Override
                public void onConnectionClosed() {
                    Platform.runLater(() -> {
                        statusLabel.setText("Disconnected");
                        messageField.setDisable(true);
                        sendButton.setDisable(true);
                        disconnectButton.setDisable(true);
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        chatArea.appendText("ERROR: " + error + "\n");
                    });
                }
            });

            // Show main chat window first
            showChatWindow(primaryStage);

            // Start listening for messages
            client.startListening();

            // Request message history after everything is set up
            Platform.runLater(() -> {
                try {
                    client.requestHistory();
                } catch (IOException e) {
                    System.err.println("Failed to request history: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            showError("Connection failed: " + e.getMessage());
            Platform.exit();
        }
    }

    private void showChatWindow(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Status bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Connected as: " + username);
        statusLabel.setStyle("-fx-font-weight: bold;");
        topBar.getChildren().add(statusLabel);
        root.setTop(topBar);

        // Center: Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefRowCount(20);
        root.setCenter(chatArea);

        // Bottom: Message input
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        messageField = new TextField();
        messageField.setPrefWidth(400);
        messageField.setPromptText("Type a message...");

        sendButton = new Button("Send");
        disconnectButton = new Button("Disconnect");

        HBox.setHgrow(messageField, Priority.ALWAYS);
        bottomBar.getChildren().addAll(messageField, sendButton, disconnectButton);
        root.setBottom(bottomBar);

        // Event handlers
        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());
        disconnectButton.setOnAction(e -> disconnect());

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            disconnect();
        });

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        messageField.requestFocus();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                client.sendMessage(message);
                messageField.clear();
            } catch (IOException e) {
                showError("Failed to send message: " + e.getMessage());
            }
        }
    }

    private void disconnect() {
        if (client != null) {
            try {
                client.disconnect();
            } catch (IOException e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            }
        }
        Platform.exit();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class ConnectionInfo {
        String host;
        int port;
        String username;

        ConnectionInfo(String host, int port, String username) {
            this.host = host;
            this.port = port;
            this.username = username;
        }
    }
}