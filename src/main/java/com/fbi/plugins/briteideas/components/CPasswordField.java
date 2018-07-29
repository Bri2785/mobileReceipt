// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.components;

import java.util.Arrays;
import javax.swing.JPasswordField;

public class CPasswordField extends JPasswordField
{
    private String displayName;
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }
    
    public String validateField() {
        final StringBuilder errorMessage = new StringBuilder();
        final char[] password = this.getPassword();
        if (password == null || password.length == 0) {
            errorMessage.append(this.getDisplayName()).append(" is empty.").append("\n");
        }
        else {
            Arrays.fill(password, '\0');
        }
        return errorMessage.toString();
    }
}
