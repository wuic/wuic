/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * •   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.capgemini.web.wuic.engine.impl.embedded;

import com.capgemini.web.wuic.engine.DimensionPacker;
import com.capgemini.web.wuic.engine.Region;

import java.awt.Dimension;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * <p>
 * This packer is inspired from the growser bin packing algorithm explained by
 * Jakes Gordon to give a position to a set of dimensions in the lowest area.  
 * </p>
 * 
 * <p>
 * {@code Comparator} interface is implemented to guarantee that dimensions will
 * be packed beginning with the biggest one (width * height).
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.2.3
 */
public class CGBinPacker<T> implements Comparator<T>, DimensionPacker<T> {

    /**
     * The root node with dimensions computed from the sum of heights and width
     * of given dimensions.
     */
    private Node<T> root;
    
    /**
     * All data with their dimensions.
     */
    private Map<T, Dimension> dataMap;
    
    /**
     * <p>
     * Builds a new packer.
     * </p>
     */
    public CGBinPacker() {
        /*
         * We use a LinkedHashMap to guarantee that the element will be added
         * always in the same order in the priority queue
         */
        dataMap = new LinkedHashMap<T, Dimension>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addElement(final Dimension dimension, final T data) {
        dataMap.put(data, dimension);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Region, T> getRegions() {
        // We order the elements from the biggest dimension to the smallest one
        final PriorityQueue<T> dataQueue = new PriorityQueue<T>(dataMap.size(), this);
        dataQueue.addAll(dataMap.keySet());
    
        // There is one region for each dimension
        final Map<Region, T> retval = new HashMap<Region, T>(dataMap.size());
        
        // Builds a root with the dimensions of the biggest element
        Dimension dim = dataMap.get(dataQueue.peek());
        root = new Node<T>(0, 0, (int) dim.getWidth(), (int) dim.getHeight(), null);
        
        for (T data : dataQueue) {
            dim = dataMap.get(data);
            
            // Finds the node with the appropriate dimensions
            final Node<T> find = this.findNode(this.root, dim); 
            final Node<T> insert;
            
            // The node has been found, insert it in the tree
            if (find != null) {
                insert = this.splitNode(find, dim, data);
            // No place found, grow the space and then insert the element
            } else {
                insert = this.growNode(dim, data);
            }
            
            retval.put(new Region(insert.getxPosition(), insert.getyPosition(), dim), data);
        }
        
        return retval;
    }

    /**
     * <p>
     * Finds the node contained in the given node with the specified dimensions.
     * </p>
     * 
     * @param node the node to traverse
     * @param dim the desired dimension
     * @return the found node, {@code null} if nothing has been found
     */
    private Node<T> findNode(final Node<T> node, final Dimension dim) {
        
        // Node filled, search on the left and on the right
        if (node.isFilled()) {
            final Node<T> right = this.findNode(node.right, dim);

            if (right == null) {
                return this.findNode(node.left, dim);
            } else {
                return right;
            }
        // Node not filled with the appropriate dimensions
        } else if ((dim.width <= node.width) && (dim.height <= node.height)) {
            return node;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Splits the node into the right and into the left with the given position
     * and filled it with the given data.
     * </p>
     * 
     * @param node the node to split
     * @param dim the dimension to use to split
     * @param data the data that actually filled the given node
     * @return the split node
     */
    private Node<T> splitNode(final Node<T> node, final Dimension dim, final T data) {
        final int x = node.getxPosition();
        final int y = node.getyPosition();
        node.data = data;
        node.left = new Node<T>(x, y + dim.height, node.width, node.height - dim.height, null);
        node.right = new Node<T>(x + dim.width, y, node.width - dim.width, dim.height, null);
        
        return node;
    }

    /**
     * <p>
     * Grow the root node with the given dimensions.
     * </p>
     * 
     * @param dim the dimension to use when growing
     * @param data the data to be filled in a node after the root has grown
     * @return the node which has the data filled
     */
    private Node<T> growNode(final Dimension dim, final T data) {
        // Grow on the right
        if (this.root.height >= (this.root.width + dim.width)) {
            return this.growRight(dim, data);
        // Grow on the left
        } else if (this.root.width >= (this.root.height + dim.height)) {
            return this.growDown(dim, data);
        // Should never occurs since the root node has been initialized with a starting size
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Grow on the right.
     * </p>
     * 
     * @param dim the dimension to use when growing
     * @param data the data to associate to the node that match the given dimension
     * @return the filled node
     */
    private Node<T> growRight(final Dimension dim, final T data) {
        final Node<T> left = this.root;
        this.root = new Node<T>(0, 0, this.root.width + dim.width, this.root.height, data);
        this.root.right = new Node<T>(left.width, 0, dim.width, left.height, null);
        this.root.left = left;

        final Node<T> node = this.findNode(this.root, dim);

        if (node != null) {
            return this.splitNode(node, dim, data);
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Grow on the left.
     * </p>
     * 
     * @param dim the dimension to use when growing
     * @param data the data to associate to the node that match the given dimension
     * @return the filled node
     */
    private Node<T> growDown(final Dimension dim, final T data) {
        final Node<T> right = this.root;
        this.root = new Node<T>(0, 0, this.root.width, this.root.height + dim.height, data);
        this.root.right = right;
        this.root.left = new Node<T>(0, right.height, right.width, dim.height, null);

        final Node<T> node = this.findNode(this.root, dim);

        if (node != null) {
            return this.splitNode(node, dim, data);
        } else {
            return null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getFilledArea() {
        return this.root.getFilledArea();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final T a, final T b) {
        final Dimension dimA = dataMap.get(a);
        final Dimension dimB = dataMap.get(b);
        
        return (dimB.width * dimB.height) - (dimA.width * dimA.height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearElements() {
        this.dataMap.clear();
    }
    
    /**
     * <p>
     * Inner class that represents the node used to build the tree which keep in
     * mind the already used and the available regions.
     * </p>
     * 
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.2.3
     * @param <D> the type of node
     */
    private class Node<D extends T> extends Region {
        
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = -8713746485598635503L;

        /**
         * The left region.
         */
        private Node<D> left;
        
        /**
         * The right region.
         */
        private Node<D> right;
        
        /**
         * The data associated to the region.
         */
        private D data;
        
        /**
         * <p>
         * Builds a new node.
         * </p>
         * 
         * @param dimension the node's dimension
         * @param d the associated data
         */
        private Node(final Dimension dimension, final D d) {
            this(0, 0, (int) dimension.getWidth(), (int) dimension.getHeight(), d);
        }

        /**
         * <p>
         * Builds a new node.
         * </p>
         * 
         * @param x the x position
         * @param y the y position
         * @param w the width
         * @param h the height
         * @param d the data
         */
        private Node(final int x, final int y, final int w, final int h, final D d) {
            super(x, y, w, h);
            this.data = d;
            left = null;
            right = null;
        }
        
        /**
         * <p>
         * Indicates if this region is filled i.e the data is not {@code null}.
         * </p>
         * 
         * @return {@code true} if the data is filled, {@code false} otherwise
         */
        private Boolean isFilled() {
            return data != null;
        }
  
        /**
         * <p>
         * Gets the bottom side (y + height).
         * </p>
         * 
         * @return the bottom side
         */
        private int getBottomSide() {
            return getyPosition() + (int) getHeight();
        }
        
        /**
         * <p>
         * Gets the right side (x + width).
         * </p>
         * 
         * @return the bottom side
         */
        private int getRightSide() {
            return getxPosition() + (int) getWidth();
        }
        
        /**
         * <p>
         * Returns the area where we found all the filled nodes.
         * </p>
         * 
         * @return the dimensions calculated from the filled nodes
         */
        private Dimension getFilledArea() {
            final Dimension retval;
            
            // This node is filled, returns its area
            if (isFilled()) {
                retval = new Dimension(getRightSide(), getBottomSide());
            } else {
                
                // The node is not filled but maybe it is the case for its children !
                if (left != null) {
                    final Dimension dimLeft = left.getFilledArea();
                    final Dimension dimRight = right.getFilledArea();
                    
                    // Merge biggest heights and widths of the two children
                    retval = new Dimension(
                            dimLeft.width > dimRight.width ? dimLeft.width : dimRight.width,
                            dimLeft.height > dimRight.height ? dimLeft.height : dimRight.height);
                } else {
                    // Not filled, no children => (0, 0)
                    retval = new Dimension();
                }
            }
            
            return retval;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Node[" + getxPosition() + ", " + getyPosition() + ", " + width + ", " + height + "]";
        }
    }
}
