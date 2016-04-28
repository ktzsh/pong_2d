import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Created by Kshitiz Sharma on 28-Apr-16.
 */
public class Ball {

    int BALL_HEIGHT;
    int BALL_WIDTH;

    double BALL_SPEEDX;
    double BALL_SPEEDY;

    double ball_velocity_x;
    double ball_velocity_y;

    int id;

    double ball_x;
    double ball_y;

    public Ball(int id, int BALL_HEIGHT, int BALL_WIDTH) {
        this.id = id;
        this.BALL_HEIGHT = BALL_HEIGHT;
        this.BALL_WIDTH = BALL_WIDTH;
    }

    public void drawBall(Graphics2D g2d) {
        Ellipse2D.Double ball = new Ellipse2D.Double(ball_x, ball_y, BALL_WIDTH, BALL_HEIGHT);
        g2d.fill(ball);
        g2d.draw(ball);
    }

    public void updateBallPositions(double ball_x, double ball_y) {
        this.ball_x = ball_x;
        this.ball_y = ball_y;
    }

    public void updateBallSpeed(double BALL_SPEEDX, double BALL_SPEEDY) {
        this.BALL_SPEEDX = BALL_SPEEDX;
        this.BALL_SPEEDY = BALL_SPEEDY;
    }

    public void updateBallVelocity(double ball_velocity_x, double ball_velocity_y) {
        this.ball_velocity_x = ball_velocity_x;
        this.ball_velocity_y = ball_velocity_y;
    }

    public double getBallSpeedX(){
        return BALL_SPEEDX;
    }
    public double getBallSpeedY(){
        return BALL_SPEEDY;
    }
    public double getBallVelocityX(){
        return ball_velocity_x;
    }
    public double getBallVelocityY(){
        return ball_velocity_y;
    }
}