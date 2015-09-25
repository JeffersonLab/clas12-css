/*
 * Commander.java
 *
 * Created on March 28, 2003, 10:00 AM
 */

package org.csstudio.mps.sns.application;

import org.csstudio.mps.sns.tools.apputils.iconlib.IconLib;
import org.csstudio.mps.sns.tools.apputils.iconlib.IconLib.*;

import javax.swing.*;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.util.*;
import java.util.logging.*;


/**
 * The Commander manages the commands (actions and menu handlers) that are used 
 * in toolbars, menu items and menus.  It creates and returns a menubar and a 
 * toolbar based on the menu definition for the application.  A default menu 
 * definition provides the starting point.  A custom menu definition can make 
 * changes as needed.
 * This class may optionally be overriden to provide custom commands.  Alternatively
 * hooks in ApplicationAdaptor, XalDocument and XalWindow allow custom commands to 
 * be specified.  
 * The XalWindow creates a commander for its document.  The commander builds the 
 * menu and toolbar and creates the associated actions.  The commander is then 
 * disposed.
 *
 * @author  tap
 */
public class Commander {
	/** map equivalent of the resource bundle properties */
    protected Map _controlMap;
	
	/** (action-name, action) map */
    protected Map _commands;
	
	/** (action-name, button model) map */
	protected Map _buttonModelMap;
    
	
    /** Primary Constructor for generating a document commander. */
    protected Commander( final Commander appCommander, final XalAbstractDocument document ) {
        _controlMap = new HashMap( appCommander._controlMap );
        _commands = new HashMap( appCommander._commands );
		_buttonModelMap = new HashMap( appCommander._buttonModelMap );
        
		loadCustomBundle( document );		// document additions
    }
    
	
    /** Constructor for generating a document commander. */
    protected Commander( final Commander appCommander, final XalDocument document ) {
		this( appCommander, (XalAbstractDocument)document );
        registerCommands( document );
    }
    
	
    /** Constructor for generating a document commander. */
    protected Commander( final Commander appCommander, final XalInternalDocument document ) {
		this( appCommander, (XalAbstractDocument)document );
        registerCommands( document );
    }
    
	
    /** Constructor for generating a document commander. */
    protected Commander( final XalInternalDocument document ) {
        _controlMap = new HashMap();
        _commands = new HashMap();
		_buttonModelMap = new HashMap();
		
		loadCustomDocumentBundle( document );		// document additions
		registerCustomCommands( (XalInternalDocument)document );
    }
    
    
    /** Constructor for generating an application commander. */
    protected Commander( final Application application ) {
        _controlMap = new HashMap();
        _commands = new HashMap();
		_buttonModelMap = new HashMap();
        
        loadDefaultBundle();
        loadCustomBundle( application );
		
        registerCommands();
    }
    
    
    /** Constructor for generating a desktop application commander. */
    protected Commander( final DesktopApplication application ) {
        _controlMap = new HashMap();
        _commands = new HashMap();
		_buttonModelMap = new HashMap();
        
        loadDefaultBundle();
        loadCustomBundle( application );
		
        registerDesktopCommands();
    }
    
    
    /** Load the default bundle. */
    protected void loadDefaultBundle() {
        loadBundle( "org.csstudio.mps.sns.application.resources.menudef" );
    }
    
    
    /**
     * Load a custom bundle if one exists.  If a custom bundle exists, it will override and extend the
	 * properties found in the default bundle.  You can use it to customize your toolbar, menubar and menus.
	 * @param application the application for which to load the custom bundle.
     */
    protected void loadCustomBundle( final Application application ) {
        String path = application.getApplicationAdaptor().getMenuDefinitionPath();
        try {
            loadBundle( path );
        }
        catch( MissingResourceException exception ) {
            //System.err.println("No custom bundle found at: " + path);
        }
    }
    
    
    /**
     * Load a custom bundle for the document if one exists.  If a custom document bundle exists, 
	 * it will override and extend the properties found in the application bundle.  You can use it to 
     * customize your toolbar, menubar and menus.
	 * @param document the document for which to load the custom bundle.
     */
    protected void loadCustomBundle( final XalAbstractDocument document ) {
        String path = document.getCustomMenuDefinitionPath();
		if ( path == null )  return;	// no custom document menu definition
		
        try {
            loadBundle( path );
        }
        catch( MissingResourceException exception ) {
			final String message = "No custom document menu bundle found at: " + path;
			Logger.getLogger("global").log( Level.WARNING, message, exception );
            System.err.println( message );
        }
    }
    
    
    /**
	 * Load a custom bundle for the document if one exists.  If a custom document bundle exists, 
	 * it will override and extend the properties found in the application bundle.  You can use it to 
     * customize your toolbar, menubar and menus.
	 * @param document the document for which to load the custom bundle.
     */
    protected void loadCustomDocumentBundle( final XalInternalDocument document ) {
        String path = document.getCustomInternalMenuDefinitionPath();
		if ( path == null )  return;	// no custom document menu definition
		
        try {
            loadBundle( path );
        }
        catch( MissingResourceException exception ) {
			final String message = "No custom document menu bundle found at: " + path;
			Logger.getLogger("global").log( Level.WARNING, message, exception );
            System.err.println( message );
        }
    }
    
    
    /**
     * Load a bundle specified by the path in Java classpath notation.  The path 
     * should refer to a properties file but the path should exclude the suffix.
     * @param path Path in Java classpath notation to a properties file excluding the file suffix
     */
    protected void loadBundle( final String path ) throws MissingResourceException {
        //_controlMap.putAll( Util.loadResourceBundle(path) );
		Util.mergeResourceBundle( _controlMap, path );
    }
    
    
    /**
     * Make and return a new menubar based on the menu definition file.
     * @return The new menubar
     */
    public JMenuBar getMenubar() {
        JMenuBar menuBar = new JMenuBar();
        
		final String menubarStr = (String)_controlMap.get( "menubar" );
		if ( menubarStr == null || menubarStr == "" )  return null;		// check if a menubar definition was found
		
        final String[] menuKeys = Util.getTokens( menubarStr );
        
        for ( int menuIndex = 0 ; menuIndex < menuKeys.length ; menuIndex++ ) {
            final String menuKey = menuKeys[menuIndex];
			if ( getState( menuKey ).isIncluded() ) {
				JMenu menu = makeMenu( menuKey );
				menuBar.add( menu );
			}
        }
        
        return menuBar;
    }
    
    
    /**
     * Make and return a new toolbar based on the menu definition file.
     * @return The new toolbar
     */
    public JToolBar getToolbar() {        
        final String[] buttonKeys = Util.getTokens( (String)_controlMap.get( "toolbar" ) );
		if ( buttonKeys.length == 0 )  return null;		// there is no toolbar defined
        
		final JToolBar toolBar = new JToolBar();
        for ( int index = 0 ; index < buttonKeys.length ; index++ ) {
            final String buttonKey = buttonKeys[index];
			if ( !getState( buttonKey ).isIncluded() )  continue;		// skip items that are marked as not included

            if ( buttonKey.equals("-") ) {
                toolBar.addSeparator();
            }
			else if ( buttonKey.startsWith("*") ) {	 // an asterisk identifies a toggle button group
				addToolbarItemsFromGroup( toolBar, buttonKey.substring(1) );
			}
            else {
                final String label = getLabel( buttonKey );
                AbstractButton button;
                
                final String actionKey = (String)_controlMap.get( buttonKey + "_action" );
                if ( actionKey != null ) {
					final ButtonModel model =  (ButtonModel)_buttonModelMap.get( actionKey );
                    final Action action = (Action)_commands.get( actionKey );
					
					if ( model != null ) {
						if ( model instanceof ToggleButtonModel ) {
							button = new JToggleButton();
						}
						else {
							button = new JButton();
						}
						button.setModel( model );
						button.setEnabled( true );
					}
					else {
						button = new JButton();
					}
					
					if ( action != null ) {
						button.setAction( action );
						if ( action.getValue( Action.SMALL_ICON ) == null ) {
							button.setText( label );
						}
						else {
							button.setText( "" );
							button.setToolTipText( label );
						}
					}
					else {
						button.setText( label );
					}
                }
                else {
					button = new JButton( label );
                    button.setEnabled( false );
                }
				if ( button == null ) {
					button = new JButton( label );
					button.setText( label );
				}
                toolBar.add( button );
            }
        }
        
        return toolBar;
    }
		
	
	/**
	 * Add to the toolbar a group of toggle buttons which are mutually exclusive with respect to selection state.
	 * @param toolbar The toolbar to which to add the toggle buttons.
	 * @param groupKey The key for the group of buttons to add with a common button group.
	 */
	private void addToolbarItemsFromGroup( final JToolBar toolbar, final String groupKey ) {
        // Get the list of items as a space delimited string
        String itemList = (String)_controlMap.get( groupKey + "_group" );
        
        // check if there are any items
        if ( itemList == null ) {
            return;
        }
        
        // Get the list of item identifiers as an array and create buttons 
        // and add them to the toolbar
        String[] itemKeys = Util.getTokens( itemList );
		ButtonGroup buttonGroup = new ButtonGroup();
        for ( int itemIndex = 0 ; itemIndex < itemKeys.length ; itemIndex++ ) {
            final String itemKey = itemKeys[itemIndex];
			if ( !getState( itemKey ).isIncluded() )  continue;		// skip items that are marked as not included
			
			// create the button and assign its action and add the button to the toolbar
			final String label = getLabel( itemKey );
			JToggleButton button = new JToggleButton( label );
			
			String actionKey = (String)_controlMap.get( itemKey + "_action" );
			if ( actionKey != null ) {
				final Action action = (Action)_commands.get( actionKey );
				button.setAction( action );
				button.setText( label );
				
				final ButtonModel model =  (ButtonModel)_buttonModelMap.get( actionKey );
				if ( model != null && ( model instanceof ToggleButtonModel ) ) {
					button.setModel( model );
					buttonGroup.add( button );
				}
				else {
					button.setEnabled( false );
					final String message = "Warning: No ToggleButtonModel provided for the toolbar button: " + label + " of group: " + groupKey;
					Logger.getLogger("global").log( Level.SEVERE, message );
					System.err.println( message );
				}
			}
			else {
				button.setEnabled( false );
			}
			toolbar.add(button);
		}
	}
    
    
    /**
     * Build a menu associated with the specified menu key.  The menus are 
     * defined in the menu definition files and the menu keys are the items 
     * listed for a menubar.  The menu, its menu items and sub menus and so forth 
     * are all created and the actions and menu handlers are assigned as 
     * appropriate.
     * @param menuKey The key identifying the menu to generate
     * @return The generated menu
     */
    private JMenu makeMenu( final String menuKey ) {
        // fetch the label for the menu and create a new menu with the label
        final String menuLabel = getLabel( menuKey );
        final JMenu menu = new JMenu( menuLabel );

        // if there is a handler for the menu make the handler a listener of the menu
        final String menuHandlerKey = (String)_controlMap.get( menuKey + "_handler" );
        final MenuListener menuHandler = (MenuListener)_commands.get( menuHandlerKey );
        if ( menuHandler != null ) {
            menu.addMenuListener( menuHandler );
        }

        // Get the list of menu items as a space delimited string
        final String menuItemList = (String)_controlMap.get( menuKey + "_menu" );
        
        // check if the menu has been assigned items
        if ( menuItemList == null ) {
			menu.setEnabled( menuHandler != null );		// if the menu has not no items and no handler then disable it
            return menu;
        }
		
		final String defaultActionKey = getActionKey( menuKey );	// this is the default action for menu items that don't have an action
        
        // Get the list of menu item identifiers as an array and create menu items and add them to the menu
        final String[] menuItemKeys = Util.getTokens( menuItemList );
        for ( int itemIndex = 0 ; itemIndex < menuItemKeys.length ; itemIndex++ ) {
            final String menuItemKey = menuItemKeys[itemIndex];
			if ( !getState( menuItemKey ).isIncluded() )  continue;		// skip items that are marked as not included
			
            if ( menuItemKey.equals( "-" ) ) {    // dashes identify separators
                menu.addSeparator();
            }
            else if ( menuItemKey.startsWith("^") ) {   // carets identify submenus
                JMenuItem menuItem = makeMenu( menuItemKey.substring(1) );
                menu.add( menuItem );
            }
			else if ( menuItemKey.startsWith("*") ) {	// an asterisk identifies a radio button group of menu items
				addMenuItemsFromGroup( menu, menuItemKey.substring(1) );
			}
            else {
                // create the menu item and assign its action and add the menu item to the menu
                JMenuItem menuItem;

                final String explicitActionKey = getActionKey( menuItemKey );
				final String actionKey = ( explicitActionKey != null ) ? explicitActionKey : defaultActionKey;
				
				if ( actionKey != null ) {
					final ButtonModel model =  (ButtonModel)_buttonModelMap.get( actionKey );
					
					if ( model != null ) {
						if ( model instanceof ToggleButtonModel ) {
							final String label = getLabel( menuItemKey );
							menuItem = new JCheckBoxMenuItem( label );
						}
						else {
							menuItem = makeMenuItem( menuItemKey );
						}
						menuItem.setModel( model );
					}
					else {
						menuItem = makeMenuItem( menuItemKey );
					}
					
					final String label = menuItem.getText();
                    final Action action = (Action)_commands.get( actionKey );
					if ( action != null ) {
						menuItem.setAction( action );
					}
					
					// disable the menu item if it doesn't have an action or a model
					if ( action == null && model == null ) {
						menuItem.setEnabled( false );
					}
					
                    menuItem.setText( label );
				}
				else {
					final String label = getLabel( menuItemKey );
					menuItem = new JMenuItem( label );
                    menuItem.setEnabled( false );
				}
				
                menu.add( menuItem );
            }
        }
        
        return menu;
    }
	
	
	/**
	 * Instantiate a new menu item given the menu item key.
	 * @param menuItemKey  the key identifying the menu item
	 * @return the new menu item
	 */
	private JMenuItem makeMenuItem( final String menuItemKey ) {
		return new JMenuItem( getLabel( menuItemKey ) );
	}
	
	
	/**
	 * Get the button model corresponding to the specified action key.
	 * @param actionKey the key for which to get the button model.
	 * @return the button model.
	 */
	public ButtonModel getModel( final String actionKey ) {
		return (ButtonModel)_buttonModelMap.get( actionKey );
	}
	
	
	/**
	 * Get the action key for the specified menu or menu item by its ID.
	 * @param menuItemKey  the ID of the menu item for which to get the label
	 * @return the action key for the menu item
	 */
	public String getActionKey( final String menuItemKey ) {
		return (String)_controlMap.get( menuItemKey + "_action" );
	}
	

	/**
	 * Get the label for the specified item by its ID.  Look for a control entry with the item ID followed by "_label".
	 * If the label identifier cannot be found then default to the item ID itself as the label.
	 * @param itemID  the ID of the menu item for which to get the label
	 * @return the label for the menu item
	 */
	public String getLabel( final String itemID ) {
		final String explicitLabel = (String)_controlMap.get( itemID + "_label" );
		// if the explicit label is not set then default to the item ID itself for the label
		return ( explicitLabel == null || explicitLabel == "" ) ? itemID : explicitLabel;
	}
	
	
	/**
	 * Get the state of the item.
	 */
	public ItemState getState( final String itemID ) {
		final String description = (String)_controlMap.get( itemID + "_state" );
		return ItemState.getState( description );
	}
	
	
	/**
	 * Add to the menu a group of radio button menu items which are mutually exclusive with respect to selection state.
	 * @param menu The menu to which to add the radio button menu items.
	 * @param groupKey The key for the group of buttons to add with a common button group.
	 */
	private void addMenuItemsFromGroup( final JMenu menu, final String groupKey ) {
        // Get the list of menu items as a space delimited string
        String menuItemList = (String)_controlMap.get( groupKey + "_group" );
        
        // check if the menu has been assigned items
        if ( menuItemList == null ) {
            return;
        }
        
        // Get the list of menu item identifiers as an array and create menu items 
        // and add them to the menu
        String[] menuItemKeys = Util.getTokens( menuItemList );
		ButtonGroup buttonGroup = new ButtonGroup();
        for ( int itemIndex = 0 ; itemIndex < menuItemKeys.length ; itemIndex++ ) {
            final String menuItemKey = menuItemKeys[itemIndex];
			if ( !getState( menuItemKey ).isIncluded() )  continue;		// skip items that are marked as not included
			
			// create the menu item and assign its action and add the menu item to the menu
			final String label = getLabel( menuItemKey );
			JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( label );
			
			String actionKey = (String)_controlMap.get( menuItemKey + "_action" );
			if ( actionKey != null ) {
				final Action action = (Action)_commands.get( actionKey );
				menuItem.setAction( action );
				menuItem.setText( label );
				
				final ButtonModel model =  (ButtonModel)_buttonModelMap.get( actionKey );
				if ( model != null && ( model instanceof ToggleButtonModel ) ) {
					menuItem.setModel( model );
					buttonGroup.add( menuItem );
				}
				else {
					menuItem.setEnabled( false );
					final String message = "Warning: No ButtonModel provided for the menu item: " + label + " of group: " + groupKey;
					Logger.getLogger("global").log( Level.SEVERE, message );
					System.err.println( message );
				}
			}
			else {
				menuItem.setEnabled( false );
			}
			menu.add( menuItem );
		}
	}
    
    
    /** Register all application commands (default and custom).  Some of these commands may be associated with documents. */
    protected void registerCommands() {
        registerDefaultCommands();
        registerCustomCommands();
    }
    
    
    /** Register the default application commands */
    private void registerDefaultCommands() {
        // file actions
        registerAction( ActionFactory.newAction() );
		registerAction( ActionFactory.newDocumentByTypeAction() );
        registerAction( ActionFactory.openDocumentAction() );
        registerMenuHandler( ActionFactory.openRecentHandler(), "open-recent-handler" );
        registerAction( ActionFactory.closeAllDocumentsAction() );
        registerAction( ActionFactory.saveAllDocumentsAction() );
        registerAction( ActionFactory.pageSetupAction() );
        registerAction( ActionFactory.quitAction() );
        
        // edit actions
		registerAction( ActionFactory.copyAction() );
		registerAction( ActionFactory.cutAction() );
		registerAction( ActionFactory.pasteAction() );
		registerTextCommands();
        
        // view actions
        registerAction( ActionFactory.showConsoleAction() );
        registerAction( ActionFactory.showLoggerAction() );
        
        // window actions
        registerAction( ActionFactory.showAllWindowsAction() );
        registerAction( ActionFactory.hideAllWindowsAction() );
        
        // help actions
        registerAction( ActionFactory.showAboutBoxAction() );
        registerAction( ActionFactory.showHelpWindow() );
    }
    
    
    /** Register all application commands (default and custom).  Some of these commands may be associated with documents. */
    protected void registerDesktopCommands() {
        registerDefaultDesktopCommands();
        registerCustomCommands();
    }
    
    
    /** Register the default application commands */
    private void registerDefaultDesktopCommands() {
        // file actions
        registerAction( ActionFactory.newAction() );
		registerAction( ActionFactory.newDocumentByTypeAction() );
        registerAction( ActionFactory.openDocumentAction() );
        registerMenuHandler( ActionFactory.openRecentHandler(), "open-recent-handler" );
        registerAction( ActionFactory.closeAllDocumentsAction() );
        registerAction( ActionFactory.saveAllDocumentsAction() );
        registerAction( ActionFactory.pageSetupAction() );
        registerAction( ActionFactory.quitAction() );
        
        // edit actions
		registerAction( ActionFactory.copyAction() );
		registerAction( ActionFactory.cutAction() );
		registerAction( ActionFactory.pasteAction() );
        registerTextCommands();
        
        // view actions
        registerAction( ActionFactory.showConsoleAction() );
        registerAction( ActionFactory.showLoggerAction() );
        
        // window actions
		registerMenuHandler( ActionFactory.documentsHandler(), "documents-handler" );
        registerAction( ActionFactory.showAllWindowsAction() );
        registerAction( ActionFactory.hideAllWindowsAction() );
        
        // help actions
        registerAction( ActionFactory.showAboutBoxAction() );
        registerAction( ActionFactory.showHelpWindow() );
    }
    
    
    /**
     * Register all document commands (default and custom).  Some of these commands may  
     * be associated with documents.
     * @param document The document for which some commands may need to be associated
     */
    protected void registerCommands( final XalDocument document ) {
        registerDefaultCommands( document );
        registerCustomCommands( document );
    }
    
    
    /**
	 * Register all document commands (default and custom).  Some of these commands may  
     * be associated with documents.
     * @param document The document for which some commands may need to be associated
     */
    protected void registerCommands( final XalInternalDocument document ) {
        registerDefaultCommands( document );
        registerCustomCommands( document );
    }
    
    
    /**
     * Register the default document commands
     * @param document The document for which some commands may need to be associated
     */
    private void registerDefaultCommands( final XalDocument document ) {
        // file actions
        registerAction( ActionFactory.closeDocumentAction( document ) );
        registerAction( ActionFactory.saveDocumentAction( document ) );
        registerAction( ActionFactory.saveAsDocumentAction( document ) );
        registerAction( ActionFactory.revertToSavedAction( document ) );
        registerAction( ActionFactory.printAction( document ) );
        
        // edit actions
        registerAction( ActionFactory.editPreferencesAction( document ) );
        
        // view actions
        
        // window actions
        registerMenuHandler( ActionFactory.documentsHandler(), "documents-handler" );
        registerAction( ActionFactory.cascadeWindowsAction( document ) );
        registerAction( ActionFactory.captureWindowAsImageAction( document ) );
        
        // help actions
    }
    
    
    /**
		* Register the default document commands
     * @param document The document for which some commands may need to be associated
     */
    private void registerDefaultCommands( final XalInternalDocument document ) {
        // file actions
        registerAction( ActionFactory.closeDocumentAction( document ) );
        registerAction( ActionFactory.saveDocumentAction( document ) );
        registerAction( ActionFactory.saveAsDocumentAction( document ) );
        registerAction( ActionFactory.revertToSavedAction( document ) );
        registerAction( ActionFactory.printAction( document ) );
        
        // edit actions
        registerAction( ActionFactory.editPreferencesAction( document ) );
        
        // view actions
        
        // window actions
        registerAction( ActionFactory.cascadeWindowsAction( document ) );
        registerAction( ActionFactory.captureWindowAsImageAction( document ) );
        
        // help actions
    }
	
    
    /** 
     * Subclasses may override this method to provide custom application commands.  Alternatively custom
     * commands may be specified in subclasses of XalDocument, XalWindow and ApplicationAdaptor for convenience.
     * @see XalDocument#customizeCommands
     * @see XalWindow#customizeCommands
     * @see ApplicationAdaptor#customizeCommands
     */
    protected void registerCustomCommands() {
    }    
    
    
    /** 
     * Subclasses may override this method to provide custom document commands.  Alternatively
     * custom commands may be specified in subclasses of XalDocument, XalWindow and 
     * ApplicationAdaptor for convenience.
     * @param document The document for which some commands may need to be associated
     * @see XalDocument#customizeCommands
     * @see XalWindow#customizeCommands
     * @see ApplicationAdaptor#customizeCommands
     */
    protected void registerCustomCommands( final XalDocument document ) {
    }
    
    
    /** 
	* Subclasses may override this method to provide custom document commands.  Alternatively
	* custom commands may be specified in subclasses of XalInternalDocument, XalInternalWindow and 
	* ApplicationAdaptor for convenience.
	* @param document The document for which some commands may need to be associated
	* @see XalDocument#customizeCommands
	* @see XalWindow#customizeCommands
	* @see ApplicationAdaptor#customizeCommands
	*/
    protected void registerCustomCommands( final XalInternalDocument document ) {
    }
    
    
    /**
     * Get the action with the given name.
     * @param actionName The name of the action to get.
     * @return The action with the given name.
     */
    public Action getAction( final String actionName ) {
        return (Action)_commands.get( actionName );
    }
    
    
    /**
     * Register the action to be used by the commander.  Every action should 
     * have a unique name because the commander fetches actions by name.
     * @param action The action to register.
     */
    public void registerAction( final Action action ) {
        final String name = (String)action.getValue( Action.NAME );
        _commands.put( name, action );
    }
    
    
    /**
     * Register the action and button model to be used by the commander.  Every action should 
     * have a unique name because the commander fetches actions by name.
     * @param action The action to register.
	 * @param model The button model to associate with the action.
     */
    public void registerAction( final Action action, final ButtonModel model ) {
        registerAction( action );
        final String name = (String)action.getValue( Action.NAME );
		registerModel( name, model );
    }
    
    
    /**
     * Register the action to be used by the commander.  Every action should 
     * have a unique name because the commander fetches actions by name.
     * @param name The name to key the model with a button or menu item.
	 * @param model The button model to associate with the action.
     */
    public void registerModel(final String name, final ButtonModel model ) {
		_buttonModelMap.put( name, model );
    }
	
	
    /**
     * Register the menu handler to be used by the commander.  Associate the specified name with the menu handler.
     * @param handler The handler to register
     * @param name The unique name to associate with the handler
     */
    public void registerMenuHandler( final MenuListener handler, final String name ) {
        _commands.put( name, handler );
    }
    
    
    /** Register the actions associated with text components. */
    protected void registerTextCommands() {
        final JTextComponent proxyTextComponent = new JTextPane();
        final Action[] actions = proxyTextComponent.getActions();
        
        for ( Action action : actions ) {
			if ( action.getValue( Action.NAME ).toString().equals( "select-all" ) ) {
				registerAction( action );
				break;
			}
        }
        
        Action selectAllAction = getAction( "select-all" );
        if ( selectAllAction != null ) {
            selectAllAction.putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_A, ActionFactory.menuKeyShortcutMask ) );
        }
    }    
}



/** Class to represent the state of a menu, menu item or toolbar item. */
class ItemState {
	final static private ItemState DEFAULT_STATE;
	
	final private boolean _included;
	
	static {
		DEFAULT_STATE = new ItemState( true );
	}
	
	
	/** Primary Constructor */
	public ItemState( final boolean include ) {
		_included = include;
	}
	
	
	/** Parse the string to generate the item state */
	public static ItemState getState( final String description ) {
		if ( description == null || description == "" ) {
			return DEFAULT_STATE;
		}
		
		boolean included = true;	// include items by default unless otherwise directed
		
		final String[] stateKeys = Util.getTokens( description );
		for ( int index = 0 ; index < stateKeys.length ; index++ ) {
			final String stateKey = stateKeys[index];
			
			if ( stateKey.equals( "included" ) ) {
				included = true;
			}
			else if ( stateKey.equals( "excluded" ) ) {
				included = false;
			}
		}
		
		return new ItemState( included );
	}
	
	
	/** Determine if the item is included */
	public boolean isIncluded() {
		return _included;
	}
}
