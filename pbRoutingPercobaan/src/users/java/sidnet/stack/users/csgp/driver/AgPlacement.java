/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.driver;

import jist.swans.field.Placement;
import jist.swans.misc.Location;
import jist.swans.Constants;

/**
 *
 * @author invictus
 */
public class AgPlacement implements Placement {
/** field dimensions. */
    private float fieldx, fieldy;

    private int totalDepth;
    private float depthNow;
    private float widthNow;
    private float heightNow;
    private int   leafDepthNow;

    private float distance_height;
    private float distance_width;

    private int maxLeafonDepthNow;
    private int nNodes;
    private long i;

    public AgPlacement(Location loc, int nNodes)
    {
      init(loc.getX(), loc.getY(), nNodes);
    }

    public int getSize()
    {
        return UNBOUNDED;
    }

    private int logbase2(int x) {
        double tX = Math.log(x) / Math.log(2);
        return (int)tX;
    }

    private void init(float fieldx, float fieldy, int nNodes)
    {
      this.fieldx = fieldx;
      this.fieldy = fieldy;

      this.nNodes = nNodes;
      this.totalDepth = logbase2(nNodes) + 1;
      this.distance_height = this.fieldy / (this.totalDepth);


      i = 0;
      depthNow = 0;
      heightNow = 0;
      widthNow = 0;
      this.maxLeafonDepthNow =(int)Math.pow(2, depthNow);
      this.distance_width = this.fieldx / (this.maxLeafonDepthNow + 1);
    }

    //////////////////////////////////////////////////
    // Placement interface
    //

    /** {@inheritDoc} */
    public Location getNextLocation()
    {
        float x,y;
        if (leafDepthNow == this.maxLeafonDepthNow) {

            depthNow++;
            heightNow = heightNow + this.distance_height;
            this.maxLeafonDepthNow =(int)Math.pow(2, depthNow);
            this.distance_width = this.fieldx / (this.maxLeafonDepthNow + 1);

            widthNow = distance_width;

            x = widthNow;
            y = heightNow;

            leafDepthNow = 1;
        } else {
            widthNow = widthNow + distance_width;

            x = widthNow;
            y = heightNow;
            leafDepthNow++;
        }

      Location l = new Location.Location2D(x, y);
      i++;
      return l;

    }
}
