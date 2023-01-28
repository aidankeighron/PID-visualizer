import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.BasicStroke;

public class PIDVisual {
    
    // Max Vel = 15 Max Acc = 0.3
    // P = 2 I = 0.1 D = 0.5
    
    private static JFrame frame = new JFrame();
    private static JSlider slider = new JSlider(0, 360);
    private static JLabel text = new JLabel();

    public static JTextField pConfig = new JTextField(5);
    public static JTextField iConfig = new JTextField(5);
    public static JTextField dConfig = new JTextField(5);
    
    public static void main(String[] args) {
        PIDVisual s = new PIDVisual();
        s.createAndShowGUI();
    }
    
    private void createAndShowGUI() {
        PIDBox pid = new PIDBox();

        Box pidConfig = Box.createHorizontalBox();
        pidConfig.add(new JLabel("kP: "));
        pidConfig.add(pConfig);
        pidConfig.add(new JLabel("kI: "));
        pidConfig.add(iConfig);
        pidConfig.add(new JLabel("kD: "));
        pidConfig.add(dConfig);

        Box bottom = Box.createVerticalBox();
        slider.setPaintTicks(false);
        slider.setValue(0);
        bottom.add(slider);
        bottom.add(text);
        bottom.add(pidConfig);
        bottom.setSize(400, 100);

        frame.setLayout(new BorderLayout());
        frame.add(pid);
        frame.add(bottom, BorderLayout.SOUTH);
        
        frame.setTitle("PID Visualizer");;
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(pid, 0, 50, TimeUnit.MILLISECONDS);
    }
    
    public static class PIDBox extends JPanel implements Runnable {
        
        private static double currentPosition = 0.0;
        private static double slowDown = 10;
        private static double maxVelocity = 15;
        private static double maxAcceleration = 0.3;
        private static double lastMotorValue = 0;
        private static PIDController pidController = new PIDController(0.9, 0.015, 0);
        
        public PIDBox() {
            pConfig.setText("0.9");
            iConfig.setText("0.015");
            dConfig.setText("0");
            super.setSize(400, 400);
            super.repaint();
            super.setVisible(true);
            pidController.setTolerance(2);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawPID(g);
            drawGraph(g);
        }

        // Graph
        private ArrayList<Double> currentPoints = new ArrayList<>();
        private ArrayList<Double>wantedPoints = new ArrayList<>();
        private int graphStartX = 300;
        private int graphStartY = 400;
        private int graphLength = 350;
        private int graphWidth = 450;
        private int graphRange = 80;

        //PID
        private int centerX = 150;
        private int centerY = 250;

        private void drawGraph(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            int hash = 2;
            Point start = new Point(graphStartX, graphStartY);
            int yMulti = graphLength/graphRange;
            g2.drawLine(start.x, start.y, start.x + graphWidth, start.y);
            g2.drawLine(start.x, start.y, start.x, start.y - graphLength);
            // create hatch marks for y axis. 
            for (int i = 0; i < 10; i++) {
                int x0 = start.x + hash;
                int x1 = start.x - hash;
                int y0 = start.y - ((i + 1) * (graphLength/10));
                int y1 = y0;
                g2.drawLine(x0, y0, x1, y1);
            }
            // and for x axis
            for (int i = 0; i < currentPoints.size()-1; i++) {
                int x0 = (int)Math.round(start.x + ((i + 1) * (graphWidth/Double.valueOf(currentPoints.size()))));
                int x1 = x0;
                int y0 = start.y + hash;
                int y1 = start.y - hash;
                if (x0 > (start.x + graphWidth)) {
                    break;
                }
                g2.drawLine(x0, y0, x1, y1);
            }
            g2.setColor(Color.RED);
            for (int i = 0; i < currentPoints.size()-1; i++) {
                int x1 = (int)Math.round(start.x + ((i ) * (graphWidth/Double.valueOf(currentPoints.size()))));
                int y1 = (int)Math.round(start.y - (currentPoints.get(i) * yMulti));
                int x2 = (int)Math.round(start.x + ((i + 1) * (graphWidth/Double.valueOf(currentPoints.size()))));
                int y2 = (int)Math.round(start.y - (currentPoints.get(i + 1) * yMulti));
                g2.drawLine(x1, y1, x2, y2);
            }
            g2.setColor(Color.BLUE);
            for (int i = 0; i < currentPoints.size()-1; i++) {
                int x1 = (int)Math.round(start.x + ((i ) * (graphWidth/Double.valueOf(wantedPoints.size()))));
                int y1 = (int)Math.round(start.y - (wantedPoints.get(i) * yMulti));
                int x2 = (int)Math.round(start.x + ((i + 1) * (graphWidth/Double.valueOf(wantedPoints.size()))));
                int y2 = (int)Math.round(start.y - (wantedPoints.get(i + 1) * yMulti));
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        private void drawPID(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);

            double angle1 = slider.getValue() - 90.0;
            double endX1 = centerX + Math.cos(Math.toRadians(angle1)) * 100;
            double endY1 = centerY + Math.sin(Math.toRadians(angle1)) * 100;
            g2.setColor(Color.GREEN);
            g2.drawLine(centerX, centerY, (int)endX1, (int)endY1);

            double endX2 = centerX + Math.cos(Math.toRadians(currentPosition - 90)) * 100;
            double endY2 = centerY + Math.sin(Math.toRadians(currentPosition - 90)) * 100;
            g2.setColor(Color.RED);
            g2.drawLine(centerX, centerY, (int)endX2, (int)endY2);

            double endX3 = centerX + Math.cos(Math.toRadians(slider.getValue() - 90)) * 100;
            double endY3 = centerY + Math.sin(Math.toRadians(slider.getValue() - 90)) * 100;
            g2.setColor(Color.BLUE);
            g2.drawLine(centerX, centerY, (int)endX3, (int)endY3);
        }
        
        private void calculatePID(double wantedPosition)
        {
            try {
                double kp = Double.parseDouble(pConfig.getText());
                double ki = Double.parseDouble(iConfig.getText());
                double kd = Double.parseDouble(dConfig.getText());
                pidController.setPID(kp, ki, kd);
            }
            catch (Exception e) { }

            double distance = wantedPosition - currentPosition;

            // PID
            double motorValue = clamp(pidController.calculate(0, distance) / slowDown, -maxVelocity, maxVelocity);
            if (pidController.atSetpoint()) motorValue = 0;
            if (Math.abs(motorValue - lastMotorValue) > maxAcceleration) {
                motorValue = lastMotorValue + maxAcceleration * Math.signum(motorValue);
            }
            
            text.setText("Value: " + (int)currentPosition +" | Output: " + (int)currentPosition + " | Distance: " + (int)distance + " | Speed: " + ((int)(motorValue*100))/100.0 + " | Acceleration: " + (motorValue - lastMotorValue));
            
            currentPosition += motorValue;
            currentPoints.add(currentPosition);
            if (currentPoints.size() > graphRange) {
                currentPoints.remove(0);
            }
            wantedPoints.add(wantedPosition);
            if (wantedPoints.size() > graphRange) {
                wantedPoints.remove(0);
            }
            lastMotorValue = motorValue;
        }

        private double clamp(double value, double low, double high) {
            return Math.max(low, Math.min(high, value));
        }

        @Override
        public void run() {
            calculatePID(slider.getValue());
            repaint();
        }
    }   
}