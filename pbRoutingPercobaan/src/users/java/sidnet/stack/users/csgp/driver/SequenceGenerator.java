/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.csgp.driver;

/**
 *
 * @author invictus
 */
public class SequenceGenerator {
    private long sequenceNumber;

    public SequenceGenerator() {
        this.sequenceNumber = (long)0;
    }

    public long getandincrement() {
        long x = sequenceNumber;
        sequenceNumber++;
        return x;
    }
}
