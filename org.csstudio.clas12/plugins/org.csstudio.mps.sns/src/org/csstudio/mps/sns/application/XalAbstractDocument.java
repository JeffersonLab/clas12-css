//
//  XalAbstractDocument.java
//  xal
//
//  Created by Thomas Pelaia on 3/28/05.
//  Copyright 2005 Oak Ridge National Lab. All rights reserved.
//

package org.csstudio.mps.sns.application;

import java.net.*;
import java.awt.print.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JOptionPane;

import org.csstudio.mps.sns.tools.database.LoginDialog;
import org.csstudio.mps.sns.tools.messaging.MessageCenter;
import org.csstudio.mps.sns.view.MPSBrowserView;


/** Abstract superclass of both free and internal documents.*/
abstract class XalAbstractDocument implements Pageable {
	/** wildcard file extension */
	static public final String WILDCARD_FILE_EXTENSION = ApplicationAdaptor.WILDCARD_FILE_EXTENSION;
	
	// public static constants for confirmation dialogs
	final static public int YES_OPTION = JOptionPane.YES_OPTION;
	final static public int NO_OPTION = JOptionPane.NO_OPTION;
	
    // basic document instance variables
    protected boolean hasChanges;       // whether the document has changes that need saving
    protected String title;             // The title of the document
    protected URL source;               // The persistent storage URL for the document
    
    // messaging
    protected MessageCenter _messageCenter;      // The local message center
    
    
    /** Constructor for new documents */
    public XalAbstractDocument() {
        setHasChanges(false);
        registerEvents();
        setSource(null);
    }
    
    
    /**
	 * Subclasses should implement this method to return the array of file 
     * suffixes identifying the files that can be written by the document.
	 * By default this method returns the same types as specified by the 
	 * application adaptor.
     * @return An array of file suffixes corresponding to writable files
     */
    public String[] writableDocumentTypes() {
         return Application.getAdaptor().writableDocumentTypes();
	}
    
    
    /** Register this document as a source of DocumentListener events. */
    protected void registerEvents() {
        _messageCenter = new MessageCenter("Xal Document Messaging");
    }
    
    
    /** Construct the main window and associate it with this document. */
    abstract void setupMainWindow();
    
    
    /** Subclasses must implement this method to make their custom main window. */
    abstract protected void makeMainWindow();
    
    
    /**
	 * Override this method to register custom document commands if any.  You 
     * do so by registering actions with the commander.  Those action instances should 
     * have a reference to this document so the action is executed on the 
     * document when the action is activated.
     * The default implementation of this method does nothing.
     * @param commander The commander that manages commands.
     * @see Commander#registerAction(Action)
     */
    protected void customizeCommands( final Commander commander ) {
    }
    
    
    /**
	 * Subclasses should override this method if this document should use a menu definition
	 * other than the default specified in application adaptor.  The document menu inherits the
	 * application menu definition.  This custom path allows the document to modify the
	 * application wide definitions for this document.  By default this method returns null.
     * @return The menu definition properties file path in classpath notation
	 * @see ApplicationAdaptor#getPathToResource
     */
    protected String getCustomMenuDefinitionPath() {
		return null;
    }
	
	
    /**
	 * Get the document title.
     * @return The title of the document.
     */
    public String getTitle() {
        return title;
    }
    
    
    /**
	 * Set the document title.
     * @param newTitle The new title for this document.
     */
    public void setTitle(String newTitle) {
        title = newTitle;
    }
    
    
    /**
	 * Get the URL of the persistent storage for this document.
     * @return The URL of this document's persistent storage.
     */
    public URL getSource() {
        return source;
    }
	
	
	/**
	 * Get the default document folder.
	 * @return the default folder for documents or null if none has been set.
	 */
	protected java.io.File getDefaultFolder() {
		return Application.getApp().getDefaultDocumentFolder();
	}
    
	
	/**
	 * Get the default document folder as a URL.
	 * @return the default folder for documents as a URL or null if none has been set.
	 */
	protected URL getDefaultFolderURL() {
		return Application.getApp().getDefaultDocumentFolderURL();
	}
	
    
    
    /**
	 * Set the URL of the persistent storage for this document.
     * @param url The URL of the persistent storage to set for this document.
     */
    public void setSource(URL url) {
        source = url;
		generateDocumentTitle();
    }
	
	
	/**
	 * Check if the document is empty.  An empty document has no source file and has
	 * not been edited.
	 * @return true if this document is empty and false if not
	 */
	public boolean isEmpty() {
		return (getSource() == null) && !hasChanges();
	}
	
	
	/**
	 * Generate and set the title for this document.  By default the title is set to 
	 * the file path of the document or the default empty document file path if the document
	 * does not have a file store.
	 */
	protected void generateDocumentTitle() {
		setTitle( getDisplayFilePath() );

	}
	
	
	/**
	 * By default the file path to display for this document is set to the file path of the 
	 * document or the default empty document file path if the document does not yet have 
	 * a file store.
	 * @return the file path of the document or the default empty document file path as appropriate
	 */
	protected String getDisplayFilePath() {		
		return (source != null) ? source.getPath() : getEmptyDocumentPath();
	}
	
	
	/**
	 * Get the default file path to use for empty documents
	 * @return "Untitled." + the first writable document type or simply "Untitled" if there are none 
	 */
	protected String getEmptyDocumentPath() {
		String[] writableTypes = writableDocumentTypes();
		String[] readableTypes = Application.getAdaptor().readableDocumentTypes();
		String defaultTitle = "";
		if ( writableTypes.length > 0 ) {
			final String defaultType = writableTypes[0];
			final String defaultExtension = ( defaultType.equals( WILDCARD_FILE_EXTENSION) ) ? "" : "." + defaultType;
			defaultTitle = "Untitled" + defaultExtension; 
		}
		else if ( readableTypes.length > 0 ) {
			defaultTitle = "Untitled";
		}
		
		return defaultTitle;
	}
    
    
    /** 
	* Indicates if there are changes that need saving.
	* @return Status of whether this document has changes that need saving.
	*/
    public boolean hasChanges() {
        return hasChanges;
    }
    
    
    /**
	 * Set the whether this document has changes.
     * @param changeStatus Status to set whether this document has changes that need saving.
     */
    abstract public void setHasChanges( final boolean changeStatus );
    
    
    /**
	 * Subclasses need to implement this method for saving the document to a URL.
     * @param url The URL to which this document should be saved.
     */
    abstract public void saveDocumentAs(URL url);
    
    
    /**
	 * Save this document to its persistent storage source.
     */
    public void saveDocument() {
        saveDocumentAs(source);
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
    abstract protected boolean closeDocument();
	
	
	/**
	 * Determine whether the user should be warned when closing a document with unsaved changes.
	 * The default behavior is to warn the user if they have unsaved changes.  Override this 
	 * method if you don't want the user to be warned of unsaved changes.
	 * @return true if the user should be warned and false if not.
	 */
	public boolean warnUserOfUnsavedChangesWhenClosing() {
		return true;
	}
	
	
	/**
	 * Free document resources.
	 */
	protected void freeResources() {
		freeCustomResources();
		_messageCenter = null;
	}
	
	
	/**
	 * Dispose of custom document resources.  Subclasses should override this method
	 * to provide custom disposal of resources.  The default implementation does nothing.
	 */
	protected void freeCustomResources() {
	}
    
    
    /**
	 * Called when the document will be closed.  The default implementation does nothing.
     * Subclasses should override this method if they need to handle this event.
     */
    protected void willClose() {
    }
    
    
    /**
	 * Get the main window for this document.
     * @return The main window for this document.
     */
    abstract public XalDocumentView getDocumentView();
	
	
	/**
	 * If the main window is not already created, make it and perform any initilization
	 * to bind the window to the document.
	 */
	void initMainWindow() {
        if ( getDocumentView() == null ) {
            setupMainWindow();
        }
	}
    
    
    /**
	 * Make this document's window visible.
     */
    public void showDocument() {
        initMainWindow();
        getDocumentView().showWindow();
    }
    
    
    /**
	 * Hide this document.
     */
    public void hideDocument() {
        getDocumentView().hideWindow();
    }
	
	
	/**
	 * Display a confirmation dialog with a title and message
	 * @param title The title of the dialog
	 * @param message The message to display
	 * @return YES_OPTION or NO_OPTION 
	 */
	public int displayConfirmDialog( final String title, final String message ) {
        return getDocumentView().displayConfirmDialog( title, message );		
	}
    
    
    /**
	 * Display a warning dialog box and provide an audible alert.
     * @param title Title of the warning dialog box.
     * @param message The warning message to appear in the warning dialog box.
     */
    public void displayWarning( final String title, final String message) {
        getDocumentView().displayWarning( title, message );
    }
    
    
    /**
	 * Display a warning dialog box with information about the exception and provide
     * an audible alert.
     * @param exception The exception about which the warning dialog is displayed.
     */
    public void displayWarning( final Exception exception ) {
        getDocumentView().displayWarning( exception );
    }    
    
    
    /**
	 * Display a warning dialog box with information about the exception and provide
     * an audible alert.  This method allows
     * clarification about the consequences of the exception (e.g. "Save Failed:").
     * @param title Title of the warning dialog box.
     * @param prefix Text that should appear in the dialog box before the exception messasge.
     * @param exception The exception about which the warning dialog is displayed.
     */
    public void displayWarning( final String title, final String prefix, final Exception exception ) {
        getDocumentView().displayWarning( title, prefix, exception );
    }
	
    
	
    
    /**
	 * Display an error dialog box and provide an audible alert.
     * @param title Title of the warning dialog box.
     * @param message The warning message to appear in the warning dialog box.
     */
    public void displayError( final String title, final String message ) {
        getDocumentView().displayError( title, message );
    }
    
    
    /**
	 * Display an error dialog box with information about the exception and 
     * provide an audible alert.
     * @param exception The exception about which the warning dialog is displayed.
     */
    public void displayError( final Exception exception ) {
        getDocumentView().displayError( exception );
    }    
    
    
    /**
	 * Display an error dialog box with information about the exception and 
     * provide an audible alert.  This method allows
     * clarification about the consequences of the exception (e.g. "Save Failed:").
     * @param title Title of the warning dialog box.
     * @param prefix Text that should appear in the dialog box before the exception messasge.
     * @param exception The exception about which the warning dialog is displayed.
     */
    public void displayError( final String title, final String prefix, final Exception exception ) {
        getDocumentView().displayError( title, prefix, exception );
    }
    
	
    // ---------- Implement Pageable interface ---------------------------------
    
    /**
	 * Implement the Pageable interface.  Subclasses may override this implementation 
     * to support custom document printing.
     * Returns the number of pages to print.
     * This default implementation simply returns one for printing a single page.
     * @return The number of pages to print
     */
    public int getNumberOfPages() {
        return 1;
    }
    
    
    /**
	 * Implement the Pageable interface.  Subclasses may override this implementation 
     * to support custom document printing.
     * This default implementation gets the page format from the default print manager.
     * @param pageIndex The page number corresponding to the page format to return
     * @return PageFormat for printing the specified page
     * @see org.csstudio.mps.sns.application.PrintManager#getPageFormat
     * @throws java.lang.IndexOutOfBoundsException when the page index is out of range
     */
    public PageFormat getPageFormat( final int pageIndex ) throws IndexOutOfBoundsException {
        return PrintManager.defaultManager().getPageFormat();
    }
    
    
    /**
	 * Implement the Pageable interface.  Subclasses may override this implementation 
     * to support custom document printing.
     * This default implementation simply returns a printable object that prints 
     * one page of the document's main window.
     * @param pageIndex The page to print
     * @return The printable object responsible for printing the main window
     * @throws java.lang.IndexOutOfBoundsException when the page index is out of range
     */
    public Printable getPrintable( final int pageIndex ) throws IndexOutOfBoundsException {
        return new Printable() {
            public int print( Graphics graphics, PageFormat pageFormat, int pageIndex ) {
                final Graphics2D graphics2D = (Graphics2D)graphics;
                graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
				
				final Dimension viewSize = getDocumentView().getContentPane().getSize();
				final double pageWidth = pageFormat.getImageableWidth();
				final double pageHeight = pageFormat.getImageableHeight();
				final double xScale = pageWidth / viewSize.width;
				final double yScale = pageHeight / viewSize.height;
				final double scale = Math.min(xScale, yScale); 
				graphics2D.scale( scale, scale );
				
                getDocumentView().getContentPane().printAll(graphics);
				
                return Printable.PAGE_EXISTS;
            }
        };
    }	
}
