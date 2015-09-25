package org.csstudio.mps.sns.tools.swing;


/**
 * "Pipeline" insert for TableModel, implementing sorted columns.
 * Based on the Sun example, but no longer split into TableMap and
 * TableSorter.
 * It also maps more of the TableModel methods.
 *
 * A sorter for TableModels. The sorter has a model (conforming to TableModel) 
 * and itself implements TableModel. TableSorter does not store or copy 
 * the data in the TableModel, instead it maintains an array of 
 * integers which it keeps the same size as the number of rows in its 
 * model. When the model changes it notifies the sorter that something 
 * has changed eg. "rowsAdded" so that its internal array of integers 
 * can be reallocated. As requests are made of the sorter (like 
 * getValueAt(row, col) it redirects them to its model via the mapping 
 * array. That way the TableSorter appears to hold another copy of the table 
 * with the rows in a different order. The sorting algorthm used is stable 
 * which means that it does not move around rows when its comparison 
 * function returns 0 to denote that they are equivalent. 
 *
 * @version 1.5 12/17/97
 * @author Philip Milne
 */

import java.awt.Cursor;
import java.util.*;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

// Imports for picking up mouse events from the JTable. 
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import javax.swing.JTable;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.event.TableModelListener;
import java.util.Date;
import java.io.PrintStream;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import java.util.Arrays;

public class TableSorter extends javax.swing.table.AbstractTableModel
    implements TableModelListener
{
    protected TableModel model; 

    int     indexes[];
    int     sortingColumn;
    boolean ascending = true;
    
    public TableSorter(TableModel model)
    {
      setModel(model);
    }

    /**
     * Provides a way of switching out the model for the sorter.
     * 
     * @param model
     */
    public void setModel(TableModel model)
    {
      if(this.model != null)
        this.model.removeTableModelListener(this);
      this.model = model; 
      model.addTableModelListener(this); 
      reallocateIndexes(); 
    }
    
    // By default, implement TableModel by forwarding all messages 
    // to the model. 
    public Class getColumnClass(int aColumn)
    {
        return model.getColumnClass(aColumn); 
    }
    
    public int getColumnCount()
    {
        return model.getColumnCount(); 
    }
    
    public String getColumnName(int aColumn)
    {
        return model.getColumnName(aColumn); 
    }

    public int getRowCount()
    {
        return model.getRowCount(); 
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".
    public Object getValueAt(int aRow, int aColumn)
    {
        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    public boolean isCellEditable(int row, int column)
    { 
         return model.isCellEditable(indexes[row], column); 
    }
           
    public void setValueAt(Object aValue, int aRow, int aColumn)
    {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }
    
    // Sorting
    public int compareRowsByColumn(int row1, int row2, int column)
    {
        Class type = model.getColumnClass(column);
        TableModel data = model;
        
        // Check for nulls.
        Object o1 = data.getValueAt(row1, column);
        Object o2 = data.getValueAt(row2, column); 
        
        // If both values are null, return 0.
        // Define null less than everything. 
        if (o1 == null && o2 == null)
            return 0; 
        else if (o1 == null)
            return -1; 
        else if (o2 == null)
            return 1; 

        /*
         * We copy all returned values from the getValue call in case
         * an optimised model is reusing one object to return many
         * values.  The Number subclasses in the JDK are immutable and
         * so will not be used in this way but other subclasses of
         * Number might want to do this to save space and avoid
         * unnecessary heap allocation.
         */
        if (type.getSuperclass() == java.lang.Number.class)
        {
            Number n1 = (Number)data.getValueAt(row1, column);
            double d1 = n1.doubleValue();
            Number n2 = (Number)data.getValueAt(row2, column);
            double d2 = n2.doubleValue();

            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            else
                return 0;
        }
        else if (type == java.util.Date.class)
        {
            Date d1 = (Date)data.getValueAt(row1, column);
            long n1 = d1.getTime();
            Date d2 = (Date)data.getValueAt(row2, column);
            long n2 = d2.getTime();

            if (n1 < n2)
                return -1;
            else if (n1 > n2)
                return 1;
            else
                return 0;
        }
        else if (type == String.class)
        {
            String s1 = (String)data.getValueAt(row1, column);
            String s2    = (String)data.getValueAt(row2, column);
            int result = s1.compareTo(s2);

            if (result < 0)
                return -1;
            else if (result > 0)
                return 1;
            else
                return 0;
        }
        else
        {   // Sort everything else as a String
            // This makes Boolean false < true
            Object v1 = data.getValueAt(row1, column);
            String s1 = v1.toString();
            Object v2 = data.getValueAt(row2, column);
            String s2 = v2.toString();
            int result = s1.compareTo(s2);

            if (result < 0)
                return -1;
            else if (result > 0)
                return 1;
            else
                return 0;
        }
    }

    public int compare (int row1, int row2)
    {
        int result = compareRowsByColumn(row1, row2, sortingColumn);
        return ascending ? result : -result;
    }

    public void reallocateIndexes()
    {
        int rowCount = model.getRowCount();

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[rowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < rowCount; row++) {
            indexes[row] = row;
        }
    }

    public void tableChanged(TableModelEvent e)
    {
        //System.out.println("Sorter: tableChanged"); 
        if (indexes.length != model.getRowCount())
        {
            reallocateIndexes();
        }
        fireTableChanged(e);
    }

    public void checkModel()
    {
        if (indexes.length != model.getRowCount())
            System.err.println("Sorter not informed of a change in model.");
    }

    public void sort ()
    {
        checkModel();
        //qsort(0, indexes.length-1);
        //sort (indexes, this);
        shuttlesort((int[])indexes.clone(), indexes, 0, indexes.length);
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    public void shuttlesort(int from[], int to[], int low, int high)
    {
        if (high - low < 2)
            return;

        int middle = (low + high)/2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-array can just be copied.  The array must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */
        if (high - low >= 4 && compare(from[middle-1], from[middle]) <= 0)
        {
            for (int i = low; i < high; i++)
            {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge. 

        for (int i = low; i < high; i++)
        {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0))
                to[i] = from[p++];
            else
                to[i] = from[q++];
        }
    }

    public void swap(int i, int j)
    {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    // There is no-where else to put this. 
    // Add a mouse listener to the Table to trigger a table sort 
    // when a column heading is clicked in the JTable. 
    public void addMouseListenerToHeaderInTable(JTable _table)
    { 
        final TableSorter sorter = this; 
        final JTable table = _table; 
        table.setColumnSelectionAllowed(false); 
        MouseAdapter listMouseListener = new MouseAdapter()
            {
                public void mouseClicked(MouseEvent e)
                {
                    TableColumnModel columnModel = table.getColumnModel();
                    int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
                    int column =
                        table.convertColumnIndexToModel(viewColumn); 
                    if (e.getClickCount() == 1 && column != -1)
                      try
                      {
                          //System.out.println("Sorting ..."); 
                          table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                          int shiftPressed =
                              e.getModifiers()&InputEvent.SHIFT_MASK; 
                          sorter.ascending = (shiftPressed == 0);
                          sorter.sortingColumn = column;
                          sorter.sort();
                      }
                      finally
                      {
                        table.setCursor(Cursor.getDefaultCursor());
                      }
                }
            };
        JTableHeader th = table.getTableHeader(); 
        th.addMouseListener(listMouseListener); 
    }

    /**
     * Returns the index of the row in the table.
     * 
     * @param tableRow The index of the row in the table.
     * @return The index of the row in the model
     */
    public int getModelRowNumber(int tableRow)
    {
      return indexes[tableRow];
    }

    /**
     * Returns the index of the row in the table.
     * 
     * @param modelRow The index of the row in the model.
     * @return The index of the row in the table, or a negative number if not found.
     */
    public int getTableRowNumber(int modelRow)
    {
      for(int i=0;i<indexes.length;i++) 
        if(indexes[i] == modelRow)
          return i;
      return -1;
    }

    /**
     * Returns the <CODE>TableModel</CODE> that was used to create the 
     * <CODE>TableSorter</CODE>.
     * 
     * @return The <CODE>TableModel</CODE> that holds the data.
     */
    final public TableModel getModel()
    {
      return model;
    }
}
