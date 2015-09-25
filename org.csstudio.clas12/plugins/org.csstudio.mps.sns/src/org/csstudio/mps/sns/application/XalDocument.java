/*
 * XalDocument.java
 *
 * Created on March 19, 2003, 11:07 AM
 */

package org.csstudio.mps.sns.application;

import org.csstudio.mps.sns.tools.bricks.WindowReference;


/**
 * The base class for custom documents.  Subclasses of this class need to define 
 * the logic for their document.  Every document has a main window and a URL source
 * that provides persistent storage.
 *
 * @author  t6p
 */
abstract public class XalDocument extends XalAbstractDocument {
	/** wildcard file extension */
	static public final String WILDCARD_FILE_EXTENSION = XalAbstractDocument.WILDCARD_FILE_EXTENSION;
	
	// public static constants for confirmation dialogs
	final static public int YES_OPTION = XalAbstractDocument.YES_OPTION;
	final static public int NO_OPTION = XalAbstractDocument.NO_OPTION;
	
    // basic document instance variables
    protected XalWindow mainWindow;     // The main window for the document
    
    // messaging
    protected XalDocumentListener documentListenerProxy;    // The proxy for document events
    
    
    /** Constructor for new documents */
    public XalDocument() {
		super();
    }
    
    
    /** Register this document as a source of DocumentListener events. */
    protected void registerEvents() {
		super.registerEvents();
        documentListenerProxy = (XalDocumentListener)_messageCenter.registerSource(this, XalDocumentListener.class);
    }
    
    
    /** Add the listener for events from this document. */
    public void addXalDocumentListener( final XalDocumentListener listener ) {
        _messageCenter.registerTarget( listener, this, XalDocumentListener.class );
    }
    
    
    /** Remove the listener from event from this document. */
    public void removeXalDocumentListener( final XalDocumentListener listener ) {
        _messageCenter.removeTarget( listener, this, XalDocumentListener.class );
    }
    
    
    /** Construct the main window and associate it with this document. */
	void setupMainWindow() {
        makeMainWindow();
      	addXalDocumentListener( mainWindow );
       	mainWindow.titleChanged(this, title);
    }
 	
	
	/** Get the window reference from the resource if any */
	static public WindowReference getDefaultWindowReference( final String tag, final Object... parameters ) {
		return Application.getAdaptor().getDefaultWindowReference( tag, parameters );
	}
	
    
    /**
     * Set the document title.
     * @param newTitle The new title for this document.
     */
    public void setTitle( final String newTitle ) {
		super.setTitle( newTitle );
        documentListenerProxy.titleChanged(this, title);
    }	
    
    
    /**
     * Set the whether this document has changes.
     * @param changeStatus Status to set whether this document has changes that need saving.
     */
    public void setHasChanges( final boolean changeStatus ) {
        if ( changeStatus != hasChanges ) {
            hasChanges = changeStatus;
            documentListenerProxy.hasChangesChanged(this, hasChanges);
        }
    }


    /**
     * This method is a request to close a document.  It may be called when, for 
     * example, the user selects "Close" from the File menu, or when the user closes the 
     * window with the close button, or when the application quits.  This request
     * starts a series of events which closes the document.  Xal document 
     * listeners are notified that the document will close.  They may perform 
     * any cleanup as necessary before the document closes.  Then the listeners 
     * are informed that the document has closed.  The application removes 
     * the document from its list of open documents and informs its listeners 
     * that the document has been closed.  If there are any unsaved changes, the 
     * user is given an opportunity to not close the document so they can save 
     * the changes.
     */
    protected boolean closeDocument() {
		if ( warnUserOfUnsavedChangesWhenClosing() && hasChanges() ) {
			if ( !mainWindow.userPermitsCloseWithUnsavedChanges() )  return false;
		}
        documentListenerProxy.documentWillClose(this);
        willClose();
        documentListenerProxy.documentHasClosed(this);
		
		freeResources();
		
        return true;
    }

	
	/**
	 * Free document resources.
	 */
	final protected void freeResources() {
		super.freeResources();
		
		documentListenerProxy = null;
		mainWindow = null;		
	}
    
    
    /**
     * Get the main window for this document.
     * @return The main window for this document.
     */
    public XalWindow getMainWindow() {
        return mainWindow;
    }
    
    
    /**
	 * Implement the method for XalAbstractDocument.
     * @return The main window for this document.
     */
    public XalDocumentView getDocumentView() {
        return mainWindow;
    }
}
