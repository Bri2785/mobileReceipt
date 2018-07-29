// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.client.filter;

import com.fbi.gui.table.FBTableColumnSettings;
import com.fbi.fbdata.inventory.ProductSearchImpl;
import com.evnt.client.modules.product.data.ProductSearchRowData;

public class CProductSearchRowData extends ProductSearchRowData
{
    public CProductSearchRowData(final ProductSearchImpl product) {
        super(product);
    }
    
    public static FBTableColumnSettings getSettings() {
        final FBTableColumnSettings settings = new FBTableColumnSettings(true, true);
        settings.addColumn(CProductSearchRowData.colProductNum);
        settings.addColumn(CProductSearchRowData.colProductDesc);
        return settings;
    }
}
