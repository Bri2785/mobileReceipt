package com.fbi.plugins.briteideas.util.rowdata;

import com.fbi.gui.rowdata.RowData;

import com.fbi.gui.table.FBTableColumn;
import com.fbi.gui.table.FBTableColumnEditable;
import com.fbi.gui.table.FBTableColumnSettings;
import com.fbi.plugins.briteideas.data.MobileReceipt;

import java.util.Date;

public class MobileReceiptSearchRowData implements RowData {

    private static int colCount;
    public static FBTableColumn colMobileReceiptId;
    public static FBTableColumn colDescription;
    public static FBTableColumn colTimeFinished;
    public static FBTableColumn colTimeStarted;
    public static FBTableColumn colTimeUploaded;

    private MobileReceipt mobileReceipt; //our data object

    protected static void init(){
        MobileReceiptSearchRowData.colCount = 0;
        MobileReceiptSearchRowData.colMobileReceiptId = new FBTableColumn(MobileReceiptSearchRowData.colCount++, "Mobile ID", int.class, 60, "", FBTableColumnEditable.NOT_EDITABLE, false, true);
        MobileReceiptSearchRowData.colDescription = new FBTableColumn(MobileReceiptSearchRowData.colCount++, "Description", String.class, 150, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptSearchRowData.colTimeFinished = new FBTableColumn(MobileReceiptSearchRowData.colCount++, "Time Finished", Date.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, false, false);
        MobileReceiptSearchRowData.colTimeStarted = new FBTableColumn(MobileReceiptSearchRowData.colCount++, "Time Started", Date.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, true, false);
        MobileReceiptSearchRowData.colTimeUploaded = new FBTableColumn(MobileReceiptSearchRowData.colCount++, "Time Uploaded", Date.class, 100, "", FBTableColumnEditable.NOT_EDITABLE, true, false);


    }

    public MobileReceiptSearchRowData(final MobileReceipt mobileReceipt){
        this.mobileReceipt = mobileReceipt;
    }

    public MobileReceipt getMobileReceipt(){
        return this.mobileReceipt;
    }

    @Override
    public int getID() {
        return this.mobileReceipt.getMr_id();
    }

    @Override
    public String getValue() {
        return this.mobileReceipt.getId() + " - " + this.mobileReceipt.getMr_id() + " - " + this.mobileReceipt.getDescription();
    }

    @Override
    public Object[] getRow() {
        final Object[] values = new Object[MobileReceiptSearchRowData.colCount];
        values[MobileReceiptSearchRowData.colMobileReceiptId.getColIndex()] = this.mobileReceipt.getMr_id();
        values[MobileReceiptSearchRowData.colDescription.getColIndex()] = this.mobileReceipt.getDescription();
        values[MobileReceiptSearchRowData.colTimeFinished.getColIndex()] = this.mobileReceipt.getTimeFinished();
        values[MobileReceiptSearchRowData.colTimeStarted.getColIndex()] = this.mobileReceipt.getTimeStarted();
        values[MobileReceiptSearchRowData.colTimeUploaded.getColIndex()] = this.mobileReceipt.getTimeUploaded();
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
        settings.addColumn(MobileReceiptSearchRowData.colMobileReceiptId);
        settings.addColumn(MobileReceiptSearchRowData.colDescription);
        settings.addColumn(MobileReceiptSearchRowData.colTimeFinished);
        settings.addColumn(MobileReceiptSearchRowData.colTimeStarted);
        settings.addColumn(MobileReceiptSearchRowData.colTimeUploaded);
        return settings;

    }

    static{

        init();
    }
}
