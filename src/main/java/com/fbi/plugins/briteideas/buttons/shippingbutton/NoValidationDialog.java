// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.shippingbutton;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridBagLayout;
import javax.swing.ImageIcon;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import com.fbi.plugins.briteideas.util.property.Property;
import java.util.HashMap;
import com.fbi.gui.util.UtilGui;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.fbi.commerce.shipping.util.ShippoInfo;

import javax.swing.JDialog;

public class NoValidationDialog extends JDialog
{
    private ShippingButton shippingButton;
    private ShippoInfo shippoInfo;
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel lblImage;
    private JTextArea txtMessage;
    private JPanel buttonBar;
    private JCheckBox chkDontShowAgain;
    private JButton okButton;
    
    public NoValidationDialog(final ShippingButton shippingButton, final ShippoInfo shippoInfo) {
        super(UtilGui.getParentFrame());
        this.shippingButton = shippingButton;
        this.shippoInfo = shippoInfo;
        this.initComponents();
        this.setVisible(true);
    }
    
    private void okButtonActionPerformed() {
        final Map<String, String> data = new HashMap<String, String>();
        data.put(Property.SHOW_NO_VALIDATE_WARNING.getKey(), Boolean.toString(!this.chkDontShowAgain.isSelected()));
        this.shippingButton.savePluginProperties((Map)data);
        this.shippoInfo.setShowNoValidateWarning(!this.chkDontShowAgain.isSelected());
        this.setVisible(false);
        this.dispose();
    }
    
    private void initComponents() {
        this.dialogPane = new JPanel();
        this.contentPanel = new JPanel();
        this.lblImage = new JLabel();
        this.txtMessage = new JTextArea();
        this.buttonBar = new JPanel();
        this.chkDontShowAgain = new JCheckBox();
        this.okButton = new JButton();
        this.setTitle("Warning");
        this.setMinimumSize(new Dimension(430, 180));
        this.setModal(true);
        this.setName("this");
        final Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        this.dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        this.dialogPane.setName("dialogPane");
        this.dialogPane.setLayout(new BorderLayout());
        this.contentPanel.setName("contentPanel");
        this.contentPanel.setLayout(new BorderLayout());
        this.lblImage.setIcon(new ImageIcon(this.getClass().getResource("/images/warning.png")));
        this.lblImage.setPreferredSize(new Dimension(70, 48));
        this.lblImage.setHorizontalAlignment(0);
        this.lblImage.setMaximumSize(new Dimension(70, 48));
        this.lblImage.setMinimumSize(new Dimension(70, 48));
        this.lblImage.setName("lblImage");
        this.contentPanel.add(this.lblImage, "West");
        this.txtMessage.setLineWrap(true);
        this.txtMessage.setWrapStyleWord(true);
        this.txtMessage.setText("ShipExpress cannot validate international addresses. If your address is correct you may click OK below to purchase your label.");
        this.txtMessage.setEditable(false);
        this.txtMessage.setOpaque(false);
        this.txtMessage.setName("txtMessage");
        this.contentPanel.add(this.txtMessage, "Center");
        this.dialogPane.add(this.contentPanel, "Center");
        this.buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        this.buttonBar.setName("buttonBar");
        this.buttonBar.setLayout(new GridBagLayout());
        ((GridBagLayout)this.buttonBar.getLayout()).columnWidths = new int[] { 0, 80 };
        ((GridBagLayout)this.buttonBar.getLayout()).columnWeights = new double[] { 1.0, 0.0 };
        this.chkDontShowAgain.setText("Do not show again");
        this.chkDontShowAgain.setName("chkDontShowAgain");
        this.buttonBar.add(this.chkDontShowAgain, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.okButton.setText("OK");
        this.okButton.setName("okButton");
        this.okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                NoValidationDialog.this.okButtonActionPerformed();
            }
        });
        this.buttonBar.add(this.okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.dialogPane.add(this.buttonBar, "South");
        contentPane.add(this.dialogPane, "Center");
        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }
}
