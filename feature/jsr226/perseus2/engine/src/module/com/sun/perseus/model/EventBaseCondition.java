/*
 * $RCSfile: EventBaseCondition.java,v $
 *
 * Copyright  1990-2009 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation. 
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt). 
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA 
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions. 
 */
package com.sun.perseus.model;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

/**
 * An <code>EventBaseCondition</code> generates a <code>TimeInstance</code>
 * everytime the associated event happens. 
 *
 * <p>It is the responsibility of this class to register as an 
 * <code>EventListener</code>. The <code>TimeInstance</code> created
 * by an <code>EventBaseCondition</code> are cleared on reset, i.e, when
 * the associated <code>TimedElementSupport</code> restarts.
 *
 * @version $Id: EventBaseCondition.java,v 1.3 2006/06/29 10:47:31 ln156897 Exp $
 */
public class EventBaseCondition extends TimeCondition 
        implements EventListener, IDRef {
    /**
     * Offset from the event base
     */
    long offset;

    /**
     * Keeps a reference to the last occurence of the event.
     */
    Time lastEventTime = Time.UNRESOLVED;

    /**
     * The event base, i.e., the element which generates events this
     * listeners listens to.
     */
    ModelNode eventBase;

    /**
     * The id of the event base. If null, the event base is the timed
     * element of this condition.
     */
    String eventBaseId;

    /**
     * The type of event this listener listens to.
     */
    String eventType;

    /**
     * @param timedElement the associated <code>TimedElementSupport</code>. 
     *        Should not be null.
     * @param isBegin defines whether this condition is for a begin list.
     * @param eventBaseId the id of the element which generates events this
     *        listener listens to. If null, this means the events are 
     *        generated by the timedElement itself.
     * @param eventType the type of event this listener listens to. Should
     *        not be null.
     * @param offset offset from the sync base. This means that time instances
     *        synchronized on the syncBase begin or end time are offset by 
     *        this amount.
     * @throws IllegalArgumentException if eventType is null or if the 
     *         <code>ModelNode</code> associated with the 
     *         <code>timedElement</code> is null.
     */
    public EventBaseCondition(final TimedElementSupport timedElement,
                              final boolean isBegin,
                              final String eventBaseId,
                              final String eventType,
                              final long offset) {
        this(timedElement, 
             isBegin, 
             eventBaseId, 
             timedElement.animationElement, 
             eventType, 
             offset);
    }

    /**
     * @param timedElement the associated <code>TimedElementSupport</code>. 
     *        Should not be null.
     * @param isBegin defines whether this condition is for a begin list.
     * @param eventBaseId the id of the element which generates events this
     *        listener listens to. If null, this means the events are 
     *        generated by the timedElement itself.
     * @param eventBase in case eventBaseId is null, this should be used as 
     *        the source for generating events. If eventBaseId is null, this
     *        should not be null.
     * @param eventType the type of event this listener listens to. Should
     *        not be null.
     * @param offset offset from the sync base. This means that time instances
     *        synchronized on the syncBase begin or end time are offset by 
     *        this amount.
     * @throws IllegalArgumentException if eventType is null or if eventBase
     *         and eventBaseId are null.
     */
    protected EventBaseCondition(final TimedElementSupport timedElement,
                                 final boolean isBegin,
                                 final String eventBaseId,
                                 final ModelNode eventBase,
                                 final String eventType,
                                 final long offset) {
        super(timedElement, isBegin);

        if (eventType == null
            ||
            (eventBaseId == null && eventBase == null)) {
            throw new IllegalArgumentException();
        }

        this.eventBaseId = eventBaseId;
        this.eventType = eventType;
        this.offset = offset;

        ModelNode elt = timedElement.animationElement;
        if (eventBaseId == null) {
            setEventBase(eventBase);
        } else {
            elt.ownerDocument.resolveIDRef(this, eventBaseId);
        }
    }

    /**
     * Implementation helper.
     *
     * @param eventBase this node's event base.
     */
    private void setEventBase(final ModelNode eventBase) {
        if (this.eventBase != null) {
            throw new IllegalStateException();
        }

        this.eventBase = eventBase;

        // Register the listener for the bubble phase.
        eventBase.ownerDocument.getEventSupport().addEventListener
            (eventBase, eventType, EventSupport.BUBBLE_PHASE, this);
    }

    /**
     * <code>IDRef</code> implementation.
     *
     * @param ref the resolved reference (from eventBaseId).
     */
    public void resolveTo(final ElementNode ref) {
        setEventBase(ref);
    }

    /**
     * Returns the id that is referenced by this <code>IDRef</code>.
     *
     * @return the idRef.
     */
     public String getIdRef() {
         return eventBaseId;
     }
    
    /**
     * Implementation of the <code>EventListener</code> interface.
     * When an event is received from the event base, this condition
     * should generate a new <code>TimeInstance</code> that will be added
     * to the associdated <code>TimedElementSupport</code> begin or end instance
     * list (depending on the <code>isBegin</code> setting). The condition
     * should also record the time of the new event as its 
     * <code>lastEventTime</code>.
     *
     * <p>Note that a condition is sensitive to events according to the
     * SMIL Animation specification, section 3.6.4 specifying 'Event
     * Sensitivity' (or SMIL 2 Timing and Synchronization Module, 
     * 'Event Sensitivity').</p>
     *
     * @param evt the event that occured
     */
    public void handleEvent(final Event evt) {
        // =====================================================================
        // If the container is not active, there is no event sensitivity
        // If the event time is unresolved, there is no event sensitivy either,
        // as there is no way to compute the time instance that should be
        // added to the timed element.
        if (timedElement.timeContainer.state 
              != 
            TimedElementSupport.STATE_PLAYING
            ||
            !((ModelEvent) evt).eventTime.isResolved()) {
            return;
        }

        // =====================================================================
        // When the container is active, begin condition are sensitive when:
        // - there is no current interval yet, i.e., the timedElement is not
        //   active.
        // - there is a current interval, and the restart behavior is 'always'
        //
        // When the container is active (i.e., there is a resolved currentTime),
        // end condition are sensitive when the element is active and the
        // restart behavior is never or when active and the restart behavior
        // is 'always' but there is no begin condition for the event.
        Time eventTime = new Time(((ModelEvent) evt).eventTime.value);
        eventTime = timedElement.toContainerSimpleTime(eventTime);
        if (timedElement.state != TimedElementSupport.STATE_PLAYING) {
            if (isBegin) {
                new TimeInstance(timedElement, 
                                 new Time(eventTime.value + offset),
                                 true,
                                 isBegin);
                lastEventTime = eventTime;
            } 
        } else {
            // The element is active
            if (timedElement.restart == TimedElementSupport.RESTART_ALWAYS) {
                // Only add an instance if this is a begin condition
                // of if there is not begin event condition
                if (isBegin || !timedElement.hasBeginCondition(this)) {
                    new TimeInstance(timedElement, 
                                     new Time(eventTime.value + offset),
                                     true,
                                     isBegin);
                    lastEventTime = eventTime;
                }
            } else {
                if (!isBegin) {
                    new TimeInstance(timedElement, 
                                     new Time(eventTime.value + offset),
                                     true,
                                     isBegin);
                    lastEventTime = eventTime;
                }
            }
        }
    }

    /**
     * Converts this <code>EventBaseCondition</code> to a String trait.
     *
     * @return a string describing this <code>TimeCondition</code>
     */
    protected String toStringTrait() {
        StringBuffer sb = new StringBuffer();

        sb.append(eventBaseId);
        sb.append('.');
        sb.append(eventType);

        if (offset != 0) {
            if (offset > 0) {
                sb.append('+');
            } 
            sb.append(offset / 1000f);
            sb.append('s');
        }

        return sb.toString();
    }


}