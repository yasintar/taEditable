/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.csgp.app;

import java.io.FileWriter;

/**
 *
 * @author MMC
 */
public class WriteCSV {
    
     FileWriter writer;
     private static final String COMMA_DELIMITER = ",";
     private static final String NEW_LINE_SEPARATOR = "\n";
     public void writeCsv(String fileName, String value){
         
          try{
                writer = new FileWriter(fileName);
                writer.append(value);
                writer.append(NEW_LINE_SEPARATOR);
                            
               }catch(Exception e){
                   e.printStackTrace();
               }
       finally{
           try{
                    writer.flush();
                    writer.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
       }
          
      }
}
