package org.omnetpp.scave.charting;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.omnetpp.common.canvas.RubberbandSupport;

/**
 * Adds mouse bindings to a ZoomableCachingCanvas for zooming and panning.
 * 
 * There are two operation modes that can be selected with setMouseMode(): 
 * panning (aka "hand") and zoom.
 *
 * Bindings in pan mode:
 *   - mouse drag: panning
 *   - wheel: vertical scroll
 *   - shift+wheel: horizontal scroll
 *   - ctrl+drag: zoom to rectangle dragged out
 *   - ctrl+wheel: zoom in/out
 *   - ctrl+leftclick: zoom in
 *   - ctrl+shift+leftclick: zoom in
 *
 * Bindings in zoom mode:
 *   - drag: zoom to rectangle dragged out
 *   - wheel: zoom in/out
 *   - leftclick: zoom in
 *   - shift+leftclick: zoom out
 *   - ctrl+drag: panning
 *   
 * Note that the opposite mode's bindings are always available by 
 * holding down the ctrl key.
 * 
 * @author Andras
 */
public class CanvasZoomSupport {
	// the adapted canvas
	protected ZoomableCachingCanvas canvas;

	// mouse pointers
	protected static final Cursor PAN_CURSOR = new Cursor(null, SWT.CURSOR_HAND);
	protected static final Cursor ZOOM_CURSOR = new Cursor(null, SWT.CURSOR_SIZEALL);

	protected RubberbandSupport rubberBand;
	
	public static final int PAN_MODE = 0;
	public static final int ZOOM_MODE = 1;
	private int mouseMode;

	// remembers the previous mouse position during dragging
	private int dragPrevX;
	private int dragPrevY;
	private boolean mousedMoved; 

    /**
     * Sets up mouse bindings on the given canvas.
     */
	public CanvasZoomSupport(final ZoomableCachingCanvas canvas) {
		this.canvas = canvas;
		rubberBand = new RubberbandSupport(canvas, 0) {
			public void rubberBandSelectionMade(Rectangle r) {
				canvas.zoomToRectangle(new org.eclipse.draw2d.geometry.Rectangle(r));
			}
		};
		setupMouseHandling();
		setMouseMode(ZOOM_MODE);
	}
    
	public int getMouseMode() {
		return mouseMode;
	}

	public void setMouseMode(int mouseMode) {
		this.mouseMode = mouseMode;
		rubberBand.setModifierKeys(mouseMode==ZOOM_MODE ? SWT.NONE : SWT.CTRL);
	}

	protected void setupMouseHandling() {
		// wheel
		canvas.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int modifier = event.stateMask & SWT.MODIFIER_MASK;
				if ((mouseMode==ZOOM_MODE && modifier==SWT.NONE) || (mouseMode==PAN_MODE && modifier==SWT.CTRL)) {
					// zoom in/out
					for (int i = 0; i < event.count; i++)
						canvas.zoomBy(1.1);
					for (int i = 0; i < -event.count; i++)
						canvas.zoomBy(1.0 / 1.1);
				}
				else if (modifier==SWT.SHIFT) {
					// if not zooming: shift+wheel does horizontal scroll
					canvas.scrollHorizontalTo(canvas.getViewportLeft() - canvas.getViewportWidth() * event.count / 20);
				}
			}
		});
		
		// mouse button down / up
		canvas.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent event) {}
			public void mouseDown(MouseEvent event) {
				canvas.setFocus();
				if (event.button == 1) {
					canvas.setCursor(mouseMode==ZOOM_MODE ? ZOOM_CURSOR : PAN_CURSOR);
					dragPrevX = event.x;
					dragPrevY = event.y;
					mousedMoved = false;
				}
			}
			public void mouseUp(MouseEvent event) {
				canvas.setCursor(null); // restore cursor at end of drag
				dragPrevX = dragPrevY = -1;
				if (!mousedMoved) {  // just a click
					int modifier = event.stateMask & SWT.MODIFIER_MASK;
					if (event.button==1) {
						if ((mouseMode==ZOOM_MODE && modifier==SWT.NONE) || (mouseMode==PAN_MODE && modifier==SWT.CTRL))
							canvas.zoomBy(1.5); //zoom in  FIXME about the click position!
						if ((mouseMode==ZOOM_MODE && modifier==SWT.SHIFT) || (mouseMode==PAN_MODE && modifier==(SWT.CTRL|SWT.SHIFT)))
							canvas.zoomBy(1/1.5); //zoom out   FIXME about the click position!
					}
				}
			}
    	});

		// dragging ("hand" cursor)
		canvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent event) {
				int modifier = event.stateMask & SWT.MODIFIER_MASK;
System.out.println(event.button); //FIXME problem: event.button is always zero!!!! we don't get the event properly!!!
				if (event.button==1) { // drag with left mouse button being held down
					if ((mouseMode==PAN_MODE && modifier==SWT.NONE) || (mouseMode==ZOOM_MODE && modifier==SWT.CTRL)) {
						doPanning(event);
					}
					mousedMoved = true;
				} 
				else if (event.button==0) { // plain mouse move (no mouse button pressed) 
					// restore cursor at end of drag. (It is not enough to do it in the 
					// "mouse released" event, because we don't receive it if user 
					// releases mouse outside the canvas!)
					canvas.setCursor(null);  
				}
			}

			private void doPanning(MouseEvent e) {
				// drag the chart
				if (dragPrevX!=-1 && dragPrevY!=-1) {
					// scroll by the amount moved since last drag call
					int dx = e.x - dragPrevX;
					int dy = e.y - dragPrevY;
					canvas.scrollHorizontalTo(canvas.getViewportLeft() - dx);
					canvas.scrollVerticalTo(canvas.getViewportTop() - dy);
					dragPrevX = e.x;
					dragPrevY = e.y;
				}
			}
		});
	}

}
