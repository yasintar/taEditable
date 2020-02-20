/*
 * ColorCodeBezier.java
 *
 * Created on June 6, 2007, 1:43 PM
 *
 * @author Oliver
 * @version 1.0
 */

package sidnet.stack.users.csgp.colorprofile;

import sidnet.core.interfaces.ColorProfile;
import java.awt.Color;

public class CSGPColorProfile extends ColorProfile{
    public static final String DATA       = "DATA";    
    public static final String TRANSMIT   = "TRANSMIT";
    public static final String RECEIVE    = "RECEIVE";   
    public static final String SINK       = "SINK";   
    public static final String SOURCE     = "SOURCE";   
    public static final String RESPONDENT = "RESPONDENT";   
    public static final String SENSE      = "SENSE";
    public static final String CH         = "CH";
    public static final String LIMA       = "LIMA";
    public static final String DUA       = "DUA";
    public static final String TRANSMIT2 = "TRANSMIT2";
    public static final String SENSE2 = "SENSE2";
    public static final String TRANSMIT5 = "TRANSMIT5";
    public static final String SATU = "SATU";
    public static final String TIGA = "TIGA";
    public static final String EMPAT = "EMPAT";
    public static final String ENAM = "ENAM";
    public static final String TUJUH = "TUJUH";
    public static final String SENSE5 = "SENSE5";
   public static final String DELAPAN = "DELAPAN";
   public static final String SEMBILAN = "SEMBILAN";
   public static final String SEPULUH = "SEPULUH";
   
    
    public CSGPColorProfile()
    {
        super(); // YOU MUST CALL SUPER()
                                /* tag     , inner(body)  , outer(contour)*/
        register(new ColorBundle(DATA      , Color.ORANGE , null        ));

        register(new ColorBundle(TRANSMIT  , Color.BLUE   , Color.GREEN   ));
        register(new ColorBundle(RECEIVE   , Color.RED  , Color.YELLOW        ));

        register(new ColorBundle(SINK      , Color.WHITE , Color.RED));
        register(new ColorBundle(SOURCE    , Color.cyan , Color.RED));
        
        register(new ColorBundle(RESPONDENT, Color.PINK, Color.CYAN  ));     
        register(new ColorBundle(SENSE     , Color.YELLOW   , null        )); 
        
        register(new ColorBundle(DUA, Color.YELLOW, Color.darkGray));
        register(new ColorBundle(LIMA, Color.GREEN, Color.darkGray));
        register(new ColorBundle(SATU, Color.BLUE, Color.darkGray));
        register(new ColorBundle(TIGA, Color.CYAN, Color.darkGray));
        register(new ColorBundle(EMPAT, Color.MAGENTA, Color.darkGray));
        register(new ColorBundle(ENAM, Color.ORANGE, Color.darkGray));
        register(new ColorBundle(TUJUH, Color.PINK, Color.darkGray));
        register(new ColorBundle(DELAPAN, Color.RED, Color.darkGray));
        register(new ColorBundle(SEMBILAN, Color.WHITE, Color.GREEN));
        register(new ColorBundle(SEPULUH, Color.getHSBColor(21, 1, 0), Color.RED));
        
        
        register(new ColorBundle(CH, Color.WHITE, Color.YELLOW  ));    
        
        register(new ColorBundle(TRANSMIT2, Color.ORANGE, Color.WHITE));
        register(new ColorBundle(SENSE2, Color.white, Color.RED));
        register(new ColorBundle(TRANSMIT5, Color.ORANGE, Color.WHITE));
        register(new ColorBundle(SENSE5, Color.white, Color.RED));
  
    }
}
