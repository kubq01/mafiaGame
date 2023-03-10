package com.example.mafiaclient;

import com.example.mafiaclient.client.Client;
import com.example.mafiaclient.client.Player;
import com.example.mafiaclient.client.PlayerAction;
import com.example.mafiaclient.client.RoleEnum;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class HelloController {
    /*
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

     */

    @FXML
    private TextArea enterChatTextArea;
    @FXML
    private Text titleText;
    @FXML
    private Button voteButton;
    @FXML
    private VBox playersListVbox;

    @FXML
    private Text userInfoRole;
    @FXML
    private Text descriptionText;

    @FXML
    private VBox chatView;

    @FXML
    private DialogPane userInfoPane;

    @FXML
    private Text userInfoPaneHeader;
    private Client client;//= new Client("127.0.0.1",4445);

    private boolean isHost = false;
    private Boolean isNight;

    private Map<Integer, Integer> translatePaneToPlayer = new HashMap<>();
    private int translationCounter = 0;


    private List<DialogPane> playersDialog = new ArrayList<>();
    private int selectedPlayer = -1;

    private Player player;
    private Map<Integer,DialogPane> dialogMap = new HashMap<>();

    private PossibleAction possibleAction;

   public HelloController() {
       try {
           Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
           alert.setTitle("Welcome");
           alert.setHeaderText(null);


           TextField textField = new TextField();
           textField.setPromptText("Please enter your nick");
           alert.getDialogPane().setContent(textField);

           ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
           alert.getButtonTypes().setAll(okButton, ButtonType.CANCEL);

           Node okButtonNode = alert.getDialogPane().lookupButton(okButton);
           okButtonNode.setDisable(true);

           textField.textProperty().addListener((observable, oldValue, newValue) -> {
               okButtonNode.setDisable(newValue.trim().isEmpty());
           });

           Optional<ButtonType> result = alert.showAndWait();
           if (result.isPresent()) {
               if(result.get() == okButton) {
                   String nickname = textField.getText();
                   //userInfoPane.setHeaderText(nickname);
                   //userInfoPaneHeader.setText(nickname);
                   player = new Player(1, RoleEnum.WAITING, nickname);
                   client = new Client("127.0.0.1", 4445, this, player);
               }else if(result.get() == ButtonType.CANCEL || result.get() == ButtonType.CLOSE)
               {
                   Platform.exit();
               }
           }

       }catch (Exception e)
       {

       }
   }

   public void setHost()
   {
       isHost = true;
       voteButton.setText("Start game");
   }

   public void setNickText(Player player)
   {
       this.player = player;
       //userInfoPane.setHeaderText(player.getNick());
       userInfoPaneHeader.setText(player.getNick());
       System.out.println("new nick is "+player.getNick());
   }


    public void sendChat(KeyEvent event)
    {
        if(event.getCode() == KeyCode.ENTER)
        {
            String text = enterChatTextArea.getText();
            System.out.println(text.substring(0,text.length()-1));
            enterChatTextArea.clear();

            if((this.isNight && !player.getRole().equals(RoleEnum.MAFIA)) ||(player.getRole().equals(RoleEnum.DECEASED))||(player.getRole().equals(RoleEnum.NOT_INITIALIZED))) {

                DialogPane pane = new DialogPane();
                Text textContent = new Text();
                textContent.setText("Nie mozesz teraz wysylac wiadomosci!");
                pane.setContent(textContent);

                chatView.getChildren().add(pane);

                return;
            }
            try {

               client.sendMessageToChat(String.valueOf("01" + player.getNick()+": "+ text.substring(0, text.length() - 1)));
            }catch (Exception e)
            {

            }
        }
    }

    public void updateChat(String msg)
    {

        if((this.isNight && !player.getRole().equals(RoleEnum.MAFIA))  ||(player.getRole().equals(RoleEnum.DECEASED))||(player.getRole().equals(RoleEnum.NOT_INITIALIZED))) {

            return;
        }
        System.out.println("Hello cotroller, update chat");

        DialogPane pane = new DialogPane();
        Text textContent = new Text();
        textContent.setText(msg);
        pane.setContent(textContent);

        chatView.getChildren().add(pane);
    }

    public void startOrVote()
    {
        if(isHost && playersDialog.size()>=3) {
            client.startGame(-1);
            isHost = false;
            voteButton.setText("Vote Unavailable");
        }
        else{
            switch (possibleAction){

                case VOTE -> {
                    if (client.sendAction(new PlayerAction(player.getID(), selectedPlayer))) {
                        possibleAction = PossibleAction.WAIT;
                        voteButton.setText("Vote Unavailable");
                    }}
                case CHECK -> {
                    try {
                        if (checkPopup(selectedPlayer)) {
                            possibleAction = PossibleAction.WAIT;
                            voteButton.setText("Vote Unavailable");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case WAIT -> {
                }
            }
        }
    }

    private boolean checkPopup(int selectedPlayer) throws IOException {
        //to
       /*FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("popup.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 200, 100);
        Stage stage = new Stage();
        stage.setTitle("Mafia!");
        stage.setScene(scene);
        stage.show();*/
        //albo to
        RoleEnum role = playerRole(selectedPlayer);
        Scene scn = new Scene(new Group());
        Stage stage = new Stage();
        stage.setTitle("Player role");
        stage.setWidth(200);
        stage.setHeight(100);
        HBox hb = new HBox();
        Label lbl = new Label(role.toString());
        switch (role){

            case MAFIA -> lbl.setTextFill(Color.web("Red"));
            case REGULAR -> lbl.setTextFill(Color.web("Blue"));
            default -> {
                return false;
            }
        }
        hb.setSpacing(10);
        hb.getChildren().add((lbl));
        ((Group) scn.getRoot()).getChildren().add(hb);
        stage.setScene(scn);
        stage.show();
        return true;
    }

    public boolean finishPopup(boolean mafiaWon) throws IOException {
        Scene scn = new Scene(new Group());
        Stage stage = new Stage();
        stage.setTitle("Finish");
        stage.setWidth(200);
        stage.setHeight(200);
        HBox hb = new HBox();
        Label lbl;
        if (mafiaWon){
            lbl = new Label("Mafia has won.");
            if (player.getRole() == RoleEnum.MAFIA){
                lbl.setTextFill(Color.web("Green"));
            }
            else {
                lbl.setTextFill(Color.web("Red"));
            }
        }
        else {
            lbl = new Label("Mafia has lost.");
            if (player.getRole() == RoleEnum.MAFIA){
                lbl.setTextFill(Color.web("Red"));
            }
            else {
                lbl.setTextFill(Color.web("Green"));
            }
        }
        hb.setSpacing(10);
        hb.getChildren().add((lbl));
        ((Group) scn.getRoot()).getChildren().add(hb);
        stage.setScene(scn);
        stage.show();
        return true;
    }

    private RoleEnum playerRole(int selectedPlayer) {
       List<Player> players = client.getPlayerList();
        for (Player player :
                players) {
            if (player.getID() == selectedPlayer){
                return player.getRole();
            }
        }
        return RoleEnum.NOT_INITIALIZED;
    }

    public void addPlayers(Player player)
    {

        DialogPane pane = new DialogPane();
        Text textHeader = new Text();
        textHeader.setText(player.getNick());
        pane.setHeader(textHeader);
        Text textContent = new Text();
        textContent.setText(player.getRole().toString());
        textContent.setWrappingWidth(150);
        pane.setContent(textContent);
        pane.setId(String.valueOf(playersDialog.size()));
        translatePaneToPlayer.put(translationCounter,player.getID());
        translationCounter++;
        pane.setOnMouseClicked(event -> {
            System.out.println(player.getID());
            if(selectedPlayer<0) {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#d45148"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = translatePaneToPlayer.get(Integer.parseInt(pane.getId()));
            }else if(selectedPlayer == Integer.parseInt(pane.getId()))
            {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = -1;
            }else
            {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#d45148"), CornerRadii.EMPTY, Insets.EMPTY)));
                DialogPane selectedPane = playersDialog.get(selectedPlayer);
                selectedPane.setBackground(new Background(
                        new BackgroundFill(Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = translatePaneToPlayer.get(Integer.parseInt(pane.getId()));
            }
        });

        playersDialog.add(pane);

        playersListVbox.getChildren().add(pane);
        dialogMap.put(player.getID(),pane);
        System.out.println("Player id for pane is "+player.getID());

    }

    public void testAddPlayer()
    {
        addPlayers(new Player(1, RoleEnum.NOT_INITIALIZED,"nick"));
    }

    public void updatePlayer(List<Player> playerList)
    {

        for(Player player : playerList)
        {
            if(player.getID() == this.player.getID())
            {
                this.player = player;
                userInfoRole.setText("Your role is: "+player.getRole().toString());
                break;
            }
        }
        //TODO - jesli detektyw oraz jest noc to mozesz podejrzec role jednego z graczy(ylko jednego!)
        //TODO - boolean aby okreslic czy juz odkryles gracza w tej turze i restart jej co zmiane na noc
        //TODO - podmienic button vote na odkrycie gracza gdy rola = detektyw i jest noc

        for(Player player : playerList)
        {
            DialogPane pane = dialogMap.get(player.getID());
            Text textContent = new Text();
            if(this.player.getRole() == RoleEnum.MAFIA && player.getRole() == RoleEnum.MAFIA || this.player.getID() == player.getID()
            || player.getRole() == RoleEnum.DECEASED)
            {
                textContent.setText(player.getRole().toString());
            }else {
                textContent.setText("You cannot see this player's role");
            }

            textContent.setWrappingWidth(150);
            pane.setContent(textContent);
        }
        setDayOrNight();

        descriptionText.setText("It's time to chat");

    }

    private void setDayOrNight() {
        if(isNight){
            titleText.setText("Night");

        }
        else{
            titleText.setText("Day");
        }
    }

    public void updateState(Boolean isNight){
       System.out.println("update state " + isNight);
       this.isNight=isNight;
       updateController();
       //setDayOrNight();
    }

    private void updateController() {
       checkState();
       switch (possibleAction){
           case VOTE -> voteButton.setText("Vote");
           case CHECK -> voteButton.setText("Check");
           case WAIT -> voteButton.setText("Vote Unavailable");
       }
    }

    public void exitGameAlert()
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Game started");
        alert.setHeaderText(null);
        alert.setContentText("The game has already started. You cannot join");

        alert.getButtonTypes().setAll(ButtonType.CLOSE);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent())
        {
            System.out.println("Exit app");
            Platform.exit();
        }
    }

    private void checkState(){
        setDayOrNight();
        System.out.println("Player's role is "+ player.getRole());
       switch (player.getRole()){
           case MAFIA -> possibleAction = PossibleAction.VOTE;
           case DETECTIVE -> {
               if (isNight){
                   possibleAction = PossibleAction.CHECK;
               }
               else {
                   possibleAction = PossibleAction.VOTE;
               }
           }
           case REGULAR -> {
               if (isNight){
                   possibleAction = PossibleAction.WAIT;
               }
               else {
                   possibleAction = PossibleAction.VOTE;
               }
           }
           default -> possibleAction = PossibleAction.WAIT;
       }
    }

    private enum PossibleAction{
       VOTE, CHECK, WAIT
    }
}