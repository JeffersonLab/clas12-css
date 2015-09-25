/*
 * MessageHandler.java
 *
 * Created on February 5, 2002, 11:40 AM
 */

package org.csstudio.mps.sns.tools.messaging;


import java.lang.reflect.*;
import java.util.*;


/**
 * MessageHandler is an abstract class whose subclasses receive and 
 * forward messages. It provides the foundation for handling messages.
 *
 * @author  tap
 */
abstract class MessageHandler<T> implements InvocationHandler, java.io.Serializable {
    protected Class<T> _protocol;
    protected Object source;
    protected T proxy;
    protected Thread[] threadPool;
    protected TargetDirectory targetDirectory;
    
    
    /** Creates new MessageHandler */
    public MessageHandler( final TargetDirectory newDirectory, final Class<T> newProtocol, final int threadPoolSize ) {
        this( newDirectory, null, newProtocol, threadPoolSize );
    }
    
    
    /** Creates new MessageHandler */
    public MessageHandler( final TargetDirectory newDirectory, final Object newSource, final Class<T> newProtocol, final int threadPoolSize ) {
        targetDirectory = newDirectory;
        source = newSource;
        _protocol = newProtocol;
        threadPool = new Thread[threadPoolSize];
        createProxy();
    }
	
	
	/** Subclasses should override this method to perform any cleanup prior to removal. */
	public void terminate() {}
    
    
    /** return the interface managed by this handler */
    public Class<T> getProtocol() {
        return _protocol;
    }
    
    
    /** return the source of the messages */
    public Object getSource() {
        return source;
    }
    
    
    /** return the proxy that will forward messages to registered targets */
    public T getProxy() {
        return proxy;
    }
    
    
    /** create the proxy for this handler to message */
    private void createProxy() {
		ClassLoader loader = this.getClass().getClassLoader();
        Class[] protocols = new Class[] {_protocol};
        
        proxy = (T)Proxy.newProxyInstance( loader, protocols, this );
    }
    
    
    /** 
     * subclasses must override whether they support synchronous or asynchronous messages
	 * @return true if the messaging is synchronous and false if not
     */
    abstract public boolean isSynchronous();
    
    
    /** implement InvocationHandler interface */
    /** invoke method */
    abstract public Object invoke( final Object proxy, final Method method, final Object[] args );
        
    
    /** get all targets associated with the source and protocol and just the protocol */
    protected Set targets() {
        Set targetSet = new HashSet();
        
        // add targets directly associated with the protocol and the target
        Set directTargets = targetDirectory.targets( source, _protocol );
        targetSet.addAll( directTargets );
        
        // add targets associated with the protocol but no target
        Set anonymousTargets = targetDirectory.targets( null, _protocol );
        targetSet.addAll( anonymousTargets );

        return targetSet;
    }
}
