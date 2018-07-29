// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.components;

import com.fbi.gui.combobox.FBComboBox;
import com.fbi.gui.rowdata.RowData;

public class CComboBox<T extends RowData> extends FBComboBox<T>
{
    private String displayName;
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }
    
    public String validateField() {
        if (this.isEnabled() && this.getSelectedID() <= 0) {
            return this.getDisplayName() + " is not set.\n";
        }
        return "";
    }
}
