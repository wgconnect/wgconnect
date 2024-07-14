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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.apache.commons.lang3.StringUtils;

/**
 * ElementBox
 *
 * The ElementBox class.
 * 
 * @author: wgconnect@proton.me
 */
public class ElementBox extends VBox {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(ElementBox.class);
    
    private static final String TEXT_FONT_STYLE = "Serif";
    private static final int TEXT_FONT_SIZE = 14;

    private final ColumnBox parentBox;
    private final Label headerLabel;
    private final Label dataLabel;

    private final String color;
    
    private static final String BOX_STYLE_COLOR = "<COLOR>";
    
    public static final String BOX_STYLE =
        "-fx-padding: 2;" +
        "-fx-border-style: solid inside;" +
        "-fx-border-width: 2;" +
        "-fx-border-insets: 5;" +
        "-fx-border-radius: 5;" +
        "-fx-border-color: " + BOX_STYLE_COLOR + ";";

    public ElementBox(ColumnBox parentBox, String header, String data, String color) {
        super(ColumnBox.VBOX_ELEMENT_SPACING);
        
        this.color = color;
        setStyle(StringUtils.replace(BOX_STYLE, BOX_STYLE_COLOR, color));
        setAlignment(Pos.CENTER);
        
        this.parentBox = parentBox;
        
        headerLabel = new Label(header);
        headerLabel.setFont(Font.font(TEXT_FONT_STYLE, FontWeight.EXTRA_BOLD, TEXT_FONT_SIZE));
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setUnderline(true);
        headerLabel.setTextFill(Color.web(color));
        getChildren().add(headerLabel);
        
        dataLabel = new Label(data);
        dataLabel.setFont(Font.font(TEXT_FONT_STYLE, FontWeight.BOLD, TEXT_FONT_SIZE));
        dataLabel.setAlignment(Pos.CENTER);
        dataLabel.setTextFill(Color.web(color));
        getChildren().add(dataLabel);
    }

    public ColumnBox getParentBox() {
        return parentBox;
    }
    
    public Label getHeaderLabel() {
        return headerLabel;
    }

    public Label getDataLabel() {
        return dataLabel;
    }
}
