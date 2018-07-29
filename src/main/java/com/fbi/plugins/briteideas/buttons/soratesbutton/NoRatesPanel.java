// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.soratesbutton;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NoRatesPanel extends JPanel
{
    private JLabel label1;
    
    public NoRatesPanel() {
        this.initComponents();
    }
    
    private void initComponents() {
        this.label1 = new JLabel();
        this.setName("this");
        this.setLayout(new GridBagLayout());
        ((GridBagLayout)this.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout)this.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.getLayout()).rowWeights = new double[] { 0.0, 1.0E-4 };
        this.label1.setText("Click the \"Get Rates\" buttons to get rates");
        this.label1.setFont(new Font("Segoe UI", 0, 16));
        this.label1.setName("label1");
        this.add(this.label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 3, new Insets(20, 0, 0, 0), 0, 0));
    }
}
