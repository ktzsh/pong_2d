//import org.json.*;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;


public class UDP implements Runnable, WindowListener, ActionListener {

    private boolean virtualHost;
    private JSONObject key_event;
    private JSONObject ballPosition;

    protected InetAddress inetAddress;

    private JSONObject player_score;

    protected int port;
    protected ArrayList<Machine> playerlist;
    private static UDP udp;
    private long acknum = 0;
    //  protected DatagramSocket socket;
    protected DatagramSocket socket;
    protected DatagramPacket outgoing, incoming;


    //Acknowledgement Signals' Hashmap
    HashMap<String,Boolean> StartGameHM = new HashMap<>();
    private boolean started = false;

    public UDP(InetAddress inetAddress, int port) {
        this.inetAddress = inetAddress;
        this.port = port;
        createAndShowGUI();
    }

    public void setUDP(UDP udp) {
        this.udp = udp;
    }

    public ArrayList<Machine> getPlayerlist() {
        return playerlist;
    }

    public void setPlayerlist(ArrayList<Machine> playerlist) {
        this.playerlist = playerlist;
    }

    protected JFrame frameMain;

    protected TextField input;
    protected LobbyServer lobbyServer;

    protected Thread listener;

    public synchronized void start() throws IOException {
        if (listener == null) {
            initNet();
            listener = new Thread(this);
            listener.start();
        }
    }


    protected void initNet() throws IOException {
//        socket = new MulticastSocket (port);
        socket = new DatagramSocket(port);
//        socket.setTimeToLive (5);
//        socket.joinGroup (inetAddress);
        outgoing = new DatagramPacket(new byte[1], 1);
        incoming = new DatagramPacket(new byte[65508], 65508);
    }

    public synchronized void stop() throws IOException {
        if (listener != null) {
            listener.interrupt();
            listener = null;
            socket.close();
        }
    }

    public void windowOpened(WindowEvent event) {
        input.requestFocus();
    }



    public void windowClosing(WindowEvent event) {
        try {
            stop();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void windowClosed(WindowEvent event) {
    }

    public void windowIconified(WindowEvent event) {
    }

    public void windowDeiconified(WindowEvent event) {
    }

    public void windowActivated(WindowEvent event) {
    }

    public void windowDeactivated(WindowEvent event) {
    }

    public void actionPerformed(ActionEvent event) {

    }

    //
    protected synchronized void handleIOException(IOException ex) {
        if (listener != null) {
            if (listener != Thread.currentThread())
                listener.interrupt();
            listener = null;
            socket.close();
        }
    }

    TextArea textArea;

    //run method for listener thread
    public void run() {
        try {
            while (!Thread.interrupted()) {
                incoming.setLength(incoming.getData().length);
                socket.receive(incoming);
                String message = new String(incoming.getData(), 0, incoming.getLength());
//                System.out.println(message);
                JSONObject jsonObject = new JSONObject(message);

                String msg_type = jsonObject.getString("MessageType");


                switch (msg_type) {
                    case "Ball_Moving":
                        ballPosition = jsonObject;
                        break;
                    case "isConnected":
                        sendAck(new Machine(incoming.getAddress().getHostAddress(),incoming.getPort()));
                        break;
                    case "Paddle_Moving":
                        break;
                    case "Wall_Hit":
                        break;
                    case "Paddle_Hit":
                        break;
                    case "Paddle_Remove":
                        break;
                    case "Ack":
                        acknum++;
                        break;
                    case "Acknowledge":
                        StartGameHM.put(InetAddress.getLocalHost().getHostAddress(),true);
                        StartGameHM.put(incoming.getAddress().getHostAddress(), true);
                        System.out.println("Acknowledge: "+ incoming.getAddress().getHostAddress());
                        JSONObject jsonObject3 = new JSONObject();
                        jsonObject3.put("MessageType","Start");
                        String jsonString = jsonObject.toString(); //only for string data ?? put(String,bool)??
                        byte[] bytes3 = jsonString.getBytes();
//                        receiveAcknowledgement(bytes3, playerlist, "StartGame");
                        new Pong(udp);
                        break;
                    case "Win":
                        break;
                    case "Player_Score":
                        player_score=jsonObject;
                        break;
                    case "Key_Event":
//                        System.out.println("space lodulakhan kaju");
                        key_event=jsonObject;
                        break;
                    case "Start" :
                        System.out.println("Start");
                        if(started){
                            break;
                        }
                        started = true;
//                        System.out.println("Start");
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("MessageType","Acknowledge");
                        jsonObject1.put("responseType", "GameStart");

                        String messageack = jsonObject1.toString();
                        byte[] bytes1 = messageack.getBytes();

                        DatagramPacket datagramPacket = new DatagramPacket(bytes1,bytes1.length,incoming.getAddress(),incoming.getPort());
                        socket.send(datagramPacket);

                        checkRoomMsg();//to get latest players
                        new Pong(udp);
                        Thread sendPackets = new Thread(){
                            long sentnum = 0;
                            @Override
                            public void run() {
                                //sendPacketof isConnected

                                while (!virtualHost) {
                                    isConnected();
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Sent: "+sentnum);
                                    System.out.println("Ack: "+acknum);
                                    if ((sentnum - acknum) > 5) {
                                        if (playerlist.size() > 1) {

                                            try {
                                                if (playerlist.get(1).getIp().equals(InetAddress.getLocalHost().getHostAddress()))
                                                    virtualHost = true;
                                            } catch (UnknownHostException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        ArrayList<Machine> newarraylist = new ArrayList<>();
                                        for (int i = 0; i < playerlist.size(); i++) {
                                            System.out.println("this time");
                                            if (i > 0) {
                                                newarraylist.add(playerlist.get(i));
                                            }
                                        }

                                        setPlayerlist(newarraylist);
                                        sentnum = 0;
                                        acknum = 0;
                                    }
                                    sentnum++;
                                    System.out.println(virtualHost);
                                }
                            }
                        };
//                        sendPackets.start();
                        break;
                    case "lobbyResp":

                        textArea.setText("");
                        playerlist = new ArrayList<>();
                        System.out.println(jsonObject.getInt("numPlayers"));
                        for (int i = 0; i < jsonObject.getInt("numPlayers"); i++) {
                            String ip_port = jsonObject.getString("Player" + i);
                            textArea.append( ip_port + "\n");
                            String[] ip_ports = ip_port.split(":");
                            Machine machine = new Machine(ip_ports[0],Integer.parseInt(ip_ports[1]));
                            playerlist.add(machine);
                        }
                        break;
                    default:
                        System.out.println("Unknown Message Type");
                }

            }
        } catch (IOException ex) {
            handleIOException(ex);
        }
    }

    public boolean getVirtualHost(){return virtualHost;}
    //send key event
    public void sendKeyEvent(int event_code, String type, int playerIndex){
        JSONObject jsonObject= new JSONObject();

        jsonObject.put("key_event_code", event_code);
        jsonObject.put("MessageType", "Key_Event");
        jsonObject.put("event_type", type);
        jsonObject.put("playerIndex", playerIndex);
        String jsonString = jsonObject.toString();
        byte[] bytes = jsonString.getBytes();
        sendToPlayers(bytes);
    }
    //send score
    public void sendPlayerScore(Integer player_1_score, Integer player_2_score, Integer player_3_score, Integer player_4_score){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("MessageType", "Player_Score");
        jsonObject.put("player_1_score", player_1_score);
        jsonObject.put("player_2_score", player_2_score);
        jsonObject.put("player_3_score", player_3_score);
        jsonObject.put("player_4_score", player_4_score);
        String jsonString = jsonObject.toString();
        byte[] bytes = jsonString.getBytes();

        sendToPlayers(bytes);

    }

    //retreiving keyEvent
    public JSONObject getKeyEvent(){
        return key_event;
    }

    public JSONObject getPlayerScore(){
        return player_score;
    }

    public void resetKeyEvent(){
        key_event=null;
    }
    public void resetScoreEvent(){
        player_score=null;
    }


    // Send Message Stuff


    public void resetBallPosition() {
        this.ballPosition = null;
    }







    public void sendBallInfo(double ball_x, double ball_y, double vel_x, double vel_y, int ball_id) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("MessageType", "Ball_Moving");
        jsonObject.put("ball_x", ball_x);
        jsonObject.put("ball_y", ball_y);
        jsonObject.put("vel_x", vel_x);
        jsonObject.put("vel_y", vel_y);
        jsonObject.put("ball_id", ball_id);

        String jsonString = jsonObject.toString();
        byte[] bytes = jsonString.getBytes();
        sendToPlayers(bytes);
    }

    public JSONObject getBallPosition(){
        return ballPosition;
    }

    public void StartGame() {
        JSONObject jsonObject = new JSONObject();
        //initialize StartGameHM
        for (Machine machine : playerlist) {
            StartGameHM.put(machine.getIp(),false);
        }
        for (Boolean aBoolean : StartGameHM.values()) {
            System.out.println(aBoolean);
        }
        System.out.println();
        jsonObject.put("MessageType","Start");

        String jsonString = jsonObject.toString(); //only for string data ?? put(String,bool)??
        byte[] bytes = jsonString.getBytes();
        sendToPlayers(bytes);

//        checkRoomMsg();//to get latest players


    }

    public boolean AndHashMap(HashMap<String,Boolean> hashMap){
        boolean result = true;
        for (String s : hashMap.keySet()) {
            result = result & hashMap.get(s);
        }
        return result;
    }

    int acknowledgecount = 0;

    public synchronized void receiveAcknowledgement(byte[] bytes, ArrayList<Machine> playerlist, String startGame) {
        DatagramPacket acknowledgement = new DatagramPacket(new byte[1024], 1024);
//        incoming = new DatagramPacket(new byte[65508], 65508);
        try {
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            socket.receive(acknowledgement);
        }
        catch (SocketTimeoutException e1){
            for (String s : StartGameHM.keySet()) {
                try {
                    if(!StartGameHM.get(s) && !(s.equals(InetAddress.getLocalHost().getHostAddress()))){

                        DatagramPacket packet = null;
                        try {
                            packet = new DatagramPacket(bytes,bytes.length, InetAddress.getByName(s),getPort(playerlist, s));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        try {
                            assert packet != null;
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(acknowledgement.getAddress()!=null) {
            InetAddress ipaddress = acknowledgement.getAddress();
            String ip = ipaddress.getHostAddress();

//        int portnum = acknowledgement.getPort();

            String message = new String(acknowledgement.getData(), 0, acknowledgement.getLength());
            JSONObject jsonObject = new JSONObject(message);

            String messageType = jsonObject.getString("MessageType");
            String responseOf = jsonObject.getString("responseType");

            if (messageType.equals("Acknowledge") && (responseOf.equals("GameStart"))) {
                StartGameHM.put(ip, true);
                System.out.println("Acknowledge: " + ip);
            }
        }
        while (!AndHashMap(StartGameHM)){
            if(acknowledgecount > 4){
                break;
            }
            acknowledgecount++;
            receiveAcknowledgement(bytes, playerlist, "StartGame");

        }

        return;


    }

    private int getPort(ArrayList<Machine> playerlist, String s) {
        for (Machine machine : playerlist) {
            if(machine.getIp().equals(s))
                return machine.getPort();
        }
        return 0;
    }

    public void checkRoomMsg() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("MessageType", "checkRoom");
        jsonObject.put("connect", true);

        String jsonString = jsonObject.toString(); //only for string data ?? put(String,bool)??
        byte[] bytes = jsonString.getBytes();
        InetAddress broadcast = null;
        try {
            broadcast = InetAddress.getByName("255.255.255.255");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        DatagramPacket lobbyServer = new DatagramPacket(bytes, bytes.length, broadcast, 1235);

        try {
            socket.send(lobbyServer);
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    public void leaveRoomMsg(){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("MessageType", "checkRoom");
        jsonObject.put("connect", false);

        String jsonString = jsonObject.toString(); //only for string data ?? put(String,bool)??
        byte[] bytes = jsonString.getBytes();
        InetAddress broadcast = null;
        try {
            broadcast = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        DatagramPacket lobbyServer = new DatagramPacket(bytes, bytes.length, broadcast, 1235);
        try {
            socket.send(lobbyServer);
        } catch (IOException e) {
            handleIOException(e);
        }
    }



    public void isConnected(){
        try {
            InetAddress inetAddress = InetAddress.getByName(playerlist.get(0).getIp());
            int port = playerlist.get(0).getPort();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("MessageType","isConnected");

            byte[] bytes = jsonObject.toString().getBytes();

            DatagramPacket packet = new DatagramPacket(bytes,bytes.length,inetAddress,port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectMsg() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("MessageType", "connectToGame");
        String jsonString = jsonObject.toString();
        byte[] bytes = jsonString.getBytes();
        InetAddress broadcast = null;
        try {
            broadcast = InetAddress.getByName("255.255.255.255");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        DatagramPacket lobbyServer = new DatagramPacket(bytes, bytes.length, broadcast, 1235);
        try {
            socket.send(lobbyServer);
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    public void sendAck(Machine machine) {
        JSONObject jsonobject = new JSONObject();
        jsonobject.put("MessageType","Ack");
        byte[] bytes = jsonobject.toString().getBytes();
        InetAddress broadcast = null;
        try {
            broadcast = InetAddress.getByName(machine.getIp());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        DatagramPacket startGame = new DatagramPacket(bytes, bytes.length, broadcast, machine.getPort());
        try {
            socket.send(startGame);
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    public void sendToPlayers(byte[] bytes){ //method to send json object to all excluding itself
        for (Machine machine : playerlist) {
            try {
                if(!(machine.getIp().equals(InetAddress.getLocalHost().getHostAddress())))
                {
                    InetAddress broadcast = null;
                    try {
                        broadcast = InetAddress.getByName(machine.getIp());
                        //            broadcast = InetAddress.getByName("127.0.0.1");
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    DatagramPacket startGame = new DatagramPacket(bytes, bytes.length, broadcast, machine.getPort());
                    //        outgoing.setData(bytes);
                    //        outgoing.setLength(bytes.length);
                    try {
                        socket.send(startGame);

                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }
    }





    /// GUI
    final static boolean shouldFill = true;
    final static boolean shouldWeightX = true;
    final static boolean RIGHT_TO_LEFT = false;

    public void addComponentsToPane(Container pane) {
        if (RIGHT_TO_LEFT) {
            pane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }

        JButton buttonCreateRoom;
        JButton buttonJoinRoom;
        JButton buttonCheckPlayer;
        JButton buttonPlay;
        JButton buttonClose;
        JButton buttonDisconnect;

        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        if (shouldFill) {
            //natural height, maximum width
            c.fill = GridBagConstraints.HORIZONTAL;
        }


        buttonCreateRoom = new JButton("Create Room");
        if (shouldWeightX) {
            c.weightx = 0.5;
        }
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(buttonCreateRoom, c);

        buttonJoinRoom = new JButton("Join Room");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        pane.add(buttonJoinRoom, c);

        buttonCheckPlayer = new JButton("Check Players");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;
        pane.add(buttonCheckPlayer, c);

        textArea = new TextArea();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 40;      //make this component tall
        c.weightx = 0.0;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(textArea, c);

        buttonPlay = new JButton("Play");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10, 0, 0, 0);  //top padding
        c.gridx = 2;       //aligned with button 2
        c.gridwidth = 1;   //2 columns wide
        c.gridy = 2;       //third row
        pane.add(buttonPlay, c);


        buttonClose = new JButton("Close");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10, 0, 0, 0);  //top padding
        c.gridx = 0;       //aligned with button 2
        c.gridwidth = 1;   //2 columns wide
        c.gridy = 2;       //third row
        pane.add(buttonClose, c);


        buttonDisconnect = new JButton("Disconnect");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.weightx = 1.5;
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10, 0, 0, 0);  //top padding
        c.gridx = 1;       //aligned with button 2
        c.gridwidth = 1;   //2 columns wide
        c.gridy = 2;       //third row
        pane.add(buttonDisconnect, c);

        buttonCreateRoom.setBorderPainted(true);
        buttonCreateRoom.setFocusPainted(false);
        buttonCreateRoom.setContentAreaFilled(false);

        buttonJoinRoom.setBorderPainted(true);
        buttonJoinRoom.setFocusPainted(false);
        buttonJoinRoom.setContentAreaFilled(false);

        buttonCheckPlayer.setBorderPainted(true);
        buttonCheckPlayer.setFocusPainted(false);
        buttonCheckPlayer.setContentAreaFilled(false);

        buttonClose.setBorderPainted(true);
        buttonClose.setFocusPainted(false);
        buttonClose.setContentAreaFilled(false);

        buttonDisconnect.setBorderPainted(true);
        buttonDisconnect.setFocusPainted(false);
        buttonDisconnect.setContentAreaFilled(false);

        buttonPlay.setBorderPainted(true);
        buttonPlay.setFocusPainted(false);
        buttonPlay.setContentAreaFilled(false);


        buttonCheckPlayer.setEnabled(false);
        buttonDisconnect.setEnabled(false);
        buttonPlay.setEnabled(false);

        buttonCreateRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread threadLobby = new Thread(){
                    @Override
                    public void run() {
                        lobbyServer = new LobbyServer();
                    }
                };
                threadLobby.start();//thread for starting the lobby room
                connectMsg();//message to join the room as well
                checkRoomMsg();//message to check the current players
                buttonCreateRoom.setEnabled(false);
                buttonJoinRoom.setEnabled(false);
                buttonCheckPlayer.setEnabled(true);
                buttonDisconnect.setEnabled(true);
                buttonPlay.setEnabled(true);
            }
        });

        buttonJoinRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectMsg();//message to join the room as well
                checkRoomMsg();//message to check the current players
                buttonJoinRoom.setEnabled(false);
                buttonCreateRoom.setEnabled(false);
                buttonCheckPlayer.setEnabled(true);
                buttonDisconnect.setEnabled(true);
                buttonPlay.setEnabled(true);
            }
        });

        buttonDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveRoomMsg();
                playerlist = new ArrayList<Machine>();
                lobbyServer.closeSocket();
                buttonJoinRoom.setEnabled(true);
                buttonCreateRoom.setEnabled(true);
                buttonCheckPlayer.setEnabled(false);
                buttonDisconnect.setEnabled(false);
                buttonPlay.setEnabled(false);
            }
        });

        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveRoomMsg();
                frameMain.setVisible(false);
                lobbyServer.closeSocket();
                socket.disconnect();
                socket.close();
                listener.interrupt();
                listener = null;
            }
        });


        buttonPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkRoomMsg();//latset room players
                StartGame();
                virtualHost = true;
            }
        });

        buttonCheckPlayer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkRoomMsg();
                buttonJoinRoom.setEnabled(false);
                buttonCheckPlayer.setEnabled(true);
                buttonDisconnect.setEnabled(true);
                buttonPlay.setEnabled(true);
            }
        });
    }


    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createAndShowGUI() {
        //Create and set up the window.
        frameMain = new JFrame("Multiplayer");
        frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content pane.
        addComponentsToPane(frameMain.getContentPane());

        //Display the window.
        frameMain.pack();
        frameMain.setLocationRelativeTo(null);
        frameMain.setVisible(true);
    }



}