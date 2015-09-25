/*
 * GenericRecord.java
 *
 * Created on May 10, 2002, 2:20 PM
 */

package org.csstudio.mps.sns.tools.data;

import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;


/**
 * GenericRecord is the default record class for DataTable.  This class can be used
 * directly for the records or a subclass of GenericRecord may be used for convenience.
 * GenericRecord stores its data as key/value pairs.
 * Note that GenericRecord is not thread safe.  This is due to the fact that DataTable is not thread safe.
 *
 * @author  tap
 */
public class GenericRecord implements KeyedRecord, DataListener {
    protected DataTable table;
    protected Map attributeTable;

	
    /** Creates new GenericRecord */
    public GenericRecord( final DataTable aTable ) {
        table = aTable;
        attributeTable = new HashMap();
    }

	
	/**
	 * Get the keys used in this record.
	 * @return The keys used in this record.
	 */
    public Set keys() {
		synchronized ( attributeTable ) {
			return attributeTable.keySet();			
		}
    }
    
    
	/**
	 * Get the collection of values held in this record.
	 * @return The collection of values held in this record.
	 */
    public Collection values() {
		synchronized ( attributeTable ) {
			return attributeTable.values();			
		}
    }
    
    
	/**
	 * Get the value associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as an Object.
	 */
    public Object valueForKey( final String key ) {
		synchronized ( attributeTable ) {
			return attributeTable.get(key);			
		}
    }
    
    
	/**
	 * Set the value to associate with the specified key.  If the value associated with a primary key is 
	 * changed we must be careful to force the table to re-index the record.
	 * @param value The new value to associate with the specified key.
	 * @param key The key for which to associated the new value.
	 */
    public void setValueForKey( final Object value, final String key) {
		final Object oldValue = valueForKey(key);
		synchronized ( attributeTable ) {
			attributeTable.put(key, value);			
		}
		if( table != null ) {
			table.reIndex( this, key, oldValue );
		}
    }
    
    
	/**
	 * Convenience method to get the value cast as a number associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as an Number.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a Number. 
	 */
    public Number numberForKey( final String key ) {
        return (Number)valueForKey(key);
    }
    
    
	/**
	 * Convenience method to get the value as a boolean associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as an boolean.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a Boolean. 
	 */
    public boolean booleanValueForKey( final String key ) {
        Boolean booleanObject = (Boolean)valueForKey(key);
        return booleanObject.booleanValue();
    }
    
    
	/**
	 * Set the boolean value to associate with the specified key.
	 * @param value The new boolean value to associate with the specified key.
	 * @param key The key for which to associated the new value.
	 */
    public void setValueForKey( final boolean value, final String key ) {
        setValueForKey(new Boolean(value), key);
    }
    
    
	/**
	 * Convenience method to get the value as an int associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as an int.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a Number. 
	 */
    public int intValueForKey( final String key ) {
        Number number = numberForKey(key);
        return number.intValue();
    }
    
    
	/**
	 * Set the int value to associate with the specified key.
	 * @param value The new int value to associate with the specified key.
	 * @param key The key for which to associated the new value.
	 */
    public void setValueForKey( final int value, final String key ) {
        setValueForKey( new Integer(value), key );
    }
    
    
	/**
	 * Convenience method to get the value as a long associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as a long.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a Number. 
	 */
    public long longValueForKey( final String key ) {
        Number number = numberForKey(key);
        return number.intValue();
    }
    
    
	/**
	 * Set the long value to associate with the specified key.
	 * @param value The new long value to associate with the specified key.
	 * @param key The key for which to associated the new value.
	 */
    public void setValueForKey( final long value, final String key ) {
        setValueForKey(new Long(value), key);
    }
    
    
	/**
	 * Convenience method to get the value as a double associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as a double.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a Number. 
	 */
    public double doubleValueForKey( final String key ) {
        Number number = numberForKey(key);
        return number.doubleValue();
    }
    
    
	/**
	 * Set the double value to associate with the specified key.
	 * @param value The new double value to associate with the specified key.
	 * @param key The key for which to associated the new value.
	 */
    public void setValueForKey( final double value, final String key ) {
        setValueForKey( new Double(value), key );
    }
    
    
	/**
	 * Convenience method to get the value as a String associated with the specified key.
	 * @param key The key for which to get the associated value.
	 * @return The value as an String.
	 * @throws java.lang.ClassCastException if the value cannot be cast as a String. 
	 */
    public String stringValueForKey( final String key ) {
        return (String)valueForKey(key);
    }
    // --- end value conversions
    
    
    /** 
	 * Overrides toString() to show key/value pairs.
	 * @return The string representation of the record.
	 */
    public String toString() {
        return attributeTable.toString();
    }
    
    
    /** 
     * dataLabel() provides the name used to identify the class in an 
     * external data source.
     * @return a tag that identifies the receiver's type
     */
    public String dataLabel() {
        return "record";
    }


    /**
     * Update the data based on the information provided by the data provider.
     * @param adaptor The adaptor from which to update the data
     */
    public void update( final DataAdaptor adaptor ) throws ParseException {
        Collection attributes = table.attributes();
        Iterator attributeIter = attributes.iterator();
        
        // read the value of each attribute
        while ( attributeIter.hasNext() ) {
            final DataAttribute attribute = (DataAttribute)attributeIter.next();
            final String key = attribute.name();
            final Class type = attribute.type();
			
			final String stringValue = adaptor.hasAttribute(key) ? adaptor.stringValue(key) : attribute.getDefaultStringValue();
			final Object value = valueOfTypeFromString(type, stringValue);
            setValueForKey(value, key);
        }
    }
    
    
	/**
	 * Parses the given string value as appropriate for the specified type and returns
	 * the object.  All such types must implement the <code>valueOf</code> method which 
	 * should complement the object's <code>toString()</code> method.  This method is called
	 * internally and is used by the update() method for decoding a data adaptor.
	 * @param type The class of the Object.
	 * @param stringValue The Object's string representation.
	 * @return The Object from the specified string.
	 */
    private Object valueOfTypeFromString( final Class type, final String stringValue ) throws ParseException {
        if ( type.equals(String.class) )  return stringValue;
        
        Object value = null;
        
		try {
			Method valueOfMethod = type.getMethod("valueOf", new Class[] {java.lang.String.class});
			// convert the value to the Object of the appropriate class
			value = valueOfMethod.invoke(null, new Object[] {stringValue});
		}
		catch(NoSuchMethodException exception) {
			final String message = "The valueOf() method was not found for the attribute of type:" + type;
			Logger.getLogger("global").log( Level.SEVERE, message, exception );
			throw new ParseException(message);
		}
		catch(SecurityException exception) {
			final String message = "The valueOf() method was not accessible for the attribute of type: " + type;
			Logger.getLogger("global").log( Level.SEVERE, message, exception );
			throw new ParseException(message);
		}
		catch(IllegalArgumentException exception) {
			// this should never get thrown since we would have received a NoSuchMethodException earlier
			final String message = "The valueOf() method does not take the correct single String argument for the type: " + type;
			System.err.println( message );
			Logger.getLogger("global").log( Level.SEVERE, message, exception );
			exception.printStackTrace();
		}
		catch(IllegalAccessException exception) {
			// this should never get thrown since we would have received a NoSuchMethodException earlier
			final String message = "The valueOf() method does not have public access for the type: " + type;
			System.err.println( message );
			Logger.getLogger("global").log( Level.SEVERE, message, exception );
			exception.printStackTrace();
		}
		catch(InvocationTargetException exception) {
			// this exception gets called if the valueOf() method throws an exception
			String message = "The valueOf() method for type: " + type + " threw an exception: " + exception.getTargetException();
			Logger.getLogger("global").log( Level.SEVERE, message, exception );
			throw new ParseException(message);
		}

        
        return value;
    }


    /**
     * Write data to the data adaptor for storage.
     * @param adaptor The adaptor to which the receiver's data is written
     */
    public void write( final DataAdaptor adaptor ) {
        Set keys = keys();
        Iterator keyIter = keys.iterator();
        
        while ( keyIter.hasNext() ) {
            String key = (String)keyIter.next();
            Object value = valueForKey(key);
            adaptor.setValue(key, value);
        }
    }
    // end DataListener methods
	
	
	
	/**
	 * A runtime exception thrown while attempting to parse values stored as strings into an object of 
	 * the appropriate type.
	 */
	public class ParseException extends RuntimeException {
		public ParseException(String description) {
			super(description);
		}
	}
}
