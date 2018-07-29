package com.fbi.plugins.briteideas.data;

import com.fbi.fbdata.FBData;

import java.util.Date;

public class MobileReceiptItem implements FBData{

    //private int id;
    private int mrid;
    private String upc;
    private Date lastScan;
    //private int status;
    private double qtyScanned;
    private int productId;
    private String productNum;
    private int partId;
    private double uomMultiply;
    private double totalReceived;

    public MobileReceiptItem( int mrid, String upc, Date lastScan, double qtyScanned,
                             int productId, String productNum, int partId, double uomMultiply, double totalReceived) {
        //this.id = id;
        this.mrid = mrid;
        this.upc = upc;
        this.lastScan = lastScan;
        //this.status = status;
        this.qtyScanned = qtyScanned;
        this.productId = productId;
        this.productNum = productNum;
        this.partId = partId;
        this.uomMultiply = uomMultiply;
        this.totalReceived = totalReceived;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductNum() {
        return productNum;
    }

    public void setProductNum(String productNum) {
        this.productNum = productNum;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public double getUomMultiply() {
        return uomMultiply;
    }

    public void setUomMultiply(double uomMultiply) {
        this.uomMultiply = uomMultiply;
    }

    public double getTotalReceived() {
        return totalReceived;
    }

    public void setTotalReceived(double totalReceived) {
        this.totalReceived = totalReceived;
    }

    public int getMrid() {
        return mrid;
    }

    public void setMrid(int mrid) {
        this.mrid = mrid;
    }

    public String getUpc() {
        return upc;
    }

    public void setUpc(String upc) {
        this.upc = upc;
    }

    public Date getLastScan() {
        return lastScan;
    }

    public void setLastScan(Date lastScan) {
        this.lastScan = lastScan;
    }

//    public int getStatus() {
//        return status;
//    }
//
//    public void setStatus(int status) {
//        this.status = status;
//    }

    public double getQtyScanned() {
        return qtyScanned;
    }

    public void setQtyScanned(double qtyScanned) {
        this.qtyScanned = qtyScanned;
    }

    @Override
    public int getId() {
        return mrid;
    }

    @Override
    public void setId(int i) {
        this.mrid = i;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
