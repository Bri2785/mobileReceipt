// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.components;

import com.fbi.commerce.shipping.util.CommerceUtil;
import com.evnt.util.Util;
import javax.swing.JTextField;

public class CTextField extends JTextField
{
    private String displayName;
    private boolean emailField;
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }
    
    public boolean getEmailField() {
        return this.emailField;
    }
    
    public void setEmailField(final boolean emailField) {
        this.emailField = emailField;
    }
    
    public String validateField() {
        final StringBuilder errorMessage = new StringBuilder();
        if (Util.isEmpty(this.getText())) {
            errorMessage.append(this.getDisplayName()).append(" is empty.").append("\n");
        }
        else if (this.getEmailField() && !CommerceUtil.isValidEmail(this.getText())) {
            errorMessage.append(this.getDisplayName()).append(" is not a valid email address.").append("\n");
        }
        return errorMessage.toString();
    }
    
    @Override
    public String getText() {
        super.setText(super.getText().trim());
        return super.getText();
    }
}
