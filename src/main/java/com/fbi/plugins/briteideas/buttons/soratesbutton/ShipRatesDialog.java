// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.soratesbutton;

import com.BI.Models.Carriers;
import com.BI.Models.FreightShipment;
import com.fbi.fbdata.accounts.Address;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import org.apache.axis2.databinding.types.NonNegativeInteger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.table.TableModel;
import com.fbi.integration.ups.UnitOfMeasurement;
import com.fbi.integration.ups.PackageWeight;
import java.awt.Component;
import com.evnt.eve.modules.AccountingModule;

import javax.swing.DefaultComboBoxModel;
import java.awt.Container;
import java.awt.CardLayout;

import com.shippo.model.Shipment;
import com.shippo.Shippo;

import java.util.stream.Collectors;

import com.fbi.integration.ups.Dimensions;
import com.fbi.sdk.constants.UOMConst;
import com.fbi.commerce.shipping.util.NumberUtil;
import java.math.BigDecimal;
import java.awt.Image;
import com.fbi.gui.util.GenericBouncyDialog;
import com.shippo.exception.APIException;
import com.shippo.exception.APIConnectionException;
import com.shippo.exception.InvalidRequestException;
import com.shippo.exception.AuthenticationException;
import com.fbi.gui.util.UtilGui;
import com.evnt.util.Util;

import java.awt.Frame;
import javax.swing.JComboBox;
import com.fbi.gui.textfield.FBTextFieldQuantity;
import javax.swing.JButton;
import com.fbi.gui.table.FBTable;
import javax.swing.JLabel;
import com.fbi.gui.misc.IconTitleBorderPanel;
import com.fbi.fbo.impl.dataexport.QueryRow;
import com.fbi.commerce.shipping.util.rowdata.CarrierRate;
import com.fbi.commerce.shipping.util.ShippingPluginProperty;
import com.fbi.integration.ups.Package;
import com.evnt.client.modules.so.filters.SOShipCartonRowData;
import com.fbi.gui.table.FBTableModel;
import org.slf4j.Logger;
import javax.swing.JDialog;
import com.BI.*;
import com.fedex.ws.rate.v22.*;

final class ShipRatesDialog extends JDialog
{
    private static final Logger LOGGER;
    private static final String PNL_RATES_NAME = "pnlRates";
    private static final String QUOTE = "QUOTE";
    private static final String OBJECT_PURPOSE = "object_purpose";
    private String cartonName;
    private FBTableModel<SOShipCartonRowData> tableModel;
    private int cartonNumber;
    private List<Package> packageList;
    private final double totalWeight;
    private String shippoApiToken;

    private Address shipFromAddress;
    private String shippoFromError;

    private Address shipToAddress;
    private String shippoToError;

    private String shipPackagesError;

    private ShippingPluginProperty shippingPluginProperty;
    private Map<String, List<CarrierRate>> carrierRateMap;
    private ShipRatesButton shipRatesButton;
    private SOShipCartonRowData selectedRow;
    private Repository repository;
    private QueryRow orderInfo;
    //private QueryRow shipFromAddress;

    private QueryRow orderSize;
    private List<QueryRow> soItemInfo;
    private FreightShipment freightShipment;

    private PropertyGetter propertyGetter;
    private RatesPanel pnlRates;
    private boolean showingRates;
    private IconTitleBorderPanel pnlCartons;
    private JLabel txtOrderTotalWeight;
    private JLabel txtRemainingWeight;
    private FBTable tblCartons;
    private JButton btnRemoveCarton;

    private JButton btnCombineCartons;

    private IconTitleBorderPanel pnlCartonDetails;
    private JLabel lblWeight;
    private FBTextFieldQuantity txtWeight;
    private JComboBox cboWeightUom;
    private JLabel lblLength;
    private FBTextFieldQuantity txtLength;
    private JLabel lblWidth;
    private FBTextFieldQuantity txtWidth;
    private JLabel lblHeight;
    private FBTextFieldQuantity txtHeight;
    private JComboBox cboSizeUOM;
    private JLabel label1;
    private JButton btnGetRates;
    private IconTitleBorderPanel tbPnlRates;

    private RateRequest rateRequest;

    ShipRatesDialog(final Repository repository, final Frame owner, final QueryRow orderInfo, final ShippingPluginProperty shippingPluginProperty,
                    final ShipRatesButton shipRatesButton, final Address ShipToAddress, final Address ShipFromAddress, final PropertyGetter plugin, final List<String> zeroWeightProductList,
                    List<QueryRow> soItemInfo, FreightShipment freightShipment, QueryRow orderSize) {
        super(owner);
        this.cartonNumber = 1;
        //LOGGER.error("Carton");
        this.packageList = new ArrayList<Package>();

        this.soItemInfo = soItemInfo;
        this.freightShipment = freightShipment;
        this.orderSize = orderSize;

        this.initComponents();
        this.repository = repository;
        this.cartonName = (Util.isEmpty(orderInfo.getString("cartonName")) ? "Carton" : orderInfo.getString("cartonName"));
        this.totalWeight = ((orderInfo.getDouble("totalWeight") != null) ? orderInfo.getDouble("totalWeight") : 0.0);
        this.shipRatesButton = shipRatesButton;
        this.carrierRateMap = new HashMap<String, List<CarrierRate>>();
        this.shippingPluginProperty = shippingPluginProperty;
        this.shippoApiToken = shippingPluginProperty.getShippoApiToken();
        //LOGGER.error(this.shippoApiToken);
        this.orderInfo = orderInfo;
        //this.shipFromAddress = shipFromAddress;
        this.shipFromAddress = ShipFromAddress;
        this.shipToAddress = ShipToAddress;

        this.propertyGetter = plugin;
        this.init(this.cartonName);
        this.setVisible(true);
        if (shippingPluginProperty.isShowZeroQtyWarning() && !zeroWeightProductList.isEmpty()) {
            final StringBuilder message = new StringBuilder("The following products have a weight of zero, if this is not correct, please update these products before continuing to get the most accurate quote.\n");
            zeroWeightProductList.forEach(product -> message.append(product).append("\n"));
            UtilGui.showMessageDialog(message.toString(), "Zero Weight Products", 2);
        }

        //LOGGER.error("Done init");
    }

//    private void buildAddressMaps() {
//        if (this.shipToAddress == null) {
//            final Map<String, Object> shipToAddressMap = new HashMap<String, Object>();
//            //shipToAddressMap.put("object_purpose", "QUOTE");
//            final String street = ShippoUtil.StreetFirstLineCheck(this.orderInfo.getString("shiptoaddress"));
//            shipToAddressMap.put("name",this.orderInfo.getString("shipToName"));
//            shipToAddressMap.put("street1", street);
//            shipToAddressMap.put("zip", this.orderInfo.getString("shipToZip"));
//            //shipToAddressMap.put("validate", "false");
//            shipToAddressMap.put("country", this.orderInfo.getString("shipToCountry"));
//            shipToAddressMap.put("state", this.orderInfo.getString("shipToState"));
//            shipToAddressMap.put("city", this.orderInfo.getString("shipToCity"));
//            try {
//                final Address addressTo = Address.create(shipToAddressMap, this.shippoApiToken);
//                this.shipToAddress = addressTo.getObjectId();
//                //LOGGER.error("To address ID: " + this.shipToAddress);
//                LOGGER.error("To address validation: " + addressTo.getValidationResults().getIsValid());
//                if (!addressTo.getValidationResults().getIsValid()) {
//                    this.shippoToError = "Address cannot be verified.";
//                }
//            }
//            catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException ex3) {
//                ShipRatesDialog.LOGGER.error(ex3.getMessage(), (Throwable)ex3);
//                this.shippoToError = ex3.getMessage();
//            }
//        }
//        if (this.shipFromAddress == null) {
//
//            final Map<String, Object> fromAddressMap = new HashMap<String, Object>();
//            //fromAddressMap.put("object_purpose", "QUOTE");
//            final String street = ShippoUtil.StreetFirstLineCheck(this.shipFromAddress.getString("street"));
//            fromAddressMap.put("name",this.shipFromAddress.getString("name"));
//            fromAddressMap.put("street1", street);
//            fromAddressMap.put("zip", this.shipFromAddress.getString("zip"));
//            //fromAddressMap.put("validate", "false");
//            fromAddressMap.put("country", this.shipFromAddress.getString("countryName"));
//            fromAddressMap.put("state", this.shipFromAddress.getString("stateCode"));
//            fromAddressMap.put("city", this.shipFromAddress.getString("city"));
//            try {
//                final Address addressFrom = Address.create(fromAddressMap, this.shippoApiToken);
//
//                this.shipFromAddress = addressFrom.getObjectId();
//
//                boolean isValidAddress = Address.validate(this.shipFromAddress, this.shippoApiToken).getValidationResults().getIsValid();
//                //LOGGER.error("From address ID: " + this.shipFromAddress);
//                LOGGER.error("From address validation: " + isValidAddress);
//
////                if (!addressFrom.getValidationResults().getIsValid()) {
////                    this.shippoFromError = "Address cannot be verified.";
////                }
//                if (!isValidAddress) {
//                    this.shippoFromError = "Address cannot be verified.";
//                }
//            }
//            catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException ex4) {
//                ShipRatesDialog.LOGGER.error(ex4.getMessage(), (Throwable)ex4);
//                this.shippoFromError = ex4.getMessage();
//            }
//        }
//    }

    private void init(final String cartonName) {
    final String[] sizeUOMList = { UOMConst.INCH.getCode(), UOMConst.CENTIMETER.getCode() };
    this.cboSizeUOM.setModel(new DefaultComboBoxModel<String>(sizeUOMList));
    final String[] weightUomList = { UOMConst.POUND.getCode(), UOMConst.KILOGRAM.getCode() };
    this.cboWeightUom.setModel(new DefaultComboBoxModel<String>(weightUomList));
    this.setDefaultCboSelections();
    this.pnlCartons.setTitle(this.pnlCartons.getTitle().replace("{0}", cartonName));
    this.pnlCartonDetails.setTitle(this.pnlCartonDetails.getTitle().replace("{0}", cartonName));
    this.txtOrderTotalWeight.setText(Double.toString(this.totalWeight));
    this.txtRemainingWeight.setText(Double.toString(0.0));


    if (this.soItemInfo != null && this.soItemInfo.size() > 0){
        //there are inventoried items //TODO: might want to change sql later if we make misc items with certain box sizes
        //make a carton for each one based on the default carton size
        //remaining weight can go in a seperate carton (misc items etc)

        double runningWeight = new Double(0);

        DecimalFormat dimsFormat = new DecimalFormat("#.##");

        for (QueryRow row: this.soItemInfo) {

            Dimensions packageDims = new Dimensions();
            packageDims.setLength(dimsFormat.format(row.getDouble("len")));
            packageDims.setHeight(dimsFormat.format(row.getDouble("height")));
            packageDims.setWidth(dimsFormat.format(row.getDouble("width")));


            if (row.getInt("totalCartons") > 1) {

                this.btnRemoveCarton.setEnabled(true);

                double weightRemaining = new Double(row.getDouble("qtyOrdered") * row.getDouble("weight"));
                double defaultCartonWeight = new Double(Math.ceil(row.getDouble("pcsPerCarton") * row.getDouble("weight")*10)/10);

                //default carton size set
                //create cartons for each division
                for (int i = 0; i < row.getInt("totalCartons") - 1 ; i++) {
                    //skip the last "package" so we can just use the qty remaining

                    if (this.shippingPluginProperty.isDebugMode())
                    {
                        LOGGER.error("Carton #: " + i + ", Carton Item: " + row.getString("num") + ", Item Weight: " + defaultCartonWeight);
                    }

                    this.addPackage(defaultCartonWeight, packageDims);
                    runningWeight = runningWeight + defaultCartonWeight;

                    weightRemaining = weightRemaining - defaultCartonWeight;
                }
                //now we have fulfilled the full cartons, fill the last one
                this.addPackage(Math.ceil(weightRemaining*10)/10, packageDims);
                if (this.shippingPluginProperty.isDebugMode())
                {
                    LOGGER.error("Last Item Carton, Carton Item: " + row.getString("num") + ", Item Weight: " + Math.ceil(weightRemaining*10)/10);
                }
                runningWeight = runningWeight + Math.ceil(weightRemaining*10)/10;



            }
            else{//just one carton

                if (this.shippingPluginProperty.isDebugMode())
                {
                    LOGGER.error("Single Carton, Carton Item: " + row.getString("num") + ", Item Weight: " + (row.getDouble("qtyOrdered") * row.getDouble("weight")));
                }
                this.addPackage(row.getDouble("qtyOrdered") * row.getDouble("weight"), packageDims);
                runningWeight = runningWeight + row.getDouble("qtyOrdered") * row.getDouble("weight");
            }
        }
        if (runningWeight < this.totalWeight){
            //must be remaining misc items with weights
            if (this.shippingPluginProperty.isDebugMode())
            {
                LOGGER.error("Misc Remaining Weight, Carton Item: No Item, Item Weight: " + Math.ceil((this.totalWeight- runningWeight)*10)/10);
            }
            if (this.totalWeight-runningWeight > 1D) {
                this.addPackage(Math.ceil((this.totalWeight - runningWeight) * 10) / 10);
            }
        }
    }
    else {
        //all items are misc in nature so do a single carton
        this.addPackage(this.totalWeight);
    }





    this.enableCartonDetails(false);
    this.manageCarrierTabs();
}

    private void getRates() {
    if (this.shippingPluginProperty.isUspsOnlySelected() && this.tableModel.getRowCount() > 1) {
        UtilGui.showMessageDialog("USPS doesn't return rates for shipments with more than one " + this.cartonName + ".", "Retrieve Rates Failed", 1);
        return;
    }
    if (this.validatePackageList(this.packageList)) {
        //TODO: change the freight carton info check to pull from the carton list in case changes are made
        new GenericBouncyDialog("Retrieving Rates", "Processing...", (Image)null, this::populateRatesTable);
    }
}

    private boolean validatePackageList(final List<Package> packageList) {
        final boolean weightError = packageList.stream().map(pack -> Double.parseDouble(pack.getPackageWeight().getWeight())).anyMatch(weight -> weight <= 0.0);
        if (weightError) {
            UtilGui.showMessageDialog("All " + this.cartonName + "(s) must have a weight value.", "Missing Weights", 1);
            return false;
        }
        return true;
    }

    private String getErrorMessage(final String errorMessage) {
        final StringBuilder sb = new StringBuilder(errorMessage);
        if (this.shippoToError != null) {
            sb.append("\nProblem with your to address: ").append(this.shippoToError);
        }
        if (this.shippoFromError != null) {
            sb.append("\nProblem with your from address: ").append(this.shippoFromError);
        }
        return sb.toString();
    }



    private void setupFedexRequest(){

        rateRequest = new RateRequest();
        rateRequest.setClientDetail(createClientDetail());
        rateRequest.setWebAuthenticationDetail(createWebAuthenticationDetail());
        rateRequest.setReturnTransitAndCommit(true);
        //
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setCustomerTransactionId("Fishbowl - Rate Request"); // The client will get the same value back in the response
        rateRequest.setTransactionDetail(transactionDetail);


        VersionId versionId = new VersionId();
        versionId.setServiceId("crs");
        versionId.setMajor(22);
        versionId.setMinor(0);

        rateRequest.setVersion(versionId);



    }
    private void setFedexShipmentDetails(){

        boolean getAllRatesFlag = true; // set to true to get the rates for different service types
        RequestedShipment requestedShipment = new RequestedShipment();

        requestedShipment.setShipTimestamp(Calendar.getInstance());
        requestedShipment.setDropoffType(DropoffType.REGULAR_PICKUP);
        if (! getAllRatesFlag) {
            requestedShipment.setServiceType(ServiceType.PRIORITY_OVERNIGHT);
            requestedShipment.setPackagingType(PackagingType.YOUR_PACKAGING);
        }

        //Brite Ideas
        Party shipper = new Party();
        com.fedex.ws.rate.v22.Address shipperAddress = new com.fedex.ws.rate.v22.Address(); // Origin information
        shipperAddress.setStreetLines(new String[] {this.shipFromAddress.getStreet()});
        shipperAddress.setCity(this.shipFromAddress.getCity());
        shipperAddress.setStateOrProvinceCode(this.shipFromAddress.getState().getCode());
        shipperAddress.setPostalCode(this.shipFromAddress.getZip());
        shipperAddress.setCountryCode("US");
        shipper.setAddress(shipperAddress);
        requestedShipment.setShipper(shipper);

        //LOGGER.error(shipper.getAddress().toString());
        LOGGER.error(shipper.getAddress().getPostalCode());


        Party recipient = new Party();
        com.fedex.ws.rate.v22.Address recipientAddress = new com.fedex.ws.rate.v22.Address(); // Destination information
        recipientAddress.setStreetLines(new String[] {this.shipToAddress.getStreet()});
        recipientAddress.setCity(this.shipToAddress.getCity());
        recipientAddress.setStateOrProvinceCode(this.shipToAddress.getState().getCode());
        recipientAddress.setPostalCode(this.shipToAddress.getZip());
        recipientAddress.setCountryCode("US");
        recipient.setAddress(recipientAddress);
        requestedShipment.setRecipient(recipient);

        //
        Payment shippingChargesPayment = new Payment();
        shippingChargesPayment.setPaymentType(PaymentType.SENDER);
        requestedShipment.setShippingChargesPayment(shippingChargesPayment);

        rateRequest.setRequestedShipment(requestedShipment);
    }
    private static boolean isResponseOk(NotificationSeverityType notificationSeverityType) {
        if (notificationSeverityType == null) {
            return false;
        }
        if (notificationSeverityType.equals(NotificationSeverityType.WARNING) ||
                notificationSeverityType.equals(NotificationSeverityType.NOTE)    ||
                notificationSeverityType.equals(NotificationSeverityType.SUCCESS)) {
            return true;
        }
        return false;
    }
    private static WebAuthenticationDetail createWebAuthenticationDetail() {
        WebAuthenticationCredential userCredential = new WebAuthenticationCredential();
        String key = System.getProperty("key");
        String password = System.getProperty("password");
        //
        // See if the key and password properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (key == null) {
            key = "C16YogVXzdSLQTp5"; // Replace "XXX" with clients key
        }
        if (password == null) {
            password = "GhILgM8csJenzeRrcObn3rjmg"; // Replace "XXX" with clients password
        }
        userCredential.setKey(key);
        userCredential.setPassword(password);

        WebAuthenticationCredential parentCredential = null;
        Boolean useParentCredential=false; //Set this value to true is using a parent credential
        if(useParentCredential){

            String parentKey = System.getProperty("parentkey");
            String parentPassword = System.getProperty("parentpassword");
            //
            // See if the parentkey and parentpassword properties are set,
            // if set use those values, otherwise default them to "XXX"
            //
            if (parentKey == null) {
                parentKey = "XXX"; // Replace "XXX" with clients parent key
            }
            if (parentPassword == null) {
                parentPassword = "XXX"; // Replace "XXX" with clients parent password
            }
            parentCredential = new WebAuthenticationCredential();
            parentCredential.setKey(parentKey);
            parentCredential.setPassword(parentPassword);
        }
        WebAuthenticationDetail webAuthenticationDetail = new WebAuthenticationDetail();
        webAuthenticationDetail.setParentCredential(parentCredential);
        webAuthenticationDetail.setUserCredential(userCredential);
        return webAuthenticationDetail;
    }
    private static ClientDetail createClientDetail() {
        ClientDetail clientDetail = new ClientDetail();
        String accountNumber = System.getProperty("accountNumber");
        String meterNumber = System.getProperty("meterNumber");

        //
        // See if the accountNumber and meterNumber properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (accountNumber == null) {
            accountNumber = "686650464"; // Replace "XXX" with clients account number
        }
        if (meterNumber == null) {
            meterNumber = "112289105"; // Replace "XXX" with clients meter number
        }
        clientDetail.setAccountNumber(accountNumber);
        clientDetail.setMeterNumber(meterNumber);
        return clientDetail;
    }

    private Map<String, Object> packageToMap(final Package pack) {
        //try {
            final Map<String, Object> parcelMap = new HashMap<String, Object>();
            final Dimensions dimensions = pack.getDimensions();
            parcelMap.put("length", NumberUtil.BDEquals(new BigDecimal(dimensions.getLength()), BigDecimal.ZERO) ? "10" : dimensions.getLength());
            parcelMap.put("width", NumberUtil.BDEquals(new BigDecimal(dimensions.getWidth()), BigDecimal.ZERO) ? "10" : dimensions.getWidth());
            parcelMap.put("height", NumberUtil.BDEquals(new BigDecimal(dimensions.getHeight()), BigDecimal.ZERO) ? "10" : dimensions.getHeight());
            parcelMap.put("distance_unit", dimensions.getUnitOfMeasurement().getCode().toLowerCase());
            final double weight = Double.parseDouble(pack.getPackageWeight().getWeight()) * (Objects.equals(this.cboWeightUom.getSelectedItem(), UOMConst.KILOGRAM.getCode()) ? 35.27 : 16.0);
//            if (weight == 0D) {
//                throw new FishbowlException("There are still 0 weight packages");
//            }
            parcelMap.put("weight", Math.round(weight * 100.0) / 100L);
            parcelMap.put("mass_unit", "oz");
            return parcelMap;
//        }
//        catch (FishbowlException ex1) {
//            shipPackagesError = ex1.getMessage();
//        }
        //return null;
    }

    private RequestedPackageLineItem packageToFedExPackage(final Package pack) {

        RequestedPackageLineItem rp = new RequestedPackageLineItem();
        rp.setGroupPackageCount(new NonNegativeInteger("1"));
        try {
        final double weight = Double.parseDouble(pack.getPackageWeight().getWeight()) * (Objects.equals(this.cboWeightUom.getSelectedItem(), UOMConst.KILOGRAM.getCode()) ? 2.2 : 1);

        Weight packageWeight = new Weight();
        packageWeight.setUnits(WeightUnits.LB);
        packageWeight.setValue(new BigDecimal(Math.round(weight * 100.0) / 100L));
        rp.setWeight(packageWeight);
        //LOGGER.error("Package weight" + packageWeight.toString());
        //

        final Dimensions dimensions = pack.getDimensions(); //fb carton
        //LOGGER.error("Package dims" + dimensions.toString());
        //
        com.fedex.ws.rate.v22.Dimensions packageDimensions = new com.fedex.ws.rate.v22.Dimensions();
        packageDimensions.setHeight(new NonNegativeInteger(String.valueOf((int)Double.parseDouble(dimensions.getHeight()))));
        packageDimensions.setLength(new NonNegativeInteger(String.valueOf((int)Double.parseDouble(dimensions.getLength()))));
        packageDimensions.setWidth(new NonNegativeInteger(String.valueOf((int)Double.parseDouble(dimensions.getWidth()))));
        packageDimensions.setUnits(LinearUnits.IN);

        rp.setDimensions(packageDimensions);
        //LOGGER.error("Package Details set");

    }
    catch (Exception e){
        LOGGER.error("Error: ",e);
    }
    return rp;

    }

    private RequestedPackageLineItem[] packagesToFedExPackages(List<Package> packages){
        ArrayList<RequestedPackageLineItem> tempList = new ArrayList<>();

        for (Package pack : packages) {
            tempList.add(this.packageToFedExPackage(pack));
        }
        return tempList.toArray(new RequestedPackageLineItem[0]);
    }

    private List<CarrierRate> fedexRateToCarrierRate(RateReplyDetail[] rateReplyDetail){
        ArrayList<CarrierRate> carrierRates = new ArrayList<>();

        for (RateReplyDetail rateDetail: rateReplyDetail) {
            CarrierRate rate = new CarrierRate();

            rate.setAmount(rateDetail.getRatedShipmentDetails()[0].getShipmentRateDetail().getTotalNetCharge().getAmount().doubleValue());
            print("Amount: " ,rate.getAmount());

            Double shippingAmount = rate.getAmount();
            if (this.shippingPluginProperty.isUseMarkup()) {
                if (this.shippingPluginProperty.isUsePercent()) {
                    shippingAmount = shippingAmount + (shippingAmount * (this.shippingPluginProperty.getMarkupPercent()));
                } else {
                    shippingAmount = shippingAmount + (this.shippingPluginProperty.getMarkupRate());
                }
            }

            rate.setAmountMarkup(Math.floor(shippingAmount * 1e2) / 1e2);

            if (rateDetail.getDeliveryDayOfWeek() != null) {
                //LOGGER.error("Rate days is not null");
                Days d = Days.daysBetween(new DateTime(), new DateTime(rateDetail.getDeliveryTimestamp()));
                rate.setDays((double)d.getDays());
            } else {
                //LOGGER.error("Rate days is null");
                rate.setDays(new Double(0.0));
                //LOGGER.error(carrierRate.getDays().toString());
            }
            //rate.setDurationTerms(rate.getDuration_terms().toString());

            //LOGGER.error(rate.getProvider().toString());
            rate.setShipService(rateDetail.getServiceType().getValue());
            rate.setCarrier("FedEx");
            rate.setToken(rateDetail.getServiceType().getValue().toLowerCase());
            LOGGER.error(rate.getToken());

            carrierRates.add(rate);
        }
        return carrierRates;
    }


    private void populateRatesTable() {
        String errorMessage;
        //LOGGER.error("Populate Started");
        try {
            shipPackagesError = null;

//set Fedex Request up
            this.setupFedexRequest();
            //LOGGER.error("Fedex Request Created");
//set addresses and shipment defaults
            this.setFedexShipmentDetails();
            //LOGGER.error("Fedex Shipment Details Set");
//set packages
            this.rateRequest.getRequestedShipment().setPackageCount(new NonNegativeInteger(String.valueOf(this.packageList.size())));
            //LOGGER.error("Fedex Package count Set");
            this.rateRequest.getRequestedShipment().setRequestedPackageLineItems(this.packagesToFedExPackages(this.packageList));
            //LOGGER.error("Fedex Shipment packages Set");
            try {
                // Initialize the service
                RateRequestE rateRequestE = new RateRequestE();
                rateRequestE.setRateRequest(rateRequest);

                RateServiceStub serviceStub = new RateServiceStub();

                // This is the call to the web service passing in a RateRequest and returning a RateReply
                RateReplyE replyE = serviceStub.getRates(rateRequestE); // Service call

                RateReply reply = replyE.getRateReply();
                if (isResponseOk(reply.getHighestSeverity())) {
                    writeServiceOutput(reply);

//get rates from reply
                    this.carrierRateMap = new HashMap<>();
                    this.carrierRateMap.put("FedEx", this.fedexRateToCarrierRate(reply.getRateReplyDetails()));

                }
                else{
                    this.pnlRates.populateTables(Collections.emptyMap(), this.packageList.size(), null);
                    errorMessage = this.getErrorMessage("Please check your Ship To and Location Group addresses.");
                    UtilGui.showMessageDialog(errorMessage, "Unable to retrieve rates", 1);
                    return;
                }
                printNotifications(reply.getNotifications());

            } catch (Exception e) {
                e.printStackTrace();
            }
            LOGGER.error("Fedex Shipment done");



//FREIGHT SECTION
            //LOGGER.error("Got all the small package rates");
            //now we need to get and add the freight rates if qualifies
            if (this.freightShipment != null) { //address were added so it qualifies
                List<CarrierRate> freightRates = new ArrayList<CarrierRate>();
                //calculate pallets and add to freight shipment
                //send to fli and then add the returned rates to the list


                FLISession session = new FLISession();
                session.getSession();
                FLISession.setDebug_mode(this.shippingPluginProperty.isDebugMode());
                //iterate of carriers enum and submit shipment to each
                //combine results
                //LOGGER.error("Got session");
                for (Carriers carrier : Carriers.values()) {
                    //send in to fli
                    com.BI.Models.CarrierRate rate = session.getCarrierRate(freightShipment, carrier);
                    //LOGGER.error("Got freight rate");

                    //rate might be null (BROWN)
                    if (rate != null) {
                        CarrierRate FBrate = new CarrierRate();
                        FBrate.setDays(Double.parseDouble(rate.getDuration()));
                        FBrate.setAmount(rate.getRate());
                        //LOGGER.error(rate.getCarrier());
                        FBrate.setCarrier(carrier.getName());
                        FBrate.setToken(carrier.getName());
                        //FBrate.setShipService("Freight"); //leave null so we dont have to set it on the SO

                        FBrate.setDurationTerms(freightShipment.getShipment_lines().get(0).itempallets + " - Pallets");

                        Double shippingAmount = rate.getRate();
                        if (this.shippingPluginProperty.isUseMarkup()) {
                            if (this.shippingPluginProperty.isUsePercent()) {
                                shippingAmount = shippingAmount + (shippingAmount * (this.shippingPluginProperty.getMarkupPercent()));
                            } else {
                                shippingAmount = shippingAmount + (this.shippingPluginProperty.getMarkupRate());
                            }
                        }

                        FBrate.setAmountMarkup(Math.floor(shippingAmount * 1e2) / 1e2);

                        freightRates.add(FBrate);
                        //LOGGER.error("added rate to rates table");
                    }
                }

                //LOGGER.error("Got all the freight rates");
                this.carrierRateMap.put("FLI", freightRates);
            }


            if (!this.showingRates) {
                CardLayout layout = (CardLayout) this.tbPnlRates.getLayout();
                layout.show(this.tbPnlRates, "pnlRates");
                this.showingRates = true;
            }

            this.pnlRates.populateTables(this.carrierRateMap, this.packageList.size(), this.orderSize);

       }
//      catch (InvalidRequestException | APIConnectionException | AuthenticationException | APIException var5) {
//            LOGGER.error(var5.getMessage(), var5);
//            errorMessage = this.getErrorMessage("Please check your Ship To and Location Group addresses.");
//            UtilGui.showMessageDialog(errorMessage, "Unable to retrieve rates", 1);
//        }
        catch (Exception ex1){
            UtilGui.showMessageDialog(ex1.getMessage(), "Package issues", 1);
        }


    }

    public static void writeServiceOutput(RateReply reply) {
        RateReplyDetail[] rrds = reply.getRateReplyDetails();
        for (int i = 0; i < rrds.length; i++) {
            RateReplyDetail rrd = rrds[i];
            print("\nService type", rrd.getServiceType());
            print("Packaging type", rrd.getPackagingType());
            print("Delivery DOW", rrd.getDeliveryDayOfWeek());
            if(rrd.getDeliveryDayOfWeek() != null){
                int month = rrd.getDeliveryTimestamp().get(Calendar.MONTH)+1;
                int date = rrd.getDeliveryTimestamp().get(Calendar.DAY_OF_MONTH);
                int year = rrd.getDeliveryTimestamp().get(Calendar.YEAR);
                String delDate = new String(month + "/" + date + "/" + year);
                print("Delivery date", delDate);
                print("Calendar DOW", rrd.getDeliveryTimestamp().get(Calendar.DAY_OF_WEEK));
            }

            RatedShipmentDetail[] rsds = rrd.getRatedShipmentDetails();
            for (int j = 0; j < rsds.length; j++) {
                print("RatedShipmentDetail " + j, "");
                RatedShipmentDetail rsd = rsds[j];
                ShipmentRateDetail srd = rsd.getShipmentRateDetail();
                print("  Rate type", srd.getRateType());
                print("  Total Billing weight", srd.getTotalBillingWeight());
                print("  Total surcharges", srd.getTotalSurcharges());
                print("  Total net charge", srd.getTotalNetCharge());

                RatedPackageDetail[] rpds = rsd.getRatedPackages();
                if (rpds != null && rpds.length > 0) {
                    print("  RatedPackageDetails", "");
                    for (int k = 0; k < rpds.length; k++) {
                        print("  RatedPackageDetail " + i, "");
                        RatedPackageDetail rpd = rpds[k];
                        PackageRateDetail prd = rpd.getPackageRateDetail();
                        if (prd != null) {
                            print("    Billing weight", prd.getBillingWeight());
                            print("    Base charge", prd.getBaseCharge());
                            Surcharge[] surcharges = prd.getSurcharges();
                            if (surcharges != null && surcharges.length > 0) {
                                for (int m = 0; m < surcharges.length; m++) {
                                    Surcharge surcharge = surcharges[m];
                                    print("    " + surcharge.getDescription() + " surcharge", surcharge.getAmount());
                                }
                            }
                        }
                    }
                }
            }
            //System.out.println("");
        }
    }
    
    private static void printNotifications(Notification[] notifications) {
        LOGGER.error("Notifications:");
        if (notifications == null || notifications.length == 0) {
            LOGGER.error("  No notifications returned");
        }
        for (int i=0; i < notifications.length; i++){
            Notification n = notifications[i];
            LOGGER.error("  Notification no. " + i + ": ");
            if (n == null) {
                LOGGER.error("null");
                continue;
            } else {
                LOGGER.error("");
            }
            NotificationSeverityType nst = n.getSeverity();

            LOGGER.error("    Severity: " + (nst == null ? "null" : nst.getValue()));
            LOGGER.error("    Code: " + n.getCode());
            LOGGER.error("    Message: " + n.getMessage());
            LOGGER.error("    Source: " + n.getSource());
        }
    }
    private static void print(String msg, Object obj) {
        if (msg == null || obj == null) {
            return;
        }
        System.out.println(msg + ": " + obj.toString());
    }

    private void setDefaultCboSelections() {
        if (AccountingModule.isUs()) {
            this.cboWeightUom.setSelectedIndex(0);
            this.cboSizeUOM.setSelectedIndex(0);
        }
        else {
            this.cboWeightUom.setSelectedIndex(1);
            this.cboSizeUOM.setSelectedIndex(1);
        }
    }

    private void manageCarrierTabs() {
        this.pnlRates = new RatesPanel(this.shippingPluginProperty, this, this.cartonName);
        final NoRatesPanel pnlNoRates = new NoRatesPanel();
        final String pnlNoRatesName = "pnlNoRates";
        this.tbPnlRates.add((Component)pnlNoRates, (Object)pnlNoRatesName);
        this.tbPnlRates.add((Component)this.pnlRates, (Object)"pnlRates");
        final CardLayout layout = (CardLayout)this.tbPnlRates.getLayout();
        layout.show((Container)this.tbPnlRates, pnlNoRatesName);
    }

    private void enableCartonDetails(final boolean enable) {
        this.lblWeight.setEnabled(enable);
        this.txtWeight.setEnabled(enable);
        this.cboWeightUom.setEnabled(enable);
        this.lblLength.setEnabled(enable);
        this.txtLength.setEnabled(enable);
        this.lblWidth.setEnabled(enable);
        this.txtWidth.setEnabled(enable);
        this.lblHeight.setEnabled(enable);
        this.txtHeight.setEnabled(enable);
        this.cboSizeUOM.setEnabled(enable);
    }

    private Package createPackage(final double weight) {
        final Package pack = new Package();
        pack.setId(this.cartonNumber);
        pack.setDescription(this.cartonName + " " + this.cartonNumber++);
        final PackageWeight packageWeight = new PackageWeight(Double.toString(weight));
        String weightUom;
        if (AccountingModule.isUs()) {
            weightUom = UOMConst.POUND.getCode();
        }
        else {
            weightUom = UOMConst.KILOGRAM.getCode();
        }
        packageWeight.setUnitOfMeasurement(new UnitOfMeasurement(weightUom));
        pack.setPackageWeight(packageWeight);
        return pack;
    }

    private void addPackage(final double weight){
        final Dimensions dimensions = new Dimensions();
        dimensions.setHeight("0");
        dimensions.setWidth("0");
        dimensions.setLength("0");
        this.addPackage(weight, dimensions);
    }


    private void addPackage(final double weight, final Dimensions dimensions) {
        final Package pack = this.createPackage(weight);

        String uomCode = UOMConst.INCH.getCode();
        if (AccountingModule.isInternational()) {
            uomCode = UOMConst.CENTIMETER.getCode();
        }
        dimensions.setUnitOfMeasurement(new UnitOfMeasurement(uomCode));
        pack.setDimensions(dimensions);
        this.packageList.add(pack);
        this.recalculateTable();
        this.tblCartons.setModel((TableModel)this.tableModel);
        this.tableModel.setSelectedRowIndex(this.tableModel.getRowCount() - 1);
        this.tblCartonsMouseReleased();
        this.txtWeight.requestFocus();
    }

    private void recalculateTable() {
        int index = -1;
        if (this.tableModel != null) {
            index = this.tableModel.getSelectedRowIndex();
        }

        this.tableModel = new FBTableModel(this.tblCartons, SOShipCartonRowData.getSettings());
        double calculatedWeight = 0.0D;
        Iterator var4 = this.packageList.iterator();

        while(var4.hasNext()) {
            Package pack = (Package)var4.next();
            calculatedWeight += Double.parseDouble(pack.getPackageWeight().getWeight());
            SOShipCartonRowData rowData = new SOShipCartonRowData(pack);
            this.tableModel.addRow(rowData);
        }

        if (calculatedWeight >= this.totalWeight) {
            this.txtRemainingWeight.setText(Double.toString(0.0D));
        } else {
            this.txtRemainingWeight.setText(String.format("%1$,.2f", this.totalWeight - calculatedWeight));
        }

        if (index >= 0) {
            this.tableModel.setSelectedRowIndex(index);
        }

    }

    private void btnAddCartonActionPerformed() {
        if (this.tableModel.getRowCount() >= 10) { //TODO: remove limit?
            UtilGui.showMessageDialog("Cannot exceed more than 10 " + this.cartonName + "'s", this.cartonName + " Max", 1);
            return;
        }
        this.addPackage(0.0);
        this.btnRemoveCarton.setEnabled(true);
    }

    private void tblCartonsMouseReleased() {
        if (this.tableModel.getSelectedRowsData().size() > 1){
            //multiple selected
            //add option to combine
            this.btnCombineCartons.setEnabled(true);
        }
        else {
            this.btnCombineCartons.setEnabled(false);
            this.saveCarton();
            this.selectedRow = (SOShipCartonRowData) this.tableModel.getSelectedRowData();
            if (this.selectedRow == null) {
                return;
            }
            this.enableCartonDetails(true);
            final PackageWeight packageWeight = this.selectedRow.getData().getPackageWeight();
            this.txtWeight.setText(packageWeight.getWeight());
            this.cboWeightUom.setSelectedItem(packageWeight.getUnitOfMeasurement().getCode());
            final Dimensions dimensions = this.selectedRow.getData().getDimensions();
            this.txtLength.setText(dimensions.getLength());
            this.txtWidth.setText(dimensions.getWidth());
            this.txtHeight.setText(dimensions.getHeight());
            this.cboSizeUOM.setSelectedItem(dimensions.getUnitOfMeasurement().getCode());
            this.txtWeight.requestFocus();
        }
    }

    private void saveCarton() {
        if (this.selectedRow == null) {
            return;
        }
        this.selectedRow.getData().getDimensions().setLength(Double.toString(this.txtLength.getQuantity().doubleValue()));
        this.selectedRow.getData().getDimensions().setWidth(Double.toString(this.txtWidth.getQuantity().doubleValue()));
        this.selectedRow.getData().getDimensions().setHeight(Double.toString(this.txtHeight.getQuantity().doubleValue()));
        final UnitOfMeasurement sizeUom = new UnitOfMeasurement(this.cboSizeUOM.getSelectedItem().toString());
        this.selectedRow.getData().getDimensions().setUnitOfMeasurement(sizeUom);
        final UnitOfMeasurement weightUom = new UnitOfMeasurement(this.cboWeightUom.getSelectedItem().toString());
        this.selectedRow.getData().getPackageWeight().setUnitOfMeasurement(weightUom);
        this.selectedRow.getData().getPackageWeight().setWeight(Double.toString(this.txtWeight.getQuantity().doubleValue()));
        this.recalculateTable();
    }

    private void btnRemoveCartonActionPerformed() {
        if (this.tableModel.getRowCount() > 1) {
            List<SOShipCartonRowData> selected = this.tableModel.getSelectedRowsData();
            if (selected.size() == this.packageList.size()) {
                UtilGui.showMessageDialog("Must have at least one" + this.cartonName, "Remove error", 1);
            } else {
                Set<Integer> selectedIds = (Set)selected.stream().map(SOShipCartonRowData::getID).collect(Collectors.toSet());
                this.packageList.removeIf((pack) -> {
                    return selectedIds.contains(pack.getId());
                });
                this.selectedRow = null;
                this.recalculateTable();
                this.clearData();
                this.btnRemoveCarton.setEnabled(this.tableModel.getRowCount() > 1);
            }
        }
    }

    private void btnCombineCartonsActionPerformed() {
        if (this.tableModel.getSelectedRowsData().size() < 2){
            UtilGui.showMessageDialog("Must have at least two" + this.cartonName + " selected.", "Combine error", 1);
        }
        else{
            //get the to total cubes of the cartons and weights and combine to new carton, remove originals
            double newWeight = new Double(0);
            double totalCubes = new Double(0);

            for (SOShipCartonRowData row : this.tableModel.getSelectedRowsData()) {
                newWeight = newWeight + Double.parseDouble(row.getData().getPackageWeight().getWeight());

                double packageCubes = Double.parseDouble(row.getData().getDimensions().getLength()) *
                        Double.parseDouble(row.getData().getDimensions().getHeight()) *
                        Double.parseDouble(row.getData().getDimensions().getWidth());

                totalCubes = totalCubes + packageCubes;

            }

            this.btnRemoveCartonActionPerformed(); //remove selected

            DecimalFormat format = new DecimalFormat("#.##");
            Dimensions newDims = new Dimensions();
            newDims.setWidth(format.format(Math.cbrt(totalCubes)));
            newDims.setHeight(format.format(Math.cbrt(totalCubes)));
            newDims.setLength(format.format(Math.cbrt(totalCubes)));

            this.addPackage(newWeight, newDims );

        }
    }


    private void clearData() {
        this.txtWeight.setText(Double.toString(0.0));
        this.txtLength.setText(Double.toString(0.0));
        this.txtWidth.setText(Double.toString(0.0));
        this.txtHeight.setText(Double.toString(0.0));
        this.setDefaultCboSelections();
        this.enableCartonDetails(false);
    }

    private void btnGetRatesActionPerformed() {
        this.saveCarton();
        this.getRates();
    }

    void addShippingLineToSo(final CarrierRate carrierRate) {
        this.shipRatesButton.addShippingLineToSo(carrierRate);
        if (this.repository.getDatabaseVersion() < 100) {
            UtilGui.showMessageDialog("Please update the carrier and carrier service for this SO in the details tab.", "Manually Update Order Shipping", 1);
        }
    }

    private void initComponents() {
        final JPanel pnlContents = new JPanel();
        this.pnlCartons = new IconTitleBorderPanel();
        final JLabel lblOrderTotalWeight = new JLabel();
        this.txtOrderTotalWeight = new JLabel();
        final JLabel lblRemainingWeight = new JLabel();
        this.txtRemainingWeight = new JLabel();
        final JButton btnAddCarton = new JButton();
        final JScrollPane scrollPane1 = new JScrollPane();
        this.tblCartons = new FBTable();
        this.btnRemoveCarton = new JButton();

        this.btnCombineCartons = new JButton();

        final JPanel pnlDetails = new JPanel();
        this.pnlCartonDetails = new IconTitleBorderPanel();
        final JPanel panel4 = new JPanel();
        this.lblWeight = new JLabel();
        this.txtWeight = new FBTextFieldQuantity();
        this.cboWeightUom = new JComboBox();
        this.lblLength = new JLabel();
        this.txtLength = new FBTextFieldQuantity();
        this.lblWidth = new JLabel();
        this.txtWidth = new FBTextFieldQuantity();
        this.lblHeight = new JLabel();
        this.txtHeight = new FBTextFieldQuantity();
        this.cboSizeUOM = new JComboBox();
        this.label1 = new JLabel();
        this.btnGetRates = new JButton();
        this.tbPnlRates = new IconTitleBorderPanel();
        this.setTitle("Shipping Rates");
        this.setName("this");
        final Container contentPane = this.getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] { 1010, 0 };
        ((GridBagLayout)contentPane.getLayout()).rowHeights = new int[] { 680, 0 };
        ((GridBagLayout)contentPane.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)contentPane.getLayout()).rowWeights = new double[] { 1.0, 1.0E-4 };
        pnlContents.setMaximumSize(new Dimension(975, 1009));
        pnlContents.setName("pnlContents");
        pnlContents.setLayout(new GridBagLayout());
        ((GridBagLayout)pnlContents.getLayout()).columnWidths = new int[] { 495, 0, 0 };
        ((GridBagLayout)pnlContents.getLayout()).rowHeights = new int[] { 241, 0, 0 };
        ((GridBagLayout)pnlContents.getLayout()).columnWeights = new double[] { 0.0, 0.0, 1.0E-4 };
        ((GridBagLayout)pnlContents.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.pnlCartons.setTitle("Estimated {0}(s)");
        this.pnlCartons.setIcon((Icon)new ImageIcon(this.getClass().getResource("/icon24/textanddocuments/documents/documents.png")));
        this.pnlCartons.setPreferredSize(new Dimension(288, 260));
        this.pnlCartons.setName("pnlCartons");
        this.pnlCartons.setLayout((LayoutManager)new GridBagLayout());
        ((GridBagLayout)this.pnlCartons.getLayout()).columnWidths = new int[] { 38, 0, 0, 0, 0, 0 };
        ((GridBagLayout)this.pnlCartons.getLayout()).rowHeights = new int[] { 0, 0, 0, 108, 0 };
        ((GridBagLayout)this.pnlCartons.getLayout()).columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlCartons.getLayout()).rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 1.0E-4 };
        lblOrderTotalWeight.setText("Order Weight:");
        lblOrderTotalWeight.setName("lblOrderTotalWeight");
        this.pnlCartons.add((Component)lblOrderTotalWeight, (Object)new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.txtOrderTotalWeight.setText("0");
        this.txtOrderTotalWeight.setName("txtOrderTotalWeight");
        this.pnlCartons.add((Component)this.txtOrderTotalWeight, (Object)new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 5, 5, 5), 0, 0));
        lblRemainingWeight.setText("Remaining Weight:");
        lblRemainingWeight.setName("lblRemainingWeight");
        this.pnlCartons.add((Component)lblRemainingWeight, (Object)new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 10, 5, 5), 0, 0));
        this.txtRemainingWeight.setText("0");
        this.txtRemainingWeight.setName("txtRemainingWeight");
        this.pnlCartons.add((Component)this.txtRemainingWeight, (Object)new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 5, 5, 0), 0, 0));
        btnAddCarton.setToolTipText("Add {0}");
        btnAddCarton.setIcon(new ImageIcon(this.getClass().getResource("/icon16/toolbar/others/add.png")));
        btnAddCarton.setMargin(new Insets(0, 0, 0, 0));
        btnAddCarton.setName("btnAddCarton");
        btnAddCarton.addActionListener(e -> this.btnAddCartonActionPerformed());
        this.pnlCartons.add((Component)btnAddCarton, (Object)new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        scrollPane1.setName("scrollPane1");
        this.tblCartons.setPreferredScrollableViewportSize(new Dimension(250, 400));
        this.tblCartons.setName("tblCartons");
        this.tblCartons.addMouseListener((MouseListener)new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                ShipRatesDialog.this.tblCartonsMouseReleased();
            }
        });
        scrollPane1.setViewportView((Component)this.tblCartons);
        this.pnlCartons.add((Component)scrollPane1, (Object)new GridBagConstraints(1, 1, 4, 3, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));

        this.btnRemoveCarton.setToolTipText("Remove {0}(s)");
        this.btnRemoveCarton.setIcon(new ImageIcon(this.getClass().getResource("/icon16/toolbar/others/delete.png")));
        this.btnRemoveCarton.setMargin(new Insets(0, 0, 0, 0));
        this.btnRemoveCarton.setEnabled(false);
        this.btnRemoveCarton.setName("btnRemoveCarton");
        this.btnRemoveCarton.addActionListener(e -> this.btnRemoveCartonActionPerformed());
        this.pnlCartons.add((Component)this.btnRemoveCarton, (Object)new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));


        this.btnCombineCartons.setToolTipText("Combine the selected cartons");
        this.btnCombineCartons.setIcon(new ImageIcon(this.getClass().getResource("/icon24/toolbar/edit/copy.png")));
        this.btnCombineCartons.setMargin(new Insets(0, 0, 0, 0));
        this.btnCombineCartons.setEnabled(false);
        this.btnCombineCartons.setName("btnCombineCartons");
        this.btnCombineCartons.addActionListener(e -> this.btnCombineCartonsActionPerformed());
        this.pnlCartons.add((Component)this.btnCombineCartons, (Object)new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));

        pnlContents.add((Component)this.pnlCartons, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));


        pnlDetails.setName("pnlDetails");
        pnlDetails.setLayout(new GridBagLayout());
        ((GridBagLayout)pnlDetails.getLayout()).columnWidths = new int[] { 480, 0 };
        ((GridBagLayout)pnlDetails.getLayout()).rowHeights = new int[] { 196, 25, 40, 0 };
        ((GridBagLayout)pnlDetails.getLayout()).columnWeights = new double[] { 0.0, 1.0E-4 };
        ((GridBagLayout)pnlDetails.getLayout()).rowWeights = new double[] { 0.0, 1.0, 0.0, 1.0E-4 };
        this.pnlCartonDetails.setType(IconTitleBorderPanel.IconConst.ShippingCarton);
        this.pnlCartonDetails.setTitle("{0} Details");
        this.pnlCartonDetails.setName("pnlCartonDetails");
        this.pnlCartonDetails.setLayout((LayoutManager)new GridBagLayout());
        ((GridBagLayout)this.pnlCartonDetails.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlCartonDetails.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlCartonDetails.getLayout()).columnWeights = new double[] { 0.0, 1.0E-4 };
        ((GridBagLayout)this.pnlCartonDetails.getLayout()).rowWeights = new double[] { 0.0, 1.0E-4 };
        panel4.setName("panel4");
        panel4.setLayout(new GridBagLayout());
        ((GridBagLayout)panel4.getLayout()).columnWidths = new int[] { 55, 65, 55, 65, 55, 65, 0, 0 };
        ((GridBagLayout)panel4.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)panel4.getLayout()).columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4 };
        ((GridBagLayout)panel4.getLayout()).rowWeights = new double[] { 0.0, 0.0, 1.0E-4 };
        this.lblWeight.setText("Weight*:");
        this.lblWeight.setName("lblWeight");
        panel4.add(this.lblWeight, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.txtWeight.setAllowPositiveOnly(true);
        this.txtWeight.setName("txtWeight");
        panel4.add((Component)this.txtWeight, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.cboWeightUom.setName("cboWeightUom");
        panel4.add(this.cboWeightUom, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.lblLength.setText("Length:");
        this.lblLength.setName("lblLength");
        panel4.add(this.lblLength, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.txtLength.setAllowPositiveOnly(true);
        this.txtLength.setName("txtLength");
        panel4.add((Component)this.txtLength, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.lblWidth.setText("Width:");
        this.lblWidth.setName("lblWidth");
        panel4.add(this.lblWidth, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.txtWidth.setAllowPositiveOnly(true);
        this.txtWidth.setName("txtWidth");
        panel4.add((Component)this.txtWidth, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.lblHeight.setText("Height:");
        this.lblHeight.setName("lblHeight");
        panel4.add(this.lblHeight, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.txtHeight.setAllowPositiveOnly(true);
        this.txtHeight.setName("txtHeight");
        panel4.add((Component)this.txtHeight, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.cboSizeUOM.setName("cboSizeUOM");
        panel4.add(this.cboSizeUOM, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.pnlCartonDetails.add((Component)panel4, (Object)new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(5, 5, 5, 5), 0, 0));
        pnlDetails.add((Component)this.pnlCartonDetails, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 0), 0, 0));
        this.label1.setText("*Enter dimensions to get the most accurate rate quote");
        this.label1.setFont(new Font("Segoe UI", 0, 10));
        this.label1.setForeground(new Color(102, 102, 102));
        this.label1.setName("label1");
        pnlDetails.add(this.label1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 15, 2, new Insets(0, 5, 5, 0), 0, 0));
        this.btnGetRates.setText("Get Rates");
        this.btnGetRates.setName("btnGetRates");
        this.btnGetRates.addActionListener(e -> this.btnGetRatesActionPerformed());
        pnlDetails.add(this.btnGetRates, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        pnlContents.add(pnlDetails, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 0), 0, 0));
        this.tbPnlRates.setType(IconTitleBorderPanel.IconConst.Accounting);
        this.tbPnlRates.setTitle("Rates");
        this.tbPnlRates.setMaximumSize(new Dimension(550, 200));
        this.tbPnlRates.setMinimumSize(new Dimension(550, 200));
        this.tbPnlRates.setPreferredSize(new Dimension(550, 200));
        this.tbPnlRates.setName("tbPnlRates");
        this.tbPnlRates.setLayout((LayoutManager)new CardLayout());
        pnlContents.add((Component)this.tbPnlRates, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(pnlContents, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(10, 10, 10, 10), 0, 0));
        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }



    static {
        LOGGER = LoggerFactory.getLogger((Class)ShipRatesDialog.class);
    }
}
