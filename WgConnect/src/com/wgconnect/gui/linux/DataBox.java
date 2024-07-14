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
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * DataBox
 *
 * The DataBox class.
 * 
 * @author: wgconnect@proton.me
 */
public class DataBox extends VBox {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(DataBox.class);

    private static final int VBOX_ELEMENT_SPACING = 2;
    private static final String TEXT_FONT_STYLE = "Serif";
    private static final int TEXT_FONT_SIZE = 14;

    private static final String BOX_STYLE =
        "-fx-padding: 2;" +
        "-fx-border-style: solid inside;" +
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" +
        "-fx-border-radius: 5;" +
        "-fx-border-color: black;";
    
    private final Label headerLabel;
    private final Label dataLabel;
    
    private final int column;
    private final int row;

    private final HBox hBox;

    public DataBox(HBox hBox, String headerStr, String dataStr, int column, int row) {
        super(VBOX_ELEMENT_SPACING);
        
        setAlignment(Pos.CENTER);
        this.hBox = hBox;

        headerLabel = new Label(headerStr);
        headerLabel.setFont(Font.font(TEXT_FONT_STYLE, FontWeight.EXTRA_BOLD, TEXT_FONT_SIZE));
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setUnderline(true);
        
        dataLabel = new Label(dataStr);
        dataLabel.setFont(Font.font(TEXT_FONT_STYLE, FontWeight.BOLD, TEXT_FONT_SIZE));
        dataLabel.setAlignment(Pos.CENTER);

        this.column = column;
        this.row = row;

        setId(headerStr);
        
        setStyle(BOX_STYLE);

        getChildren().addAll(headerLabel, dataLabel);
    }
    
    public void addToPane() {
        hBox.getChildren().add(this);        
    }
    
    public void setHeaderText(String headerStr) {
        headerLabel.setText(headerStr);
    }
    
    public String getHeaderText() {
        return headerLabel.getText();
    }
    
    public void setDataText(String dataStr) {
        dataLabel.setText(dataStr);
    }
    
    public String getDataText() {
        return dataLabel.getText();
    }
    
    public int getColumn() {
        return column;
    }
    
    public int getRow() {
        return row;
    }
    
    public HBox getBox() {
        return hBox;
    }
}
