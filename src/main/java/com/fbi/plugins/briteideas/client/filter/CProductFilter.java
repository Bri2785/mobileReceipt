// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.client.filter;

import com.fbi.gui.rowdata.RowData;
import com.fbi.sdk.constants.PartTypeConst;
import com.evnt.client.modules.product.data.ProductSearchRowData;
import com.evnt.client.modules.product.filters.FBFilterProduct;

public class CProductFilter extends FBFilterProduct
{
    public CProductFilter(final boolean includeInActive) {
        super(includeInActive);
    }
    
    protected boolean doesRowMatch(final ProductSearchRowData productSearchRowData, final String filter) {
        return super.doesRowMatch(productSearchRowData, filter) && productSearchRowData.getProduct().getProductType() == PartTypeConst.SHIPPING;
    }
    
    public boolean allowNew() {
        return false;
    }
}
