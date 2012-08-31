/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package appwebserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author rafaelperazzo
 */
public class Testes {
    
    public static void main(String[] args) {
        String s1 = "Visitas ao servidor:";
        System.out.println(s1.substring(s1.indexOf("itas"), s1.indexOf("or")));
        System.out.println(s1.indexOf("itas"));
        System.out.println(s1.indexOf("or"));
        final String cmd = "php -B <? printf(\"teste\"); printf(\"teste2\");?>";

        int pid = -1;

        try {
            // Run ls command
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedInputStream buffer = new BufferedInputStream( process.getInputStream() );
            BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
            String line = null;     
            while ( ( line = commandOutput.readLine() ) != null ) {
                System.out.println( "command output: " + line );
             }//end while
             commandOutput.close(); 
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
        }
    }
    
}
