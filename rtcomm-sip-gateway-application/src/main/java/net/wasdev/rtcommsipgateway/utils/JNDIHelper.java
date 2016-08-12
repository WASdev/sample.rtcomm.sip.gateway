package net.wasdev.rtcommsipgateway.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import net.wasdev.rtcommsipgateway.servlets.WebRTCGatewayServlet;
/**
 * This will Override a property file with JNDI values if they currently exist.
 * @author jfmartin
 *
 */
public class JNDIHelper {

    private static Logger log = Logger.getLogger(JNDIHelper.class.getName());

    public JNDIHelper(){}
    
    public String loadJNDIValue(String key){
        String value = null;
        try {
            InitialContext context = new InitialContext();

            value = "" + (Object) context.lookup(key);
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            log.fine(String.format("Failed to load JNDI Value for key: %s", key ));
        }
        
        return value;
        
    }

    public void overrideProperties(HashMap<String, String> jndiToPropertyKeys, Properties driverProperties) {
        // TODO Auto-generated method stub
        
       Iterator it = jndiToPropertyKeys.entrySet().iterator();
        
        while(it.hasNext()){
            HashMap.Entry<String,String> pair = (Map.Entry<String, String>)it.next();
            
            String jndiName = pair.getKey();
            String propertyKey = pair.getValue();
            
            String overridenValue = loadJNDIValue(jndiName);
            
            if(overridenValue != null){
                driverProperties.put(propertyKey, overridenValue);
                log.fine(String.format("Overrode Property: %s with %s", propertyKey, overridenValue));
            }
        }
        
    }
    
}
