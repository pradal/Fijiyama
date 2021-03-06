package com.vitimage.fijiyama;
//TODO : common
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JList;

/**Original class from adamski on stack overflow :
* https://stackoverflow.com/questions/2544758/how-to-adjust-position-of-scroll-in-the-scrollpane
*/
public final class ScrollUtil {
    public static final int NONE = 0, TOP = 1, VCENTER = 2, BOTTOM = 4, LEFT = 8, HCENTER = 16, RIGHT = 32,SELECTED = 64;
    private static final int OFFSET = 0;// Required for hack (see below).100; 

    private ScrollUtil() {
    }

    /**
     * Scroll to specified location.  e.g. <tt>scroll(component, BOTTOM);</tt>.
     *
     * @param c JComponent to scroll.
     * @param part Location to scroll to.  Should be a bit-wise OR of one or moe of the values:
     * NONE, TOP, VCENTER, BOTTOM, LEFT, HCENTER, RIGHT.
     */
    public static void scroll(JComponent c, int part,int []opts) {
        scroll(c, part & (LEFT|HCENTER|RIGHT), part & (TOP|VCENTER|BOTTOM|SELECTED),opts);
    }

    /**
     * Scroll to specified location.  e.g. <tt>scroll(component, LEFT, BOTTOM);</tt>.
     *
     * @param c JComponent to scroll.
     * @param horizontal Horizontal location.  Should take the value: LEFT, HCENTER or RIGHT.
     * @param vertical Vertical location.  Should take the value: TOP, VCENTER or BOTTOM.
     */
    public static void scroll(JComponent c, int horizontal, int vertical,int[]opts) {
        Rectangle visible = c.getVisibleRect();
        Rectangle bounds = c.getBounds();
//        System.out.println("\nUpdating scrolling");
        //        System.out.println("Bounds="+bounds.getSize());
        //System.out.println("Visible old="+visible.getSize());
        //System.out.println("Visible.y old="+visible.y);
        //System.out.println("Opt="+opts[0]+", "+opts[1]);
        switch (vertical) {
            case TOP:     visible.y = 0; break;
            case VCENTER: visible.y = (bounds.height - visible.height) / 2; break;
            case BOTTOM:  visible.y = bounds.height - visible.height + OFFSET; break;
            case SELECTED:  visible.y = (int)((bounds.height*1.0*opts[0])/opts[1]) - visible.height/2 + OFFSET; break;
       }
        visible.y=Math.max(visible.y, 0);
        //System.out.println("Visible new="+visible.getSize());
        //System.out.println("Visible.y old="+visible.y);
        
        
        switch (horizontal) {
            case LEFT:    visible.x = 0; break;
            case HCENTER: visible.x = (bounds.width - visible.width) / 2; break;
            case RIGHT:   visible.x = bounds.width - visible.width + OFFSET; break;
        }
        // When scrolling to bottom or right of viewport, add an OFFSET value.
        // This is because without this certain components (e.g. JTable) would
        // not scroll right to the bottom (presumably the bounds calculation
        // doesn't take the table header into account.  It doesn't matter if
        // OFFSET is a huge value (e.g. 10000) - the scrollRectToVisible method
        // still works correctly.

        c.scrollRectToVisible(visible);
    }
}