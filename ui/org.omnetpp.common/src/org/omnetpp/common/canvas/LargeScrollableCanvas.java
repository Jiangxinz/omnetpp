package org.omnetpp.common.canvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A canvas that supports a scrollable area which can be very large,
 * as the area's width/height are stored as 64-bit numbers. This class
 * overcomes the scrollbar's 32-bit limit.
 * 
 * @author andras
 */
public abstract class LargeScrollableCanvas extends Canvas {
	
	private long virtualWidth, virtualHeight; // 64-bit size of the "virtual canvas"
	private long viewX, viewY; // 64-bit coordinates of top-left corner of the viewport
	private int hShift, vShift; // used for scrollbar mapping 

	public LargeScrollableCanvas(Composite parent, int style) {
		super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL);
		init();
	}

	private void init() {
		getHorizontalBar().setIncrement(10);
		getVerticalBar().setIncrement(10);
		
		getHorizontalBar().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				horizontalBarChanged();
			}
		});
		getVerticalBar().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				verticalBarChanged();
			}
		});
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e.gc);
			}
		});
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				configureScrollbars();
			}
		});
	}

	/**
	 * Override this to do your own drawing. 
	 */
	protected abstract void paint(GC graphics);

	public void setVirtualSize(long width, long height) {
		this.virtualWidth = width;
		this.virtualHeight = height;
		configureScrollbars();
		redraw();
	}

	public long getVirtualHeight() {
		return virtualHeight;
	}

	public void setVirtualHeight(long height) {
		this.virtualHeight = height;
		configureScrollbars();
		redraw();
	}

	public long getVirtualWidth() {
		return virtualWidth;
	}

	public void setVirtualWidth(long width) {
		this.virtualWidth = width;
		configureScrollbars();
		redraw();
	}

	public long getViewportLeft() {
		return viewX;
	}

	/**
	 * Sets viewportLeft.
	 */
	public void scrollHorizontalTo(long x) {
		this.viewX = clipX(x);
		adjustScrollbars();
		redraw();
	}

	public long getViewportTop() {
		return viewY;
	}

	/**
	 * Sets viewportTop.
	 */
	public void scrollVerticalTo(long y) {
		this.viewY = clipY(y);
		adjustScrollbars();
		redraw();
	}
	
	private long clipX(long x) {
		return clip(x, virtualWidth-getViewportWidth());
	}
	
	private long clipY(long y) {
		return clip(y, virtualHeight-getViewportHeight());
	}

	/**
	 * Utility function to clip x to (0, upperBound). If upperBound<0, 0 is returned.
	 */
	private static long clip(long x, long upperBound) {
		if (x > upperBound) 
			x = upperBound;  // if negative, we'll fix it below
		if (x < 0) 
			x = 0;
		return x;
	}

	/**
	 * Returns the width of the client area (see {@link Canvas#getClientArea()}.
	 */
	public int getViewportWidth() {
		return getClientArea().width;
	}

	/**
	 * Returns the height of the client area (see {@link Canvas#getClientArea()}.
	 */
	public int getViewportHeight() {
		return getClientArea().height;
	}
	
	private void horizontalBarChanged() {
		viewX = clipX(((long)getHorizontalBar().getSelection()) << hShift);
		redraw();
	}

	private void verticalBarChanged() {
		viewY = clipY(((long)getVerticalBar().getSelection()) << vShift);
		redraw();
	}
	
	private void configureScrollbars() {
		viewX = clipX(viewX);
		viewY = clipY(viewY);
		hShift = configureScrollbar(getHorizontalBar(), virtualWidth, viewX, /*getSize().x*/getViewportWidth());
		vShift = configureScrollbar(getVerticalBar(), virtualHeight, viewY, /*getSize().y*/getViewportHeight());
	}

	private int configureScrollbar(ScrollBar sb, long virtualSize, long virtualPos, int widgetSize) {
		if (widgetSize >= virtualSize) {
			// hide scrollbar when not needed
			sb.setVisible(false);
			return 0;
		}
		else {
			// show scrollbar
			sb.setVisible(true);
			
			// count how many bits we need to shift to fit into 32 bits
			int shift = 0;
			while (virtualSize>>shift > Integer.MAX_VALUE) 
				shift++;

			int newMax = (int)(virtualSize>>shift);
			int newThumb = (int)(widgetSize >> shift);
			int newSel = (int)(virtualPos >> shift);
			int newPageIncr = (int)((widgetSize>>shift > 0) ? widgetSize>>shift : 1);
				
			sb.setMinimum(0);
			sb.setMaximum(newMax);
			sb.setThumb(newThumb);
			sb.setSelection(newSel);
			sb.setPageIncrement(newPageIncr);
			return shift;
		}
	}

	private void adjustScrollbars() {
		getHorizontalBar().setSelection((int)(viewX >> hShift));
		getVerticalBar().setSelection((int)(viewY >> vShift));
	}
}
