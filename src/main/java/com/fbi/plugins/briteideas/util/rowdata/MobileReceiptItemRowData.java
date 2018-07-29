package com.fbi.plugins.briteideas.util.rowdata;

import com.fbi.gui.rowdata.RowData;
import com.fbi.gui.table.FBTableColumn;
import com.fbi.gui.table.FBTableColumnEditable;
import com.fbi.gui.table.FBTableColumnSettings;
import com.fbi.plugins.briteideas.data.MobileReceiptItem;

import java.util.Date;

public class MobileReceiptItemRowData implements RowData {

    private static int colCount;
    //public static FBTableColumn colMobileReceiptId;
    public static FBTableColumn colUpc;
    public static FBTableColumn colLastScan;
    public static FBTableColumn colQtyScanned;
    public static FBTableColumn colProductNum;
    public static FBTableColumn colUomMultiply;
    public static FBTableColumn colTotalReceived;
    //public static FBTableColumn colStatus;

    private MobileReceiptItem mobileReceiptItem; //our data object

    protected static void init(){
        MobileReceiptItemRowData.colCount = 0;
        //MobileReceiptItemRowData.colMobileReceiptId = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Receipt ID", int.class, 60, "", FBTableColumnEditable.NOT_EDITABLE, true, true);
        MobileReceiptItemRowData.colUpc = new FBTableColumn(MobileReceiptItemRowData.colCount++, "UPC", String.class, 125, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptItemRowData.colLastScan = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Last Scan", Date.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptItemRowData.colProductNum = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Product Num", String.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptItemRowData.colQtyScanned = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Qty Scanned", double.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptItemRowData.colUomMultiply = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Case Qty", double.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptItemRowData.colTotalReceived = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Total", double.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);

        //MobileReceiptItemRowData.colStatus = new FBTableColumn(MobileReceiptItemRowData.colCount++, "Time Uploaded", Date.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, true, false);


    }

    public MobileReceiptItemRowData(final MobileReceiptItem mobileReceiptItem){
        this.mobileReceiptItem = mobileReceiptItem;
    }

    public MobileReceiptItem getMobileReceiptItem(){
        return this.mobileReceiptItem;
    }

    @Override
    public int getID() {
        return this.mobileReceiptItem.getId();
    }

    @Override
    public String getValue() {
        return this.mobileReceiptItem.getUpc() + " - " + this.mobileReceiptItem.getQtyScanned();
    }

    @Override
    public Object[] getRow() {
        final Object[] values = new Object[MobileReceiptItemRowData.colCount];
        //values[MobileReceiptItemRowData.colMobileReceiptId.getColIndex()] = this.mobileReceiptItem.getMrid();
        values[MobileReceiptItemRowData.colUpc.getColIndex()] = this.mobileReceiptItem.getUpc();
        values[MobileReceiptItemRowData.colLastScan.getColIndex()] = this.mobileReceiptItem.getLastScan();
        values[MobileReceiptItemRowData.colProductNum.getColIndex()] = this.mobileReceiptItem.getProductNum();
        values[MobileReceiptItemRowData.colQtyScanned.getColIndex()] = this.mobileReceiptItem.getQtyScanned();
        values[MobileReceiptItemRowData.colUomMultiply.getColIndex()] = this.mobileReceiptItem.getUomMultiply();
        values[MobileReceiptItemRowData.colTotalReceived.getColIndex()] = this.mobileReceiptItem.getTotalReceived();
        //values[MobileReceiptItemRowData..getColIndex()] = this.mobileReceiptItem.getTimeUploaded(); //TODO:status later
        return values;
    }

    @Override
    public void setValueAt(int i, Object o) {
        //dont need since read only
    }

    @Override
    public boolean isCellEditable(int i) {
        return false;
    }


    public static FBTableColumnSettings getSettings(){
        final FBTableColumnSettings settings = new FBTableColumnSettings(true, true);
        //settings.addColumn(MobileReceiptItemRowData.colMobileReceiptId);
        settings.addColumn(MobileReceiptItemRowData.colUpc);
        settings.addColumn(MobileReceiptItemRowData.colLastScan);
        settings.addColumn(MobileReceiptItemRowData.colProductNum);
        settings.addColumn(MobileReceiptItemRowData.colQtyScanned);
        settings.addColumn(MobileReceiptItemRowData.colUomMultiply);
        settings.addColumn(MobileReceiptItemRowData.colTotalReceived);
        //settings.addColumn(MobileReceiptItemRowData.colTimeUploaded); //TODO: status later
        return settings;

    }

    static{

        init();
    }
}
