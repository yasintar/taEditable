/*
 * ColorCodeBezier.java
 *
 * Created on June 6, 2007, 1:43 PM
 *
 * @author Oliver
 * @version 1.0
 */

package sidnet.colorprofiles;

import sidnet.core.interfaces.ColorProfile;
import java.awt.Color;

public class ColorProfileGeneric extends ColorProfile{
    public static final String DATA       = "DATA";    
    public static final String TRANSMIT   = "TRANSMIT";
    public static final String RECEIVE    = "RECEIVE";   
    public static final String SINK       = "SINK";   
    public static final String SOURCE     = "SOURCE";   
    public static final String RESPONDENT = "RESPONDENT";   
    public static final String SENSE      = "SENSE";
    
    public ColorProfileGeneric()
    {
        super(); // YOU MUST CALL SUPER()
                                /* tag     , inner(body)  , outer(contour)*/
        register(new ColorBundle(DATA      , Color.ORANGE , null        ));
        register(new ColorBundle(TRANSMIT  , null         ,Color.GREEN   ));
        register(new ColorBundle(RECEIVE   , Color.GREEN  , null        ));
        register(new ColorBundle(SINK      , Color.YELLOW , Color.YELLOW));
        register(new ColorBundle(SOURCE    , Color.YELLOW , Color.YELLOW));
        register(new ColorBundle(RESPONDENT, Color.MAGENTA, Color.CYAN  ));     
        register(new ColorBundle(SENSE     , Color.BLUE   , null        ));  
    }
}
