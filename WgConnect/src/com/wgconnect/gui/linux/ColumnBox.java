/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
 *
 * WgConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WireguardDhcp.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.gui.linux;

import com.wgconnect.core.util.WgConnectLogger;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * ColumnBox
 *
 * The ColumnBox class.
 * 
 * @author: wgconnect@proton.me
 */
public class ColumnBox extends VBox {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(ColumnBox.class);

    public static final int VBOX_ELEMENT_SPACING = 2;
    public static final String TEXT_FONT_STYLE = "Serif";
    public static final int TEXT_FONT_SIZE = 14;

    private static final Paint rowSeparatorColor = Color.TRANSPARENT;
    private static final int ROW_SEPARATOR_HEIGHT = 10;
    
    private final int column;

    private final HBox hBox;

    public ColumnBox(HBox hBox, int column) {
        super(VBOX_ELEMENT_SPACING);
        
        setAlignment(Pos.CENTER);
        this.hBox = hBox;

        this.column = column;

        setId(hBox.getId());
    }
    
    public void addElement(ElementBox element, int index) {
        getChildren().add(index, element);
    }
    
    public void addRowSeparator(Node separator) {
        getChildren().add(separator);
    }
    
    public void addElements(ElementBox... elements) {
        for (ElementBox e : elements) {
           getChildren().add(e);
           Line line = new Line(0, 0, e.getHeaderLabel().getWidth(), ROW_SEPARATOR_HEIGHT);
           line.setFill(rowSeparatorColor);
           getChildren().add(line);
        }
    }
    
    public int getColumn() {
        return column;
    }
    
    public HBox getBox() {
        return hBox;
    }
}
