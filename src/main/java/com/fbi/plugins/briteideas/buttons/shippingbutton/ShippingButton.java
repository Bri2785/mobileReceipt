// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.shippingbutton;

import com.fbi.fbo.message.request.ImportRequest;
import com.fbi.fbo.impl.message.request.ImportRequestImpl;
import java.util.ArrayList;
import com.evnt.util.StringRowData;
import com.fbi.importexport.impl.customfield.IECustomFieldData;
import com.fbi.fbo.message.request.SaveShipmentRequest;
import com.fbi.fbo.impl.message.request.SaveShipmentRequestImpl;
import java.util.Date;

import com.fbi.plugins.briteideas.util.property.Property;
import com.shippo.model.Transaction;
import com.evnt.util.Money;
import com.evnt.util.FbiMessage;
import com.fbi.sdk.constants.CustomFieldTypeConst;
import com.fbi.sdk.constants.RecordTypeConst;
import java.math.BigDecimal;
import java.util.List;

import com.evnt.util.Quantity;
import com.fbi.commerce.shipping.type.ShipServiceEnum;
import com.fbi.fbo.shipping.Carton;
import com.fbi.commerce.shipping.util.ShippoInfo;
import com.fbi.fbo.message.response.ResponseBase;
import com.fbi.fbo.message.request.RequestBase;
import com.fbi.fbo.impl.ApiCallType;
import com.fbi.fbo.message.request.GetShipmentRequest;
import com.fbi.fbo.impl.dataexport.QueryRow;
import com.fbi.plugins.briteideas.exception.FishbowlException;
import com.fbi.plugins.briteideas.fbapi.Api;
import com.fbi.fbo.message.response.GetShipmentResponse;
import com.fbi.fbo.impl.message.request.GetShipmentRequestImpl;
import com.evnt.util.Util;
import com.fbi.gui.util.UtilGui;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.fbi.fbo.shipping.Shipment;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import com.fbi.plugins.briteideas.fbapi.ApiCaller;
import com.fbi.plugins.FishbowlPluginButton;
import com.shippo.model.TransactionCollection;

public final class ShippingButton extends FishbowlPluginButton implements ApiCaller, PropertyGetter, Repository.RunSql
{
    private Repository repository;
    private int shipId;
    private String cartonName;
    private Shipment shipment;
    
    public ShippingButton() {
        this.repository = new Repository(this);
        this.shipId = -1;
        this.cartonName = "Carton";
        this.setModuleName("Shipping");
        this.setPluginName("ShipExtend");
        this.setIcon((Icon)new ImageIcon(this.getClass().getResource("/images/create_label.png")));
        this.setText("Create Label");
        this.addActionListener(event -> this.actionPerformed());
    }
    
    private void actionPerformed() {
        if (!this.hasAccess("Ship Button")) {
            UtilGui.showMessageDialog("You do not have rights to access this feature.", "Access Denied", 1);
            return;
        }
        if (Util.isEmpty(this.getProperty("shippoApiToken"))) {
            UtilGui.showMessageDialog("In the ShipExpress Plugin, enter your Shippo API Key in Account Setup section of the Shipping Settings.", "Missing Shippo Api Key", 1);
            return;
        }
        this.shipId = this.getObjectId();
        if (this.shipId <= 0) {
            UtilGui.showMessageDialog("You must select a shipment before using this feature.", "Load Error", 1);
            return;
        }
        final QueryRow customFieldInfo = this.repository.getCustomFieldInfo(this.shipId);
        if (customFieldInfo != null && !Util.isEmpty(customFieldInfo.getString("info"))) {
            final int keepGoing = UtilGui.showConfirmDialog("A label has already been created for this shipment.  Would you like to create another label?", "Label Already Created", 0);
            if (keepGoing != 0) {
                return;
            }
        }
        final QueryRow row = this.repository.getCartonName();
        if (row != null) {
            this.cartonName = row.getString("name");
        }
        final GetShipmentRequest getShipmentRequest = (GetShipmentRequest)new GetShipmentRequestImpl();
        getShipmentRequest.setShipmentID(this.shipId);
        try {
            this.shipment = Api.GET_SHIPMENT.call(this, getShipmentRequest).getShipping();
        }
        catch (FishbowlException e) {
            UtilGui.showMessageDialog(e.getMessage(), "Fetch Shipment Error", 0);
        }
        if (this.shipment == null) {
            UtilGui.showMessageDialog("Could not retrieve information for the shipment.", "Fetch Shipment Error", 0);
            return;
        }
        if (!this.validateShipment(this.shipment)) {
            return;
        }
        final QueryRow shipFromAddress = this.repository.getShipFromAddress(this.shipment.getLocGroupName());
        if (shipFromAddress == null) {
            UtilGui.showMessageDialog("Please create a Main Office or Ship To address for the " + this.shipment.getLocGroupName() + " group in the company addresses tab.", "Missing Location Group Address", 1);
            return;
        }
        final String mainPhoneNumber = this.repository.getPhoneFromMainOffice();
        if (mainPhoneNumber == null || mainPhoneNumber.isEmpty()) {
            UtilGui.showMessageDialog("Your default main office address does not have a Main phone number setup.  Please setup a Main phone number in the company module under the address tab.", "Missing Main Phone Number", 1);
            return;
        }
        new PurchaseLabelDialog(UtilGui.getParentFrame(), this.shipment, shipFromAddress, this.getShippoInfo(), this.getMainObject(), this.cartonName, this.repository, mainPhoneNumber, this);
    }
    
    public ResponseBase call(final ApiCallType requestType, final RequestBase requestBase) throws FishbowlException {
        try {
            return this.runApiRequest(requestType, requestBase);
        }
        catch (Exception e) {
            throw new FishbowlException(e);
        }
    }
    
    private ShippingButton getMainObject() {
        return this;
    }
    
    private ShippoInfo getShippoInfo() {
        return new ShippoInfo(Property.SHIPPO_API_TOKEN.get(this), Property.LABEL_TEXT.get(this), Property.LABEL_FORMAT.get(this), Property.SHOW_NO_VALIDATE_WARNING.get(this));
    }
    
    private boolean validateShipment(final Shipment shipment) {
        final StringBuilder errors = new StringBuilder();
        if (Util.isEmpty(shipment.getCarrier())) {
            errors.append("Please select a carrier for this order.\n");
        }
        if (shipment.getCarrierServiceId() <= 0) {
            errors.append("Please select a carrier service for this order.\n");
        }
        if ("USPS".equalsIgnoreCase(shipment.getCarrier()) && shipment.getCartonCount() > 1) {
            errors.append("USPS doesn't return rates for shipments with more than one ").append(this.cartonName).append(".\n");
        }
        if (shipment.getCartonCount() > 10) {
            errors.append("A max of 10 cartons per order can be used with Fishbowl ShipExpress.\n");
        }
        boolean zeroWeightError = false;
        boolean tooHeavyError = false;
        for (final Carton carton : shipment.getCartons()) {
            if (tooHeavyError && zeroWeightError) {
                break;
            }
            if (carton.getFreightWeight().isZero() && !zeroWeightError) {
                zeroWeightError = true;
                errors.append("All ").append(this.cartonName).append("(s)  must have a weight greater than zero.\n");
            }
            else {
                if (tooHeavyError || !ShipServiceEnum.USPS_FIRST.getFbCarrier().equalsIgnoreCase(shipment.getCarrier()) || !ShipServiceEnum.USPS_FIRST.getFbService().equalsIgnoreCase(this.repository.getCarrierServiceById(shipment.getCarrierServiceId())) || !carton.getFreightWeight().greaterThanOrEqualTo(Quantity.quantityOne)) {
                    continue;
                }
                tooHeavyError = true;
                errors.append("USPS First Class Mail requires the total shipment weight to be less than 1 lb.\n");
            }
        }
        if (!Util.isEmpty(errors)) {
            UtilGui.showMessageDialog(errors.toString(), "Shipment Validation Error", 1);
            return false;
        }
        return true;
    }
    
    public String getProperty(final String key) {
        return this.repository.getProperty(key);
    }
    
    public List<QueryRow> executeSql(final String query) {
        return (List<QueryRow>)this.runQuery(query);
    }
    
    void saveShippingInfo(final TransactionCollection transactionCollection, final String rateId, final BigDecimal rateAmount) {
        final QueryRow customField = this.repository.getCustomField("Label ID", RecordTypeConst.SHIP.getId(), CustomFieldTypeConst.CFT_LONG_TEXT.getId());
        if (customField == null) {
            UtilGui.showMessageDialog("Please create a custom field for shipping called 'Label ID'", "Unable to Save Label Id", 1);
            return;
        }
        if (!this.updateLabelId(rateId)) {
            UtilGui.showMessageDialog("Could not update the ShipExpress custom field.", "Save Error", 1);
            return;
        }
        if (!this.updateTracking(transactionCollection, rateAmount)) {
            UtilGui.showMessageDialog("Could not update the tracking on the shipment.", "Update Tracking Error", 1);
            return;
        }
        this.reloadObject();
    }
    
    private boolean updateTracking(final TransactionCollection transactionCollection, final BigDecimal rateAmount) {
        final GetShipmentRequest getShipmentRequest = (GetShipmentRequest)new GetShipmentRequestImpl();
        getShipmentRequest.setShipmentID(this.shipId);
        Shipment ship;
        try {
            final GetShipmentResponse response = Api.GET_SHIPMENT.call(this, getShipmentRequest);
            if (response.getStatusCode() != FbiMessage.SUCCESS.getId()) {
                return false;
            }
            ship = response.getShipping();
        }
        catch (FishbowlException e) {
            return false;
        }
        int index = ship.getCartonCount() - 1;
        Double freightAmount = 0.0;
        if (rateAmount != null) {
            freightAmount = rateAmount.doubleValue();
        }
        if (Property.SET_CARTON_COST.get(this)) {
            ship.getCartons().get(0).setFreightAmount(new Money(freightAmount));
        }
        for (final Transaction transaction : transactionCollection.getData()) {
            ship.getCartons().get(index--).setTrackingNum(transaction.getTrackingNumber().toString());
        }
        ship.setDateLastModified((Date)null);
        final SaveShipmentRequest saveShipmentRequest = (SaveShipmentRequest)new SaveShipmentRequestImpl();
        saveShipmentRequest.setShipment(ship);
        try {
            return Api.SAVE_SHIPMENT.call(this, saveShipmentRequest).getStatusCode() == FbiMessage.SUCCESS.getId();
        }
        catch (FishbowlException e2) {
            return false;
        }
    }
    
    private boolean updateLabelId(final String rateId) {
        final StringRowData header = new StringRowData(IECustomFieldData.defaultHeader);
        final StringRowData row = new StringRowData(new String[0]);
        row.setColumnNames(header);
        row.set("FieldName", "Label ID");
        row.set("ModuleName", RecordTypeConst.SHIP.getTableName());
        row.set("RecordData", String.valueOf(this.shipment.getShipNumber()));
        row.set("Data", rateId);
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(header.toString());
        importRows.add(row.toString());
        final ImportRequest cfImport = (ImportRequest)new ImportRequestImpl();
        cfImport.setImportType("ImportCustomFieldData");
        cfImport.setRows((ArrayList)importRows);
        try {
            return Api.IMPORT.call(this, cfImport).getStatusCode() == FbiMessage.SUCCESS.getId();
        }
        catch (FishbowlException e) {
            return false;
        }
    }
}
