/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp.driver;
//import sidnet.stack.users.sample_p2p.driver.*;
import java.util.Scanner;
import java.lang.Math;
import sidnet.stack.users.csgp.driver.SwingRoutingDriver;
/**
 *
 * @author Dewi
 */
public class FindNearestPoints {
    public static void main(String[] args)
    {
        Scanner scan = new Scanner(System.in);

        //System.out.print("Enter the number of points: ");
        final int numOfPoints = SwingRoutingDriver.nodes;

        double[][] points = new double[numOfPoints][2];
        double shortestDistance=0;
        double distance=0;
        String closestPoint1="";
        String closestPoint2="";

        //enter x,y coords into the ix2 table points[][]
        for (int i=0; i<numOfPoints; i++)
        {
                //System.out.print("Enter point x" + i + ": ");
                points[i][0] = SwingRoutingDriver.tempLocX[i];

                //System.out.print("Enter point y" + i + ": ");
                points[i][1] = SwingRoutingDriver.tempLocY[i];
        }

        //get the distance between the point in the ith row and the (m+1)th row
        //and check if it's shorter than the distance between 0th and 1st
        for (int i=0; i<numOfPoints; i++)
        {
            //use m=i rather than 0 to avoid duplicate computations
            for (int m=i; m<numOfPoints-1;m++)
            {
                double dx = points[i][0] - points[m+1][0];
                double dy = points[i][1] - points[m+1][1];
                distance = Math.sqrt(dx*dx + dy*dy);

                //set shortestDistance and closestPoints to the first iteration
                if (m == 0 && i == 0)
                {
                    shortestDistance = distance;
                    closestPoint1 = "(" + points[0][0] + "," + points[0][1] + ")";
                    closestPoint2 = "(" + points[1][0] + "," + points[1][1] + ")";
                }
                //then check if any further iterations have shorter distances
                else if (distance < shortestDistance)
                {
                    shortestDistance = distance;
                    closestPoint1 = "(" + points[i][0] + "," + points[i][1] + ")";
                    closestPoint2 = "(" + points[m+1][0] + "," + points[m+1][1] + ")";
                }
            }
        }
        System.out.println("The shortest distance is: " + shortestDistance);
        System.out.println("The closest points are " + closestPoint1 + " and " + closestPoint2);
    }

}
