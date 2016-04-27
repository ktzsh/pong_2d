

import javafx.geometry.Point2D;
import jdk.management.cmm.SystemResourcePressureMXBean;
import org.json.JSONObject;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class Board extends JPanel implements ActionListener {


    private static final long serialVersionUID = 1L; //suppresses warning

    //Game constants
    public int WINDOW_X = 650;
    public int WINDOW_Y = 650;
    private int MAX_SCORE = 5;

    private double BALL_SPEEDX = 1.2;
    private double BALL_SPEEDY = 0.9;

    private int BALL_HEIGHT = 12;
    private int BALL_WIDTH = 12;
    private int PADDLE_WIDTH = 8;
    private int PADDLE_HEIGHT = 60;
    private double PADDLE_SPEED = 2;
    private double PADDLE_SPEED_AI = 3;

    //game state variables
    private int server = 0; //0 = left, 1 = right
    private String message = "";
    private int player_1_score = 0;
    private int player_2_score = 0;
    private int player_3_score = 0;
    private int player_4_score = 0;
    private int pane_x; //the playable game area is smaller than the window
    private int pane_y;
    private boolean pause = true; //start paused for instruction screen
    private Rectangle2D paddleOne_one;
    private Rectangle2D paddleTwo_one;
    private Rectangle2D paddleOne_two;
    private Rectangle2D paddleTwo_two;
    private Timer timer;
    private double paddleOneY;
    private double paddleTwoX;
    private double paddleOneOppY;
    private double paddleTwoOppX;

    private boolean positiveYOne_one;
    private boolean positiveYOne_two;
    private boolean positiveXTwo_one;
    private boolean positiveXTwo_two;

    private boolean OneOneStop = true;
    private boolean OneTwoStop = true;
    private boolean TwoOneStop = true;
    private boolean TwoTwoStop = true;

    private double ball_x;
    private double ball_y;

    private double ball_vel_x = 1;
    private double ball_vel_y = 1;

    private boolean positiveBallX = true;
    private boolean positiveBallY = true;

    double eRestitution = 0.003; //restitution
    double aRestitution = 0.003; //resitution

    private String gameMode;

    private UDP UDPObject;

    private int playerIndex;


    //hand off key presses to the Paddles class.
    private Paddles paddles;
    private boolean flag = false;

    private class TAdapter extends KeyAdapter {

        public void keyReleased(KeyEvent e) {
            paddles.keyReleased(e.getKeyCode(), playerIndex);
            if(UDPObject!=null)
                UDPObject.sendKeyEvent(e.getKeyCode(), "Released", playerIndex);
        }

        public void keyPressed(KeyEvent e) {
            paddles.keyPressed(e.getKeyCode(), playerIndex);
            if(UDPObject!=null) {
                UDPObject.sendKeyEvent(e.getKeyCode(), "Pressed", playerIndex);
            }
        }
    }

    public int getRandomBallX() {
        int max = pane_x / 2 + 100;
        int min = pane_x / 2 - 100;
        Random random = new Random();
        int rdmX = random.nextInt(max - min + 1) + min;
//		System.out.println(rdmX);
        return rdmX;
    }

    public int getRandomBallY() {
        int max = pane_x / 2 + 100;
        int min = pane_x / 2 - 100;
        Random random = new Random();
        int rdmY = random.nextInt(max - min + 1) + min;
//		System.out.println(rdmY);
        return rdmY;
    }

    public Board(String mode, UDP UDPObject) {
        paddles = new Paddles();
        addKeyListener(new TAdapter());
        setFocusable(true);
        setBackground(Color.BLACK);
        this.gameMode = mode;
        this.UDPObject = UDPObject;

        if (UDPObject!=null) {
            ArrayList<Machine> playersList = UDPObject.getPlayerlist();

            try {
                String ipMy = InetAddress.getLocalHost().getHostAddress();

                for(int i=0; i<playersList.size(); i++)
                {
                    if (playersList.get(i).getIp().equals(ipMy)){
                        playerIndex = i;
                        break;
                    }
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
        }
        System.out.println("KTZ:::: " + playerIndex);

        //create polygons used in the game
//        paddleOne_one = new Rectangle(0,0,PADDLE_WIDTH, PADDLE_HEIGHT);
//        paddleOne_two = new Rectangle(0,0,PADDLE_WIDTH, PADDLE_HEIGHT);
//		paddleTwo_one = new Rectangle(0,0,PADDLE_HEIGHT, PADDLE_WIDTH);
//		paddleTwo_two = new Rectangle(0,0,PADDLE_HEIGHT, PADDLE_WIDTH);

        setDoubleBuffered(true);
        timer = new Timer(5, this);
        timer.start();
    }

    public void Update_Dimensions() {
        pane_y = this.getHeight();
        pane_x = this.getWidth();
        resetBall("Begin Game");
        //System.out.println(pane_x + "::" + pane_y);
        paddleOneY = pane_y / 2 - PADDLE_HEIGHT / 2;
        paddleOneOppY = pane_y / 2 - PADDLE_HEIGHT / 2;
        paddleTwoX = pane_x / 2 - PADDLE_HEIGHT / 2;
        paddleTwoOppX = pane_x / 2 - PADDLE_HEIGHT / 2;
    }

    public void paint(Graphics g) {

        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = new Font("Arial", Font.PLAIN, 40);

        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        if (message.length() > 0) {

            g2d.drawString(message, pane_x / 2 - (60 + message.length() * 4), 120);
            if (pause == true) {
                g2d.drawString("Press SPACE to Start", pane_x / 2 - (140 + message.length() * 4), 220);
            }
        }
        //draw scores on the left and rightn


        if (gameMode.equals("Single")) {
            g2d.drawString(Integer.toString(player_1_score), pane_x / 2, pane_y / 2 + 60);
            g2d.drawString(Integer.toString(player_2_score), pane_x / 2 - 60, pane_y / 2);
            g2d.drawString(Integer.toString(player_3_score), pane_x / 2, pane_y / 2 - 60);
            g2d.drawString(Integer.toString(player_4_score), pane_x / 2 + 60, pane_y / 2);
        }
        else {
            g2d.drawString(Integer.toString(player_1_score), pane_x / 2 - 60, pane_y / 2);
            g2d.drawString(Integer.toString(player_2_score), pane_x / 2 + 60, pane_y / 2);
        }

        //update paddle locations
//        paddleOne_one.setLocation(0, paddleOneY);
//		paddleOne_two.setLocation(pane_x-PADDLE_WIDTH, paddleOneOppY);//subtract 1 important*******************//
//        paddleTwo_one.setLocation(paddleTwoX, 0);
//		paddleTwo_two.setLocation(paddleTwoOppX, pane_y-PADDLE_WIDTH);

        paddleOne_one = new Rectangle2D.Double(0, paddleOneY, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddleOne_two = new Rectangle2D.Double(pane_x - PADDLE_WIDTH, paddleOneOppY, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddleTwo_one = new Rectangle2D.Double(paddleTwoX, 0, PADDLE_HEIGHT, PADDLE_WIDTH);
        paddleTwo_two = new Rectangle2D.Double(paddleTwoOppX, pane_y - PADDLE_WIDTH, PADDLE_HEIGHT, PADDLE_WIDTH);

        g2d.setColor(Color.WHITE);
        g2d.fill(paddleOne_one);
        g2d.draw(paddleOne_one);
        g2d.fill(paddleTwo_two);
        g2d.draw(paddleTwo_two);
        g2d.fill(paddleOne_two);
        g2d.draw(paddleOne_two);
        g2d.fill(paddleTwo_one);
        g2d.draw(paddleTwo_one);
        //update ball location
        Ellipse2D.Double ball = new Ellipse2D.Double(ball_x, ball_y, BALL_WIDTH, BALL_HEIGHT);
        g2d.fill(ball);
        g2d.draw(ball);

        //g2d.draw(ball);
        Toolkit.getDefaultToolkit().sync();
        g.dispose();

    }

    private void resetBall(String msg) {
        message = msg;
//        ball_x = getRandomBallX();
//        ball_y = getRandomBallY();
        ball_x = pane_x / 2;
        ball_y = pane_y / 2;

        BALL_SPEEDX = 1.2;
        BALL_SPEEDY = 0.9;

        //reverse serve direction every time
//    	if(server == 0){
//    		server = 1;
//    		if(ball_vel_x > 0){
//    			ball_vel_x *= -1;
//    		}
//
//    	} else {
//    		server = 0;
//    		if(ball_vel_x < 0){
//    			ball_vel_x *= -1;
//    		}
//    	}

    }

    private void resetScore() {
        player_1_score = 0;
        player_2_score = 0;
    }

    /**
     * Read inputs, update the game state, paint window.
     * Called by the Timer
     */
    public void actionPerformed(ActionEvent e) {
        //wait for message display
        if (message.length() > 0) {
            try {
                Thread.sleep(800);
                message = "";
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        JSONObject key_event;
        if (UDPObject != null) {
            key_event = UDPObject.getKeyEvent();

            if (key_event == null) {
//                System.out.println("null");
            } else {

                String event_type = key_event.getString("event_type");
                int key_event_code = key_event.getInt("key_event_code");
                int playerIndex_others = key_event.getInt("playerIndex");
//                System.out.println(event_type + " " + key_event_code);
                if (event_type.equals("Pressed")) {
                    paddles.keyPressed(key_event_code, playerIndex_others);
                } else if (event_type.equals("Released")) {
                    paddles.keyReleased(key_event_code, playerIndex_others);
                }

            }
            UDPObject.resetKeyEvent();
        }
        //check for pause

        if (paddles.Space == true) {
            //wait for debounce
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (!flag) {
                pause = !pause;
                flag = true;
            }

        } else {
            flag = false;
        }
        if (pause == true) {
            return;
        }


        //check for end game
        if (paddles.Esc == true) {
            resetScore();
            resetBall("Game Over");
            repaint();

            pause = true;

        }
        //apply input from keys


        InputApply(gameMode);


        //collision detection
        /*	The math here is generous to the players.
    		Hitting the side of the paddle still results in a bounce.
    	*/
        //hit one_one paddle and one_two

        //paddle one_one ************************************************************************************************
        //***************************************************************************************************************


        if (ball_x < PADDLE_WIDTH && (ball_y + BALL_HEIGHT > paddleOneY && ball_y < paddleOneY + PADDLE_HEIGHT)) {

            ball_vel_x *= -1;
            ball_x = PADDLE_WIDTH;
            BALL_SPEEDX *= (1 + eRestitution * (Math.abs(paddleOneY + PADDLE_HEIGHT / 2 - ball_y)));

            if (OneOneStop) {

            } else if (positiveYOne_one == positiveBallY) {
                BALL_SPEEDY += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleOneY + PADDLE_HEIGHT / 2 - ball_y));
                ball_vel_y *= 1;
//                System.out.println("X:" + ball_x + "  Y:" + ball_y + "SPEED: " + BALL_SPEEDX + "::" + BALL_SPEEDY);

            } else {
                BALL_SPEEDY += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleOneY + PADDLE_HEIGHT / 2 - ball_y));
                ball_vel_y *= -1;
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 1) {
                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                }
            }


            ; // put back in front of paddle if it's behind.
        } else if (ball_x < 0) {

            //hit left wall*********************************************************************************************
            //**********************************************************************************************************

            ball_vel_x *= -1;
            if (gameMode.equals("Single")) {
                player_2_score++;
                if (player_2_score >= MAX_SCORE) {
                    //Do Checks
                }
            } else {
                player_1_score++;
                if (player_1_score >= MAX_SCORE) {
                    resetBall("Player 2 Wins!");
                    resetScore();
                }
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 1) {

                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                    UDPObject.sendPlayerScore(player_1_score, player_2_score, player_3_score, player_4_score);

                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    JSONObject player_scores = UDPObject.getBallPosition();

                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                    if (player_scores != null) {
                        player_1_score = player_scores.getInt("player_1_score");
                        player_2_score = player_scores.getInt("player_2_score");
                        player_3_score = player_scores.getInt("player_3_score");
                        player_4_score = player_scores.getInt("player_4_score");

                        UDPObject.resetScoreEvent();
                    }
                }
            }
        }

        //hit one_two ***************************************************************************************************
        //***************************************************************************************************************

        else if (ball_x + BALL_HEIGHT > pane_x - PADDLE_WIDTH && (ball_y + BALL_HEIGHT > paddleOneOppY && ball_y < paddleOneOppY + PADDLE_HEIGHT)) {

            ball_vel_x *= -1;
            ball_x = pane_x - BALL_HEIGHT - PADDLE_WIDTH;
            BALL_SPEEDX *= (1 + eRestitution * (Math.abs(paddleOneOppY + PADDLE_HEIGHT / 2 - ball_y)));

            if (OneTwoStop) {

            } else if (positiveYOne_two == positiveBallY) {
                BALL_SPEEDY += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleOneOppY + PADDLE_HEIGHT / 2 - ball_y));
                ball_vel_y *= 1;
            } else {
                BALL_SPEEDY += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleOneOppY + PADDLE_HEIGHT / 2 - ball_y));
                ball_vel_y *= -1;
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 3 || playerIndex ==1) {
                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                }
            }

            // right wall ***********************************************************************************************
            //************************************************************************************************************
        } else if (ball_x + BALL_HEIGHT > pane_x) {
            //hit right wall
            ball_vel_x *= -1;
            if (gameMode.equals("Single")) {
                player_4_score++;
                if (player_4_score >= MAX_SCORE) {
                    //Do Checks
                }
            } else {
                player_1_score++;
                if (player_1_score >= MAX_SCORE) {
                    resetBall("Player 2 Wins!");
                    resetScore();
                }
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 3 || playerIndex == 1) {

                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                    UDPObject.sendPlayerScore(player_1_score, player_2_score, player_3_score, player_4_score);

                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    JSONObject player_scores = UDPObject.getBallPosition();

                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                    if (player_scores != null) {
                        player_1_score = player_scores.getInt("player_1_score");
                        player_2_score = player_scores.getInt("player_2_score");
                        player_3_score = player_scores.getInt("player_3_score");
                        player_4_score = player_scores.getInt("player_4_score");

                        UDPObject.resetScoreEvent();
                    }
                }
            }
        }


        //hit top wall *******************************************************************************************************
        //*******************************************************************************************************************
        else if (ball_y < 0) {
            ball_vel_y *= -1;
            if (gameMode.equals("Single")) {
                player_3_score++;
                if (player_3_score >= MAX_SCORE) {
                    //DO Checks
                }
            } else {
                player_2_score++;
                if (player_2_score >= MAX_SCORE) {
                    resetBall("Player 1 Wins!");
                    resetScore();
                }
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 2 || playerIndex == 0) {

                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                    UDPObject.sendPlayerScore(player_1_score, player_2_score, player_3_score, player_4_score);

                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    JSONObject player_scores = UDPObject.getBallPosition();

                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                    if (player_scores != null) {
                        player_1_score = player_scores.getInt("player_1_score");
                        player_2_score = player_scores.getInt("player_2_score");
                        player_3_score = player_scores.getInt("player_3_score");
                        player_4_score = player_scores.getInt("player_4_score");

                        UDPObject.resetScoreEvent();
                    }
                }
            }
        }
        //hit paddle two_one **********************************************************************************************
        //********************************************************************************************************************
        else if (ball_y < PADDLE_WIDTH && (ball_x + BALL_HEIGHT > paddleTwoX && ball_x + BALL_HEIGHT < paddleTwoX + PADDLE_HEIGHT)) {

            ball_vel_y *= -1;
            ball_y = PADDLE_WIDTH;
            // ball_x can be behind paddle. put back out in front.
            //ball_x = pane_x - BALL_WIDTH - PADDLE_WIDTH;
            BALL_SPEEDY *= (1 + eRestitution * Math.abs(paddleTwoX + PADDLE_HEIGHT / 2 - ball_x));

            if (TwoOneStop) {

            } else if (positiveXTwo_one == positiveBallX) {
                BALL_SPEEDX += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleTwoX + PADDLE_HEIGHT / 2 - ball_x));
                ball_vel_x *= 1;
            } else {
                BALL_SPEEDX += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleTwoX + PADDLE_HEIGHT / 2 - ball_x));
                ball_vel_x *= -1;
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 2 || playerIndex == 0) {
                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                }
            }
        }

        //paddle two_two*****************************************************************************************************
        //********************************************************************************************************************

        else if (ball_y + BALL_HEIGHT > pane_y - PADDLE_WIDTH && (ball_x + BALL_HEIGHT > paddleTwoOppX && ball_x + BALL_HEIGHT < paddleTwoOppX + PADDLE_HEIGHT)) {

            ball_vel_y *= -1;
            ball_y = pane_y - BALL_HEIGHT - PADDLE_WIDTH;
            BALL_SPEEDY *= (1 + eRestitution * Math.abs(paddleTwoOppX + PADDLE_HEIGHT / 2 - ball_x));

            if (TwoTwoStop) {

            } else if (positiveXTwo_two == positiveBallX) {
                BALL_SPEEDX += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleTwoOppX + PADDLE_HEIGHT / 2 - ball_x));
                ball_vel_x *= 1;
            } else {
                BALL_SPEEDX += (PADDLE_SPEED) * (aRestitution * Math.abs(paddleTwoOppX + PADDLE_HEIGHT / 2 - ball_x));
                ball_vel_x *= -1;
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 0) {
                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                }
            }
        }

        //hit bottom wall ***********************************************************************************************
        //*****************************************************************************************************************

        else if (ball_y + BALL_HEIGHT > pane_y) {
            ball_vel_y *= -1;
            if (gameMode.equals("Single")) {
                player_1_score++;
                if (player_1_score >= MAX_SCORE) {
                    //Do Checks
                }
            } else {
                player_2_score++;
                if (player_2_score >= MAX_SCORE) {
                    resetBall("Player 1 Wins!");
                    resetScore();
                }
            }

            if (gameMode.equals("Multiplayer")) {
                if (playerIndex == 0) {

                    UDPObject.sendBallInfo(ball_x, ball_y, BALL_SPEEDX, BALL_SPEEDY, ball_vel_x, ball_vel_y, 1);
                    UDPObject.sendPlayerScore(player_1_score, player_2_score, player_3_score, player_4_score);

                } else {

                    JSONObject ballPosition = UDPObject.getBallPosition();
                    JSONObject player_scores = UDPObject.getBallPosition();

                    if (ballPosition != null) {
                        ball_x = ballPosition.getDouble("ball_x");
                        ball_y = ballPosition.getDouble("ball_y");
                        BALL_SPEEDX = ballPosition.getDouble("BALL_SPEEDX");
                        BALL_SPEEDY = ballPosition.getDouble("BALL_SPEEDY");
                        ball_vel_x = ballPosition.getDouble("vel_x");
                        ball_vel_y = ballPosition.getDouble("vel_y");

                        UDPObject.resetBallPosition();
                    }
                    if (player_scores != null) {
                        player_1_score = player_scores.getInt("player_1_score");
                        player_2_score = player_scores.getInt("player_2_score");
                        player_3_score = player_scores.getInt("player_3_score");
                        player_4_score = player_scores.getInt("player_4_score");

                        UDPObject.resetScoreEvent();
                    }
                }
            }
        }
        else {
            ball_x += ball_vel_x * BALL_SPEEDX;
            ball_y += ball_vel_y * BALL_SPEEDY;
        }
            //**************************************************END********************************************************//
            //************************************************************************************************//


//        if (gameMode.equals("Multiplayer")) {
//
//        } else {
//
//        }
//        ball_x += ball_vel_x * BALL_SPEEDX;
//        ball_y += ball_vel_y * BALL_SPEEDY;

            //send ball position and velocity and player scores
//
//        if(UDPObject!=null) {
//            boolean virtualHost = UDPObject.getVirtualHost();
//
//            if (virtualHost) {
//                UDPObject.sendBallInfo(ball_x, ball_y, ball_vel_x, ball_vel_y, 1);
//                UDPObject.sendPlayerScore(player_1_score, player_2_score, player_3_score, player_4_score);
//            } else {
//                //            checkVirtualHost(UDPObject);
//                JSONObject player_scores = UDPObject.getPlayerScore();
//                JSONObject ballPosition = UDPObject.getBallPosition();
//                if (ballPosition != null) {
//                    ball_x = ballPosition.getDouble("ball_x");
//                    ball_y = ballPosition.getDouble("ball_y");
//                    ball_vel_x = ballPosition.getDouble("vel_x");
//                    ball_vel_y = ballPosition.getDouble("vel_y");
//                    UDPObject.resetBallPosition();
//                }
//                if (player_scores!=null){
//                    player_1_score = player_scores.getInt("player_1_score");
//                    player_2_score = player_scores.getInt("player_2_score");
//                    player_3_score = player_scores.getInt("player_3_score");
//                    player_4_score = player_scores.getInt("player_4_score");
//                    UDPObject.resetScoreEvent();
//                }
//            }
//        }
        repaint();


    }

    public synchronized void checkVirtualHost(UDP udpObject) {
        ArrayList<Machine> arraylist = udpObject.getPlayerlist();
        Machine machine = arraylist.get(0);
        InetAddress inet = null;
        try {
            inet = InetAddress.getByName(machine.getIp());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        int port = machine.getPort();

    }

    public void InputApply(String mode) {
        //single player game mode
        if (mode.equals("Single")) {
            if (paddles.Left == true) {

                positiveXTwo_two = false;
                TwoTwoStop = false;

                if (paddleTwoOppX > 0) {
                    paddleTwoOppX -= PADDLE_SPEED;
                }
            } else if (paddles.Right == true) {

                positiveXTwo_two = true;
                TwoTwoStop = false;

                if (paddleTwoOppX < pane_x - PADDLE_HEIGHT) {
                    paddleTwoOppX += PADDLE_SPEED;
                }
            } else {
                TwoTwoStop = true;
            }


            //paddleOne_one AI
            if (paddleOneY + PADDLE_HEIGHT / 2 > ball_y) {
                if (paddleOneY > 0) {
                    paddleOneY -= PADDLE_SPEED_AI;
                }
                positiveYOne_one = false;
                OneOneStop = false;
            } else if (paddleOneY + PADDLE_HEIGHT / 2 == ball_y) {
                OneOneStop = true;
            } else {
                if (paddleOneY < pane_y - PADDLE_HEIGHT) {
                    paddleOneY += PADDLE_SPEED_AI;
                }
                positiveYOne_one = true;
                OneOneStop = false;
            }

            //paddleOne_two AI
            if (paddleOneOppY + PADDLE_HEIGHT / 2 > ball_y) {
                if (paddleOneOppY > 0) {
                    paddleOneOppY -= PADDLE_SPEED_AI;
                }
                positiveYOne_two = false;
                OneTwoStop = false;
            } else if (paddleOneOppY + PADDLE_HEIGHT / 2 == ball_y) {
                OneTwoStop = true;
            } else {
                if (paddleOneOppY < pane_y - PADDLE_HEIGHT) {
                    paddleOneOppY += PADDLE_SPEED_AI;
                }
                positiveYOne_two = true;
                OneTwoStop = false;
            }

            //paddleTwo_one AI
            if (paddleTwoX + PADDLE_HEIGHT / 2 > ball_x) {
                if (paddleTwoX > 0) {
                    paddleTwoX -= PADDLE_SPEED_AI;
                }
                positiveXTwo_one = false;
                TwoOneStop = false;
            } else if (paddleTwoX + PADDLE_HEIGHT / 2 == ball_x) {
                TwoOneStop = true;
            } else {
                if (paddleTwoX < pane_x - PADDLE_HEIGHT) {
                    paddleTwoX += PADDLE_SPEED_AI;
                }
                positiveXTwo_one = true;
                TwoOneStop = false;
            }
        }
        //game mode two player
        else {
            if ((paddles.S == true) && (paddles.playerIndex == 0)) {

                positiveYOne_one = true;
                positiveYOne_two = true;
                OneOneStop = false;
                OneTwoStop = false;

                if (paddleOneY < pane_y - PADDLE_HEIGHT) {
                    paddleOneY += PADDLE_SPEED;
                    paddleOneOppY += PADDLE_SPEED;
                }
            } else if ((paddles.W == true) && (paddles.playerIndex == 0)) {

                positiveYOne_one = false;
                positiveYOne_two = false;
                OneOneStop = false;
                OneTwoStop = false;

                if (paddleOneY > 0) {
                    paddleOneY -= PADDLE_SPEED;
                    paddleOneOppY -= PADDLE_SPEED;
                }
            } else {
                OneOneStop = true;
                OneTwoStop = true;
            }

            if (((paddles.Left == true) && (paddles.playerIndex == 1))) {

                positiveXTwo_one = false;
                positiveXTwo_two = false;
                TwoOneStop = false;
                TwoTwoStop = false;

                if (paddleTwoX > 0) {
                    paddleTwoX -= PADDLE_SPEED;
                    paddleTwoOppX -= PADDLE_SPEED;
                }
            } else if ((paddles.Right == true) && (paddles.playerIndex == 1)) {

                positiveXTwo_one = true;
                positiveXTwo_two = true;
                TwoOneStop = false;
                TwoTwoStop = false;

                if (paddleTwoX < pane_x - PADDLE_HEIGHT) {
                    paddleTwoX += PADDLE_SPEED;
                    paddleTwoOppX += PADDLE_SPEED;
                }
            } else {
                TwoOneStop = true;
                TwoTwoStop = true;
            }
        }
    }
}