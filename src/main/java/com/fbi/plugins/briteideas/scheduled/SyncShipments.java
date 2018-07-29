// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.scheduled;

import com.fbi.plugins.briteideas.exception.CommerceException;
import com.fbi.plugins.briteideas.exception.FishbowlException;
import com.fbi.plugins.briteideas.fbapi.Api;
import com.fbi.plugins.briteideas.fbapi.ApiCaller;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.plugins.briteideas.util.property.Property;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import org.slf4j.LoggerFactory;
import com.fbi.fbo.impl.dataexport.QueryRow;
import com.fbi.fbo.message.response.ResponseBase;
import com.fbi.fbo.message.request.RequestBase;
import com.fbi.fbo.impl.ApiCallType;
import com.fbi.sdk.constants.RecordTypeConst;
import com.fbi.importexport.impl.customfield.IECustomFieldData;
import com.fbi.fbo.message.request.ImportRequest;
import com.fbi.fbo.message.response.ImportResponse;
import com.fbi.fbo.impl.message.request.ImportRequestImpl;
import com.evnt.util.StringRowData;
import com.fbi.fbo.message.request.GetShipmentRequest;
import com.fbi.fbo.message.response.GetShipmentResponse;
import com.fbi.fbo.impl.message.request.GetShipmentRequestImpl;
import com.fbi.fbo.message.request.SaveShipmentRequest;
import com.evnt.util.FbiMessage;
import com.fbi.fbo.message.response.SaveShipmentResponse;
import com.fbi.fbo.impl.message.request.SaveShipmentRequestImpl;
import com.evnt.util.Money;
import com.fbi.commerce.shipping.type.ShipServiceEnum;
import com.fishbowl.hub.db.api.shipping.Carton;

import java.util.ArrayList;
import com.fishbowl.hub.db.api.shipping.Shipment;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import com.fbi.sdk.constants.ShipStatusConst;
//import com.fbi.commerce.shipping.connection.HubApi;
import com.fbi.commerce.shipping.util.CommerceUtil;
import com.evnt.util.Util;
import java.util.Date;
import java.util.HashMap;
import org.slf4j.Logger;
import com.fbi.plugins.ScheduledFishbowlPlugin;

public final class SyncShipments extends ScheduledFishbowlPlugin implements ApiCaller, PropertyGetter, Repository.RunSql
{
    private static final Logger LOGGER;
    private final Repository repository;
    private final StringBuilder errors;
    
    public SyncShipments() {
        this.repository = new Repository(this);
        this.errors = new StringBuilder();
        this.setModuleName("ShipExpress");
    }
    
    public void run(final HashMap hashMap) throws Exception {
        final Date startTime = new Date();
        SyncShipments.LOGGER.info("Begin ShipExpress Sync");
        if (Util.isEmpty(this.getProperty("shippoApiToken"))) {
            SyncShipments.LOGGER.info("Missing Shippo Api Key");
            throw new CommerceException("Missing Shippo Api Key - Enter it into the ShipExpress Settings tab and hit save.");
        }
        try {
            final String username = CommerceUtil.decrypt(Property.USERNAME.get(this));
            final String password = CommerceUtil.decrypt(Property.PASS.get(this));
            HubApi.login(username, password);
            final Long lastSyncDateMs = Property.LAST_SYNC_DATE.get(this);
            final Date lastSyncDate = (lastSyncDateMs != null) ? new Date(lastSyncDateMs) : null;
            final List<Shipment> openShipments = this.repository.getShipmentsToSync(lastSyncDate, Arrays.asList(ShipStatusConst.ENTERED.getId(), ShipStatusConst.PACKED.getId()));
            HubApi.syncOpenShipments(openShipments);
            openShipments.clear();
            final List<Shipment> shippedShipments = this.repository.getShipmentsToSync(lastSyncDate, Collections.singletonList(ShipStatusConst.SHIPPED.getId()));
            HubApi.syncShippedShipments(shippedShipments);
            shippedShipments.clear();
            this.updateFbShipments(HubApi.getShippedShipments());
            final List<String> shipNums = HubApi.getOpenShipmentIds();
            final List<String> voidedShipNums = this.repository.getVoidedShipNums(shipNums);
            shipNums.clear();
            HubApi.voidShipments(voidedShipNums);
            voidedShipNums.clear();
            this.savePluginProperties((Map)Collections.singletonMap(Property.LAST_SYNC_DATE.getKey(), String.valueOf(startTime.getTime())));
            if (this.errors.length() > 0) {
                throw new CommerceException(this.errors.toString());
            }
        }
        finally {
            HubApi.logout();
            SyncShipments.LOGGER.info("Commerce sync finished");
        }
    }
    
    private void updateFbShipments(final List<Shipment> shippedShipments) throws CommerceException {
        final List<Long> coIdList = new ArrayList<Long>();
        for (final Shipment shipment : shippedShipments) {
            try {
                final com.fbi.fbo.shipping.Shipment fbShipment = this.updateTracking(shipment);
                this.updateLabelId(shipment.getShippoRateId(), shipment.getShipNum());
                if (Property.AUTO_FULFILL_ORDERS.get(this)) {
                    this.updateFbOrderStatus(shipment.getShipNum(), fbShipment.getStatus(), fbShipment.getOrderNumber());
                }
            }
            catch (CommerceException e) {
                this.errors.append(e.getMessage()).append("\n");
            }
            coIdList.add(shipment.getCoId());
        }
        HubApi.updateShipExpressOrderPostStatus(coIdList);
    }
    
    private static String unableToUpdateShipment(final String shipNum) {
        return "Unable to update shipment '" + shipNum + "'.";
    }
    
    private com.fbi.fbo.shipping.Shipment updateTracking(final Shipment shipment) throws CommerceException {
        final com.fbi.fbo.shipping.Shipment fbShipment = this.getShipment(shipment.getShipNum());
        int index = 0;
        boolean isCarrierSet = false;
        for (final Carton carton : shipment.getCartons()) {
            if (!isCarrierSet) {
                fbShipment.setCarrier(carton.getCarrierName());
                final Integer carrierServiceId = this.repository.getCarrierServiceId(carton.getCarrierName(), ShipServiceEnum.getShippoFbCarrierServiceByCarrierAndService(carton.getCarrierName(), carton.getCarrierService()));
                if (carrierServiceId != null) {
                    fbShipment.setCarrierServiceId((int)carrierServiceId);
                }
                isCarrierSet = true;
            }
            if (index < fbShipment.getCartonCount()) {
                fbShipment.getCarton(index).setTrackingNum(carton.getTrackingNum());
                if (Property.SET_CARTON_COST.get(this)) {
                    fbShipment.getCarton(index).setFreightAmount(new Money(carton.getCartonCost()));
                }
            }
            ++index;
        }
        this.saveShipment(shipment.getShipNum(), fbShipment);
        return fbShipment;
    }
    
    private void saveShipment(final String shipNum, final com.fbi.fbo.shipping.Shipment fbShipment) throws CommerceException {
        final SaveShipmentRequest saveShipmentRequest = (SaveShipmentRequest)new SaveShipmentRequestImpl();
        saveShipmentRequest.setShipment(fbShipment);
        SaveShipmentResponse saveShipmentResponse;
        try {
            saveShipmentResponse = Api.SAVE_SHIPMENT.call(this, saveShipmentRequest);
        }
        catch (FishbowlException e) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
        if (saveShipmentResponse.getStatusCode() != FbiMessage.SUCCESS.getId()) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
    }
    
    private com.fbi.fbo.shipping.Shipment getShipment(final String shipNum) throws CommerceException {
        final GetShipmentRequest getShipmentRequest = (GetShipmentRequest)new GetShipmentRequestImpl();
        getShipmentRequest.setShipmentNum(shipNum);
        GetShipmentResponse getShipmentResponse;
        try {
            getShipmentResponse = Api.GET_SHIPMENT.call(this, getShipmentRequest);
        }
        catch (Exception e) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
        if (getShipmentResponse.getStatusCode() != FbiMessage.SUCCESS.getId()) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
        return getShipmentResponse.getShipping();
    }
    
    private void updateFbOrderStatus(final String shipNum, final int shipStatusId, final String soNum) throws CommerceException {
        if (ShipStatusConst.ENTERED.getId() == shipStatusId) {
            this.packOrder(soNum, shipNum);
        }
        if (ShipStatusConst.SHIPPED.getId() != shipStatusId) {
            this.shipOrder(shipNum);
        }
    }
    
    private void packOrder(final String soNum, final String shipNum) throws CommerceException {
        final StringRowData header = new StringRowData(new String[] { "SONum" });
        final StringRowData row = new StringRowData(new String[0]);
        row.setColumnNames(header);
        row.set("SONum", soNum);
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(header.toString());
        importRows.add(row.toString());
        final ImportRequest request = (ImportRequest)new ImportRequestImpl();
        request.setImportType("ImportPackingData");
        request.setRows((ArrayList)importRows);
        ImportResponse response;
        try {
            response = Api.IMPORT.call(this, request);
        }
        catch (FishbowlException e) {
            throw new CommerceException("Unable to pack shipment '" + shipNum + "'.");
        }
        if (response.getStatusCode() != FbiMessage.SUCCESS.getId()) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
    }
    
    private void shipOrder(final String shipNum) throws CommerceException {
        final StringRowData header = new StringRowData(new String[] { "ShipNum" });
        final StringRowData row = new StringRowData(new String[0]);
        row.setColumnNames(header);
        row.set("ShipNum", shipNum);
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(header.toString());
        importRows.add(row.toString());
        final ImportRequest request = (ImportRequest)new ImportRequestImpl();
        request.setImportType("ImportShippingData");
        request.setRows((ArrayList)importRows);
        ImportResponse response;
        try {
            response = Api.IMPORT.call(this, request);
        }
        catch (FishbowlException e) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
        if (response.getStatusCode() != FbiMessage.SUCCESS.getId()) {
            throw new CommerceException(unableToUpdateShipment(shipNum));
        }
    }
    
    private void updateLabelId(final String rateId, final String shipNum) {
        final StringRowData header = new StringRowData(IECustomFieldData.defaultHeader);
        final StringRowData row = new StringRowData(new String[0]);
        row.setColumnNames(header);
        row.set("FieldName", "Label ID");
        row.set("ModuleName", RecordTypeConst.SHIP.getTableName());
        row.set("RecordData", shipNum);
        row.set("Data", rateId);
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(header.toString());
        importRows.add(row.toString());
        final ImportRequest cfImport = (ImportRequest)new ImportRequestImpl();
        cfImport.setImportType("ImportCustomFieldData");
        cfImport.setRows((ArrayList)importRows);
        try {
            Api.IMPORT.call(this, cfImport);
        }
        catch (FishbowlException ex) {}
    }
    
    public ResponseBase call(final ApiCallType requestType, final RequestBase requestBase) throws FishbowlException {
        try {
            return this.runApiRequest(requestType, requestBase);
        }
        catch (Exception e) {
            throw new FishbowlException(e);
        }
    }
    
    public String getProperty(final String key) {
        return this.repository.getProperty(key);
    }
    
    public List<QueryRow> executeSql(final String query) {
        return (List<QueryRow>)this.runQuery(query);
    }
    
    static {
        LOGGER = LoggerFactory.getLogger((Class)ScheduledFishbowlPlugin.class);
    }
}
