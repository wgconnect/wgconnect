/*
 * Copyright 2024 wgdhcp@protonmail.com. All Rights Reserved.
 *
 * WgDhcp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgDhcp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WireguardDhcp.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * CircularQueue
 *
 * The CircularQueue class.
 * 
 * @author: wgconnect@proton.me
 */
public class CircularQueue<T> {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(CircularQueue.class);

    private int front;
    private int rear;
    private final List<T> queueArray;

    public CircularQueue(T... elements) {
        front = 0;
        rear = -1;
        queueArray = new ArrayList<>();
        
        for (T element : elements) {
            enqueue(element);
        }
    }
    
    public void enqueue(T element) {
        queueArray.add(++rear, element);
    }
    
    public T dequeue() {
        T element = null;
        if (!queueArray.isEmpty()) {
            element = (T) queueArray.get(front);
            front = (front + 1) % queueArray.size();
        }
        
        return element;
    }

    public T peek() {
        T element = null;
        if (!queueArray.isEmpty()) {
            element = (T) queueArray.get(front);
        }
        
        return element;
    }
    
    public T remove(int index) {
        return queueArray.remove(index);
    }
    
    public boolean isEmpty() {
        return queueArray.isEmpty();
    }

    public void clear() {
        queueArray.clear();
    }
}
