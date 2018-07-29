// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.soratesbutton;

import com.BI.Models.*;
import com.fbi.commerce.shipping.type.CountryEnum;
import com.fbi.commerce.shipping.type.ShipServiceEnum;
import com.evnt.util.StringRowData;
import com.fbi.commerce.shipping.util.fedex.ValidateAddress;
import com.fbi.commerce.shipping.util.shippo.ShippoUtil;
import com.fbi.fbdata.accounts.Country;
import com.fbi.fbdata.accounts.State;
import com.fbi.fbo.message.request.ImportRequest;
import com.evnt.util.FbiMessage;
import com.fbi.fbo.impl.message.request.ImportRequestImpl;
import com.fbi.fbo.message.request.salesorder.AddSOItemRequest;
import com.fbi.fbo.orders.SalesOrderItem;
import com.fbi.plugins.briteideas.fbapi.Api;
import com.fbi.fbo.impl.message.request.salesorder.AddSOItemRequestImpl;
import com.evnt.util.Money;
import com.fbi.sdk.constants.SOItemStatusConst;
import com.fbi.sdk.constants.SOItemTypeConst;
import com.evnt.util.Quantity;
import com.fbi.fbo.impl.orders.SalesOrderItemImpl;
import com.fbi.commerce.shipping.util.Compatibility;
import com.fbi.commerce.shipping.util.rowdata.CarrierRate;
import com.fbi.plugins.briteideas.exception.FishbowlException;
import com.fbi.fbo.message.response.ResponseBase;
import com.fbi.fbo.message.request.RequestBase;
import com.fbi.fbo.impl.ApiCallType;
import com.fbi.plugins.briteideas.util.property.Property;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.fbi.sdk.constants.UOMConst;
import com.evnt.eve.modules.AccountingModule;
import com.evnt.util.Util;
import com.fbi.gui.util.UtilGui;

import javax.swing.ImageIcon;
import com.fbi.commerce.shipping.util.ShippingPluginProperty;
import com.fbi.fbo.impl.dataexport.QueryRow;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import com.fbi.plugins.briteideas.fbapi.ApiCaller;
import com.fbi.plugins.FishbowlPluginButton;
import com.shippo.exception.APIConnectionException;
import com.shippo.exception.APIException;
import com.shippo.exception.AuthenticationException;
import com.shippo.exception.InvalidRequestException;
import com.shippo.model.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShipRatesButton extends FishbowlPluginButton implements ApiCaller, PropertyGetter, Repository.RunSql
{
    private static final Logger LOGGER;
    private Repository repository;
    private int soId;
    private ShipRatesDialog shipRatesDialog;
    private QueryRow orderInfo;
    private String shippoFromId;
    private ArrayList<String> shipFromError;
    private String shippoToId;
    private ArrayList <String> shipToError;

    private com.fbi.fbdata.accounts.Address shipToAddress;
    private com.fbi.fbdata.accounts.Address shipFromAddress;

    private boolean isValidToAddress;
    private boolean isValidFromAddress;

    private QueryRow shipFromAddressQueryRow;


    private List<QueryRow> orderItemInfo;
    private QueryRow orderSize;

    private ShippingPluginProperty shippingPluginProperty;

    public ShipRatesButton() {
        this.repository = new Repository(this);
        this.setModuleName("Sales Order");
        this.setPluginName("ShipExtend");
        this.setIcon(new ImageIcon(this.getClass().getResource("/images/ship_quote.png")));
        this.setText("Ship Quote");

        //this.shipToError = new ArrayList<String>();
        this.shipFromError = new ArrayList<String>();

        this.addActionListener((event) -> {
            this.getRatesActionPerformed();
        });
    }


    private void getRatesActionPerformed() {
        try {
            if (!this.hasAccess("Rates Button")) {

                UtilGui.showMessageDialog("You do not have rights to access this feature.", "Access Denied", 1);
            } else {

                this.shippingPluginProperty = this.getShippingProperties();

                if (Util.isEmpty(this.shippingPluginProperty.getShippoApiToken())) {

                    UtilGui.showMessageDialog("In the ShipExtend Plugin, enter your Shippo API Key in Account Setup section of the Shipping Settings.", "Missing Shippo Api Key", 1);
                } else if (!this.shippingPluginProperty.isUseFedEx() && !this.shippingPluginProperty.isUseUps() && !this.shippingPluginProperty.isUseUsps()) {

                    UtilGui.showMessageDialog("There are no carriers set to display rates in the ShipExtend plugin settings.", "No Carriers Set", 1);
                } else {

                    this.soId = this.getObjectId();
                    if (this.soId < 0) {
                        UtilGui.showMessageDialog("Please select an order before getting rates.", "Unable to get rates", 1);
                    } else {
                        UOMConst displayWeightUom;
                        if (AccountingModule.isUs()) {
                            displayWeightUom = UOMConst.POUND;
                        } else {
                            displayWeightUom = UOMConst.KILOGRAM;
                        }

                        List<String> noUomConversionList = new ArrayList();
                        List<String> zeroWeightProductList = new ArrayList();
                        if (this.checkSoItemValues(displayWeightUom, noUomConversionList, zeroWeightProductList)) {
                            this.orderInfo = (QueryRow) this.repository.getFbOrderInfo(this.soId, displayWeightUom.getId()).get(0);

                            this.orderItemInfo = this.repository.getSOItemInfo(this.soId);
                            this.orderSize = this.repository.getSOFreightInfo(this.soId);


                            shipFromAddressQueryRow = this.repository.getShipFromAddress(this.orderInfo.getString("lgName"));
                            if (shipFromAddressQueryRow == null) {
                                String lgName = this.orderInfo.getString("lgName");
                                UtilGui.showMessageDialog("Please create a Main Office or Ship To address for the " + lgName + " location group in the company addresses tab.", "Missing Location Group Address", 1);
                            } else {


//allow to edit addresses if not valid
                                this.buildShipToAddress();
                                this.buildShipFromAddress();
                                //LOGGER.error("Zip Code: " + this.shipFromAddress.getZip());


                                //this.buildAddressMaps();
                                //LOGGER.error("Valid From: " + String.valueOf(this.isValidFromAddress));
                                //LOGGER.error("Valid To: " + String.valueOf(this.isValidToAddress));

                                if (!this.isValidFromAddress || !this.isValidToAddress) {
                                    //UtilGui.showMessageDialog(this.getErrorMessage("Address Errors"), "Unable to validate addresses", 1);
                                    return;
                                }


                                //freight section
                                //only builds if address are valid
                                FreightShipment freightShipment = null;


                                //LOGGER.error("Total pallets: " + this.orderSize.getInt("numOfPallets"));
                                //LOGGER.error("Total height: " + this.orderSize.getDouble("avgPalletHeight"));
                                //LOGGER.error("Total cubes: " + totalCubes.toString());
                                //LOGGER.error(this.orderSize.getString("totalOrderCubes"));
                                if (this.orderSize != null){

                                    Integer totalCubes = this.orderSize.getInt("totalOrderCubes");

                                    if (this.shippingPluginProperty.isDebugMode()) {
                                        LOGGER.error("Freight Cubes: " + totalCubes + ", Threshold = 110592");
                                    }


                                    if (totalCubes > 110592) { //48*48*48
                                        if (this.shippingPluginProperty.isDebugMode()) {
                                            LOGGER.error("SO meets freight size");
                                        }

                                        //meets minimum size
                                        //build FLI specific Address
                                        freightShipment = this.buildFreightShipmentAddresses();

                                        //LOGGER.error("Freight shipment built");

//                                try {
                                        //build pallets here too
                                        this.buildFreightShipmentPallets(freightShipment,
                                                this.orderSize.getInt("numOfPallets"),
                                                this.orderSize.getDouble("avgPalletHeight"),
                                                (double) this.orderSize.getInt("totalOrderCubes"));

                                        //LOGGER.error("Freight pallets built");
//                                }
//                                catch (Exception ex){
//                                    LOGGER.error(ex.getMessage(), ex);
//                                }
                                    }
                                }
                                //if the errors != null we set the iSValid to false
                                if (this.shipToError != null && !this.shipToError.isEmpty()) {
                                    //valid but warnings or adjustments
                                    //corrected address is pushed back to FB
                                    UtilGui.showMessageDialog(this.getErrorMessage("Address Messages") + System.getProperty("line.separator") +
                                            "These changes have been saved on the SO", "Addresses have been adjusted", 1);

                                }

//LOGGER.error("asdkfdsf");

                                this.shipRatesDialog = new ShipRatesDialog(this.repository, UtilGui.getParentFrame(), this.orderInfo,
                                        this.shippingPluginProperty, this.getParentPanel(), this.shipToAddress, this.shipFromAddress, this, zeroWeightProductList,
                                        this.orderItemInfo, freightShipment, this.orderSize);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error: ", e);
        }
    }


    private void buildAddressMaps() {
    this.shipToError = new ArrayList<String>();
    this.shipFromError = new ArrayList<String>();
    this.shippoToId = null;
    this.shippoFromId = null;
    this.isValidToAddress = false;
    this.isValidFromAddress = false;

    if (this.shippoToId == null) {
        final Map<String, Object> shipToAddressMap = new HashMap<String, Object>();
        //shipToAddressMap.put("object_purpose", "QUOTE");
        final String street = ShippoUtil.StreetFirstLineCheck(this.orderInfo.getString("shiptoaddress"));
        shipToAddressMap.put("name",this.orderInfo.getString("shipToName"));
        shipToAddressMap.put("street1", street);
        shipToAddressMap.put("zip", this.orderInfo.getString("shipToZip"));
        //shipToAddressMap.put("validate", "false");
        shipToAddressMap.put("country", this.orderInfo.getString("shipToCountry"));
        shipToAddressMap.put("state", this.orderInfo.getString("shipToState"));
        shipToAddressMap.put("city", this.orderInfo.getString("shipToCity"));
        try {
            this.shippoToId = Address.create(shipToAddressMap, this.shippingPluginProperty.getShippoApiToken()).getObjectId();

            final Address addressTo = Address.validate(this.shippoToId, this.shippingPluginProperty.getShippoApiToken());

            if (addressTo.getValidationResults() == null){
                throw new APIConnectionException("Check Shippo API version");

            }
            isValidToAddress = addressTo.getValidationResults().getIsValid();

            LOGGER.error("To address validation: " + this.shippoToId + " - " +  isValidToAddress);

            if (!isValidToAddress) {
                //this.shipToError = new ArrayList<String>();
                this.shipToError.add("Ship To Address cannot be verified.");
            }
            else {
                //is valid but shippo made some adjustments
                if (addressTo.getValidationResults().getValidationMessages().size() > 0) {

                    //once the client library is updates to support type then we can check it
                    //compare all of the fields to see the changes
//LOGGER.error("Messages are not blank");

                    for (Address.ValidationMessage message: addressTo.getValidationResults().getValidationMessages() ) {
//LOGGER.error(message.getCode() + " - " + message.getText());

                        //this.shipToError.add(message.getText());
                    }

                    String changes = this.compareAddressFields(addressTo, this.orderInfo);
                    this.shipToError.add(changes);
                    //this.updateSoAddress(this.orderInfo.getString("num"),addressTo);
                    this.reloadObject(); //shows changes on the SO module

                    //we need to upadate our soInfo now that we have changes the address
                    this.orderInfo = (QueryRow)this.repository.getFbOrderInfo(this.soId, UOMConst.POUND.getId()).get(0);

                    //this.isValidToAddress = false;



                }
            }
        }
        catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException ex3) {
            LOGGER.error(ex3.getMessage(), (Throwable)ex3);
            this.shipToError.add(ex3.getMessage());
        }
        catch (Exception e){
            LOGGER.error(e.getMessage(), e);
        }
    }

    if (this.shippoFromId == null) {

        final Map<String, Object> fromAddressMap = new HashMap<String, Object>();
        //fromAddressMap.put("object_purpose", "QUOTE");
        final String street = ShippoUtil.StreetFirstLineCheck(this.shipFromAddressQueryRow.getString("street"));
        fromAddressMap.put("name",this.shipFromAddressQueryRow.getString("name"));
        fromAddressMap.put("street1", street);
        fromAddressMap.put("zip", this.shipFromAddressQueryRow.getString("zip"));
        //fromAddressMap.put("validate", "false");
        fromAddressMap.put("country", this.shipFromAddressQueryRow.getString("countryName"));
        fromAddressMap.put("state", this.shipFromAddressQueryRow.getString("stateCode"));
        fromAddressMap.put("city", this.shipFromAddressQueryRow.getString("city"));
        try {
            this.shippoFromId = Address.create(fromAddressMap, this.shippingPluginProperty.getShippoApiToken()).getObjectId();

            final Address addressFrom = Address.validate(this.shippoFromId, this.shippingPluginProperty.getShippoApiToken());

            //we've already tested this and we are not moving so lets just set to true
            isValidFromAddress = true;//addressFrom.getValidationResults().getIsValid();

            //LOGGER.error("From address validation: " + this.shippoFromId + " - " + isValidFromAddress);

            if (!isValidFromAddress) {
                this.shipFromError.add("Ship From Address cannot be verified.");
            }
            //removed so we dont have unnecessary errors showing up
//                if (addressFrom.getValidationResults().getValidationMessages().size() > 0){
//                    //shippo made some adjustments
//                    for (Address.ValidationMessage message: addressFrom.getValidationResults().getValidationMessages() ) {
//                        this.shipFromError.add(message.getText());
//                    }
//
//                }
        }
        catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException ex4) {
            LOGGER.error(ex4.getMessage(), (Throwable)ex4);
            this.shipFromError.add(ex4.getMessage());
        }
    }
}

    private void buildShipFromAddress() {
        //if (this.shipFromAddress == null) {

            this.shipFromAddress = new com.fbi.fbdata.accounts.Address();

            final String street = ShippoUtil.StreetFirstLineCheck(this.shipFromAddressQueryRow.getString("street"));
            shipFromAddress.setStreet(street);
            shipFromAddress.setCity(this.shipFromAddressQueryRow.getString("city"));
            shipFromAddress.setState(new State(null, this.shipFromAddressQueryRow.getString("stateCode"), 0));
            shipFromAddress.setZip(this.shipFromAddressQueryRow.getString("zip"));
            shipFromAddress.setAttn(this.shipFromAddressQueryRow.getString("name"));
            shipFromAddress.setCountry(new Country(this.shipFromAddressQueryRow.getString("countryName"), null));

        //}
        this.isValidFromAddress = true;

    }

    private void buildShipToAddress() {
        //this.shipToError = new ArrayList<String>();

        this.isValidToAddress = false;


        this.shipToAddress = new com.fbi.fbdata.accounts.Address();

        final String street = ShippoUtil.StreetFirstLineCheck(this.orderInfo.getString("shiptoaddress"));
        shipToAddress.setStreet(street);
        shipToAddress.setCity(this.orderInfo.getString("shipToCity"));
        shipToAddress.setState(new State(null, this.orderInfo.getString("shipToState"), 0));
        shipToAddress.setZip(this.orderInfo.getString("shipToZip"));
        shipToAddress.setAttn(this.orderInfo.getString("shipToName"));
        shipToAddress.setName(this.orderInfo.getString("shipToName"));
        shipToAddress.setCountry(new Country(this.orderInfo.getString("shipToCountry"), null));

        try {
            //validate address
            ValidateAddress vaRequest = new ValidateAddress(shipToAddress);
            if (vaRequest.Validate()) {
                //valid address
                //show address changes
                this.isValidToAddress = true;
                this.shipToError = vaRequest.getMessageList().get(shipToAddress.getName());

                //TODO: if shipToError.size() > 0
                if (this.shipToError != null && !this.shipToError.isEmpty()) {
                    LOGGER.error("Message list has items");

                    LOGGER.error(this.shipToError.toString());
                    this.updateSoAddress(this.orderInfo.getString("num"), shipToAddress);
                    this.reloadObject(); //shows changes on the SO module
                }
            } else {
                //get error messages
                UtilGui.showMessageDialog(vaRequest.getOutputMessage());
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }


    }

    private String compareAddressFields(Address addressTo, QueryRow orderInfo) {
        StringBuilder sb = new StringBuilder();
        final String street = ShippoUtil.StreetFirstLineCheck(this.orderInfo.getString("shiptoaddress"));

        if (!addressTo.getStreet1().toString().equalsIgnoreCase(street)) {
            sb.append("Old Street: ");
            sb.append(street);
            sb.append(", New Street: ");
            sb.append(addressTo.getStreet1().toString());
            sb.append(System.getProperty("line.separator"));

        }
        if (!addressTo.getCity().toString().equalsIgnoreCase(this.orderInfo.getString("shipToCity"))) {
            sb.append("Old City: ");
            sb.append(this.orderInfo.getString("shipToCity"));
            sb.append(", New City: ");
            sb.append(addressTo.getCity().toString());
            sb.append(System.getProperty("line.separator"));
        }
        if (!addressTo.getZip().toString().equalsIgnoreCase(this.orderInfo.getString("shipToZip"))) {
            sb.append("Old Zip: ");
            sb.append(this.orderInfo.getString("shipToZip"));
            sb.append(", New Zip: ");
            sb.append(addressTo.getZip().toString());
            sb.append(System.getProperty("line.separator"));
        }

        return  sb.toString();

    }

    private FreightShipment buildFreightShipmentAddresses() {
        FreightShipment shipment = new FreightShipment();

        ShipLocation ship_froms = new ShipLocation();
        ship_froms.setEmail("shipping@briteidea.com");
        ship_froms.setPhone("402-553-1178 x 113");
        ship_froms.setFax("402-553-7402");
        ship_froms.setPickup_contact("Natalie");

        com.BI.Models.Address shipFromAddress = new com.BI.Models.Address();
        shipFromAddress.setValid("1");
        shipFromAddress.setAddress("197091");
        shipFromAddress.setName("BRITE IDEAS DECORATING");
        shipFromAddress.setAddress1("2011 N 156th ST");
        shipFromAddress.setCity_zip("OMAHA");
        shipFromAddress.setZip("68116");
        shipFromAddress.setCountry("US");

        ship_froms.setAddress(shipFromAddress);

        Estimated_Pickup pickup = new Estimated_Pickup();

        //cant do weekend day or holiday probably

        DateFormat dtf = new SimpleDateFormat("MM/dd/yy");
        Calendar c = Calendar.getInstance();
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

        if (dayOfWeek == Calendar.FRIDAY) { // If it's Friday so skip to Monday
            c.add(Calendar.DATE, 3);
        } else if (dayOfWeek == Calendar.SATURDAY) { // If it's Saturday skip to Monday
            c.add(Calendar.DATE, 2);
        } else {
            c.add(Calendar.DATE, 1);
        }
        //System.out.println(dtf.format(localDate)); //2016/11/16

        pickup.setDate(dtf.format(c.getTime()));
        ship_froms.setEstimated_pickup(pickup);



        ShipLocation ship_tos = new ShipLocation();
        ship_tos.setEmail(this.orderInfo.getString("email"));
        ship_tos.setPhone(this.orderInfo.getString("phone"));
        //ship_tos.setFax("402-553-7402");
        ship_tos.setDelivery_contact(this.orderInfo.getString("shipToName"));

        com.BI.Models.Address shipToAddress = new com.BI.Models.Address();
        shipToAddress.setValid("1");
        //shipToAddress.setAddress("");
        //this uses the validates address in memory
        shipToAddress.setName(this.shipToAddress.getName());
        shipToAddress.setAddress1(this.shipToAddress.getStreet());
        shipToAddress.setCity_zip(this.shipToAddress.getState().getCode());
        shipToAddress.setZip(this.shipToAddress.getZip().substring(0,5)); //only can have the 5 digit zip
        shipToAddress.setCountry("US");

        ship_tos.setAddress(shipToAddress);


        shipment.setBtnum("BRIT01");
        shipment.setStatus("15");
        shipment.setShip_froms(ship_froms);
        shipment.setShip_tos(ship_tos);
        shipment.setCustpo(this.orderInfo.getString("num"));



        return shipment;

    }

    private void buildFreightShipmentPallets(FreightShipment shipment, int numOfPallets, Double avgPalletHeight, Double orderCubes) {
        try {
            shipment.setShipment_lines(new ArrayList<ShipmentLine>());

            ShipmentLine pallet = new ShipmentLine();
            pallet.setSort("0");
            pallet.setItempallets(Integer.toString(numOfPallets));
            pallet.setHandling_unit("1");
            pallet.setWeight_per_handling_unit(Double.toString(this.orderInfo.getDouble("totalWeight") / numOfPallets));
            pallet.setLength("48.00");
            pallet.setHeight(avgPalletHeight.toString());
            pallet.setWidth("48.00");
            pallet.setItemqty("30"); //TODO: get from FB
            pallet.setPackagetype("3");

            LOGGER.error("Freight Density: " + this.orderInfo.getDouble("totalWeight") / (orderCubes / 1728D));
            FreightClass freightClass = FreightClass.getClassDescription(this.orderInfo.getDouble("totalWeight") / (orderCubes / 1728D));
            pallet.setCnd_search(freightClass.getDescription());
            pallet.setNmfc(freightClass.getNmfc_number());
            pallet.setHazmat("0");


            shipment.getShipment_lines().add(pallet);
        }
        catch(Exception ex){
            LOGGER.error(ex.getMessage(), ex);
        }

    }

    private String getErrorMessage(final String errorMessage) {
        final StringBuilder sb = new StringBuilder(errorMessage);
        if (this.shipToError != null) {
            for (String message : this.shipToError) {
                //sb.append("\nTo address: \n").append(message);
                sb.append(message);
            }

        }
        if (this.shipFromError != null) {
            for (String message : this.shipFromError) {
                sb.append("\nFrom address: \n").append(message);
            }

        }

        return sb.toString();
    }

    
    private boolean checkSoItemValues(final UOMConst displayWeightUom, final List<String> noUomConversionList, final List<String> zeroWeightProductList) {
        for (final QueryRow queryRow : this.repository.checkUomConversions(this.soId, displayWeightUom.getId())) {
            if (queryRow.getDouble("hasConversion") < 0.0 && this.hasWeight(queryRow)) {
                noUomConversionList.add(queryRow.getString("num") + " - \"" + queryRow.getString("fromUom") + "\"");
            }
            if (!this.hasWeight(queryRow)) {
                zeroWeightProductList.add(queryRow.getString("num"));
            }
        }
        if (!noUomConversionList.isEmpty()) {
            final StringBuilder message = new StringBuilder("The quote feature requires either the product UOM to be in ").append(displayWeightUom.getName()).append("s or a conversion rule to be set.\nTo setup a UOM conversion rule, go to Setup - UOM module, select the appropriate UOM, and set the rule on the conversions tab.\nUnable to create quote the because the following product(s) do not have the appropriate UOM conversion setup.\n");
            noUomConversionList.forEach(product -> message.append(product).append("\n"));
            UtilGui.showMessageDialog(message.toString(), "Uom Conversion Missing", 0);
            return false;
        }
        return true;
    }
    
    private boolean hasWeight(final QueryRow queryRow) {
        return queryRow.getDouble("weight") != null && queryRow.getDouble("weight") > 0.0;
    }
    
    private ShipRatesButton getParentPanel() {
        return this;
    }
    
    private ShippingPluginProperty getShippingProperties() {
        //LOGGER.error("Loading Plugin Properties");
        try {
            final ShippingPluginProperty property = new ShippingPluginProperty();

        //LOGGER.error("Loading Plugin Properties - Object created");
        property.setShippoApiToken(Property.SHIPPO_API_TOKEN.get(this));
        property.setDebugMode(Property.DEBUG_MODE.get(this));
        //LOGGER.error("Loading Plugin Properties - Got token");
        property.setUseUps(Property.USE_UPS.get(this));
        property.setUseFedEx(Property.USE_FEDEX.get(this));
        property.setUseUsps(Property.USE_USPS.get(this));
        property.setUseMarkup(Property.USE_MARKUP.get(this));
        property.setUsePercent(Property.USE_PERCENT.get(this));
        property.setMarkupPercent(Property.MARKUP_PERCENT.get(this));
        property.setMarkupRate(Property.MARKUP_RATE.get(this));
        //LOGGER.error("Loading Plugin Properties - Got through easy");
        property.setShippingItemId(Property.SHIPPING_ITEM_ID.get(this));
        property.setShowZeroQtyWarning(Property.SHOW_ZERO_QTY_WARING.get(this));
        //LOGGER.error("Loading Plugin Properties - Got through all");
        return property;
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
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
    
    void addShippingLineToSo(final CarrierRate carrierRate) {
        //LOGGER.error("buttons called");
        if (this.soId <= 0) {
            return;
        }
        final Integer shippingProductId = this.shippingPluginProperty.getShippingItemId();
        final QueryRow product = this.repository.getProductNumAndActive((long)shippingProductId);
        if (product == null) {
            UtilGui.showMessageDialog("The specified shipping item does not exist.", "Add Ship Item Error", 0);
            return;
        }
        if (!Compatibility.getBoolean(this.repository, product, "activeFlag")) {
            UtilGui.showMessageDialog("The specified shipping item is not active.", "Add Ship Item Error", 0);
            return;
        }
        final SalesOrderItem shippingItem = (SalesOrderItem)new SalesOrderItemImpl();
        shippingItem.setProductNumber(product.getString("num"));
        shippingItem.setDescription(carrierRate.getCarrier() + (carrierRate.getShipService() !=null? " - " + carrierRate.getShipService():""));
        shippingItem.setQuantity(Quantity.quantityOne);
        shippingItem.setItemType(SOItemTypeConst.SHIPPING.getId());
        shippingItem.setStatus(SOItemStatusConst.ENTERED.getId());

        //LOGGER.error("before shipping amount");
        Money shippingAmount = new Money(carrierRate.getAmountMarkup());
//        if (this.shippingPluginProperty.isUseMarkup()) {
//            if (this.shippingPluginProperty.isUsePercent()) {
//                shippingAmount = shippingAmount.add(shippingAmount.multiply(this.shippingPluginProperty.getMarkupPercent()));
//            }
//            else {
//                shippingAmount = shippingAmount.add(new Money(this.shippingPluginProperty.getMarkupRate()));
//            }
//        }
        shippingItem.setProductPrice(shippingAmount);
        shippingItem.setTotalPrice(shippingAmount);
        shippingItem.setTaxable(product.getBoolean("taxableFlag"));
        shippingItem.setUOMCode(UOMConst.EACH.getCode().toLowerCase());
        shippingItem.setDateScheduledFulfillment(this.orderInfo.getDate("dateFirstShip"));
        shippingItem.setNewItemFlag(Boolean.TRUE);
        shippingItem.setTaxCode("NON");
        shippingItem.setQBClass(this.orderInfo.getString("qbClassName"));
        if (AccountingModule.isAu()) {
            final String code = product.getString("code");
            if (!this.useAuNzTax(this.orderInfo.getString("shipToCountry"), this.orderInfo.getString("fromCountry")) || code == null) {
                shippingItem.setTaxCode("NON");
            }
            else {
                shippingItem.setTaxCode(code);
            }
        }
        else if (AccountingModule.isCa()) {
            shippingItem.setTaxCode("E");
        }

        //LOGGER.error("Before API call");
        final AddSOItemRequest addSOItemRequest = (AddSOItemRequest)new AddSOItemRequestImpl();
        addSOItemRequest.setOrderNum(this.orderInfo.getString("num"));
        addSOItemRequest.setSOItem(shippingItem);
        try {
            Api.ADD_SO_ITEM.call(this, addSOItemRequest);
        }
        catch (FishbowlException e) {
            UtilGui.showMessageDialog("Unable to add shipping cost to the sales order.", "Order Item Error", 0);
        }
        if (this.repository.getDatabaseVersion() >= 100) {
            this.updateSalesOrder(this.orderInfo.getString("num"), carrierRate);
        }
        this.shipRatesDialog.dispose();
        this.reloadObject();
    }
    
    private void updateSalesOrder(final String soNum, final CarrierRate carrierRate) {
        final ArrayList<String> soCsv = this.mapSo(soNum, carrierRate);
        final ImportRequest importRequest = (ImportRequest)new ImportRequestImpl();
        importRequest.setImportType("ImportSalesOrderDetails");
        importRequest.setRows((ArrayList)soCsv);
        try {
            if (Api.IMPORT.call(this, importRequest).getStatusCode() != FbiMessage.SUCCESS.getId()) {
                UtilGui.showMessageDialog("Unable to update carrier and service on sales order.", "Update Sales Order Error", 0);
            }
        }
        catch (FishbowlException e) {
            UtilGui.showMessageDialog("Unable to update carrier and service on sales order.", "Update Sales Order Error", 0);
        }
    }

    private void updateSoAddress(final String soNum, final com.fbi.fbdata.accounts.Address address){
        final ArrayList<String> soCsv = this.mapSoAddress(soNum, address);
        final ImportRequest importRequest = (ImportRequest)new ImportRequestImpl();
        importRequest.setImportType("ImportSalesOrderDetails");
        importRequest.setRows((ArrayList)soCsv);
        try {
            if (Api.IMPORT.call(this, importRequest).getStatusCode() != FbiMessage.SUCCESS.getId()) {
                UtilGui.showMessageDialog("Unable to update ship-to address on sales order.", "Update Sales Order Error", 0);
            }
        }
        catch (FishbowlException e) {
            UtilGui.showMessageDialog("Unable to update ship-to address on sales order.", "Update Sales Order Error", 0);
        }
    }
    
    private ArrayList<String> mapSo(final String soNum, final CarrierRate carrierRate) {
        final StringRowData soHeaderRow = new StringRowData(new String[] { "SONum", "CarrierName", "CarrierService" });
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(soHeaderRow.toString());
        final StringRowData orderRow = new StringRowData(soHeaderRow.size());
        orderRow.setColumnNames(soHeaderRow);
        orderRow.set("SONum", soNum);
        orderRow.set("CarrierName", carrierRate.getCarrier());
        //LOGGER.error("TOKEN: " + carrierRate.getToken());

        final ShipServiceEnum shipService = ShipServiceEnum.getEnum(carrierRate.getToken());

        if (shipService != null) {
            orderRow.set("CarrierService", shipService.getFbService());
        }
        importRows.add(orderRow.toString());
        return importRows;
    }

    private ArrayList<String> mapSoAddress(final String soNum, final com.fbi.fbdata.accounts.Address newAddress) {
        final StringRowData soAddressHeaderRow = new StringRowData(new String[] { "SONum", "ShipToname", "ShipToAddress", "ShipToCity", "ShipToState", "ShipToZip" });
        final ArrayList<String> importRows = new ArrayList<String>();
        importRows.add(soAddressHeaderRow.toString());
        final StringRowData addressRow = new StringRowData(soAddressHeaderRow.size());
        addressRow.setColumnNames(soAddressHeaderRow);
        addressRow.set("SONum", soNum);
        addressRow.set("ShipToname", newAddress.getName().toString());
        addressRow.set("ShipToAddress", newAddress.getStreet().toString());
        addressRow.set("ShipToCity", newAddress.getCity().toString());
        addressRow.set("ShipToState", newAddress.getState().toString());
        addressRow.set("ShipToZip", newAddress.getZip().toString());

        importRows.add(addressRow.toString());
        return importRows;
    }


    
    private boolean useAuNzTax(final String shipCountry, final String fromCountry) {
        return AccountingModule.isAu() && (CountryEnum.get(shipCountry).equals(CountryEnum.AUSTRALIA) || CountryEnum.get(shipCountry).equals(CountryEnum.NEW_ZEALAND)) && (!CountryEnum.get(shipCountry).equals(CountryEnum.AUSTRALIA) || !CountryEnum.get(fromCountry).equals(CountryEnum.NEW_ZEALAND)) && (!CountryEnum.get(shipCountry).equals(CountryEnum.NEW_ZEALAND) || !CountryEnum.get(fromCountry).equals(CountryEnum.AUSTRALIA));
    }

    static {
        LOGGER = LoggerFactory.getLogger((Class)ShipRatesButton.class);
    }
}
