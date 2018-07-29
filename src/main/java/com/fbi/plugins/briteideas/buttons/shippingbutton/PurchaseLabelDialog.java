// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.shippingbutton;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import javax.swing.border.Border;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Dimension;

import com.fbi.plugins.briteideas.DisplayLabelDialog;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.plugins.briteideas.util.property.Property;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import com.shippo.model.TransactionCollection;
import com.fbi.commerce.shipping.util.NumberUtil;
import com.shippo.model.Transaction;
import com.fbi.fbdata.accounts.AddressInformation;
import com.fbi.sdk.constants.accounts.AddressInformationTypeConst;
import com.shippo.exception.ShippoException;
import com.shippo.exception.APIConnectionException;
import com.shippo.exception.APIException;
import com.shippo.exception.InvalidRequestException;
import com.shippo.exception.AuthenticationException;
import com.evnt.util.Util;
import com.shippo.model.Address;
import com.fbi.sdk.constants.CountryConst;
import com.fbi.commerce.shipping.util.shippo.ShippoUtil;
import com.fbi.sdk.constants.UOMConst;
import com.evnt.eve.modules.AccountingModule;
import java.math.BigDecimal;
import com.evnt.util.Quantity;
import com.fbi.fbo.shipping.Carton;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;
import java.util.Iterator;
import com.fbi.gui.util.UtilGui;
import com.evnt.util.Money;
import com.fbi.commerce.shipping.type.ShipServiceEnum;
import com.fbi.commerce.shipping.util.shippo.ShippoErrorMessageParser;
import com.shippo.Shippo;
import com.fbi.fbo.shipping.ShippingItem;
import java.util.Map;
import java.util.List;
import com.fbi.commerce.shipping.util.shippo.CommerceShippoException;
import java.util.HashMap;
import java.awt.Image;
import com.fbi.gui.util.GenericBouncyDialog;
import javax.swing.ImageIcon;
import com.fbi.fbo.impl.dataexport.QueryRow;
import java.awt.Frame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.fbi.commerce.shipping.util.ShippoInfo;
import com.shippo.model.Rate;
import com.fbi.fbo.shipping.Shipment;
import javax.swing.JDialog;

public final class PurchaseLabelDialog extends JDialog {
    private static final String PURCHASE = "PURCHASE";
    private static final String OBJECT_PURPOSE = "object_purpose";
    private static final String EMAIL = "email";
    private Shipment fbShipment;
    private Rate selectedRate;
    private ShippoInfo shippoInfo;
    private ShippingButton shippingButton;
    private String cartonName;
    private Repository repository;
    private String fbCarrierService;
    private String mainPhoneNumber;
    private PropertyGetter propertyGetter;
    private boolean canValidateAddress;
    private JPanel pnlContent;
    private JButton btnPurchaseLabel;
    private JPanel pnlRateInfo;
    private JTextPane txtRate;
    private JLabel lblUSPSBillMessage;

    public PurchaseLabelDialog(Frame owner, Shipment shipment, QueryRow shipFromAddress, ShippoInfo shippoInfo, ShippingButton shippingButton, String cartonName, Repository repository, String mainPhoneNumber, PropertyGetter plugin) {
        super(owner);
        this.fbShipment = shipment;
        this.shippoInfo = shippoInfo;
        this.shippingButton = shippingButton;
        this.cartonName = cartonName;
        this.repository = repository;
        this.mainPhoneNumber = mainPhoneNumber;
        this.propertyGetter = plugin;
        this.setIconImage((new ImageIcon(this.getClass().getResource("/images/24x24-fb-commerce-circular-icon-orange.png"))).getImage());
        this.initComponents();
        this.setupCarrierService();
        new GenericBouncyDialog("Retrieving Rates", "Processing...", (Image)null, () -> {
            if (!this.fetchRate(shipFromAddress)) {
                this.dispose();
            } else {
                this.setModal(true);
                this.setVisible(true);
            }
        });
    }

    private boolean fetchRate(QueryRow shipFromAddress) {
        this.canValidateAddress = true;
        Map<String, Object> addressTo = this.createShipToAddressMap();
        Map<String, Object> addressFrom = this.createShipFromAddress(shipFromAddress);
        if (!this.canValidateAddress && this.shippoInfo.isShowNoValidateWarning()) {
            new NoValidationDialog(this.shippingButton, this.shippoInfo);
        }

        CommerceShippoException exception = new CommerceShippoException(new HashMap());
        String addressToId = this.validateAddress(addressTo, exception, CommerceShippoException.ErrorType.ADDRESS_TO);
        String addressFromId = this.validateAddress(addressFrom, exception, CommerceShippoException.ErrorType.ADDRESS_FROM);
        List<Map<String, Object>> parcelArrayMap = this.createParcelMap();
        Map<String, Object> extraMap = new HashMap();
        extraMap.put("bypass_address_validation", true);
        Map<String, Object> shipmentMap = new HashMap();
        shipmentMap.put("address_to", addressToId);
        shipmentMap.put("address_from", addressFromId);
        shipmentMap.put("parcel", parcelArrayMap.toArray());
        shipmentMap.put("extra", extraMap);
        shipmentMap.put("object_purpose", "PURCHASE");
        shipmentMap.put("async", false);
        this.determineReferences(this.fbShipment.getOrderNumber(), this.fbShipment.getShipItemList(), shipmentMap);

        com.shippo.model.Shipment shipment;
        try {
            Shippo.setApiVersion("2016-10-25");
            shipment = com.shippo.model.Shipment.create(shipmentMap, this.shippoInfo.getShippoApi());
        } catch (Exception var13) {
            CommerceShippoException ex = (new ShippoErrorMessageParser()).parseException(var13);
            this.mapException(exception, ex);
            this.displayShippoError(exception);
            return false;
        }

        return this.extractRateFromShipment(exception, shipment);
    }

    private boolean extractRateFromShipment(CommerceShippoException exception, com.shippo.model.Shipment shipment) {
        if ((shipment == null || shipment.getRates().isEmpty()) && !exception.getErrorMap().isEmpty()) {
            this.displayShippoError(exception);
            return false;
        } else {
            if (shipment != null) {
                Iterator var3 = shipment.getRates().iterator();

                while(var3.hasNext()) {
                    Rate rate = (Rate)var3.next();
                    String carrier = ShipServiceEnum.getShippoCarrierByFbCarrier(this.fbShipment.getCarrier());
                    if (carrier.equals(rate.getProvider().toString()) && ShipServiceEnum.getShippoTokenByFbCarrierAndService(this.fbCarrierService, carrier).equals(rate.getServicelevel().toString())) { //COME BACK
                        this.selectedRate = rate;
                        this.txtRate.setText(this.txtRate.getText().replace("ShipRate", (new Money(rate.getAmount().toString())).toFullString()));
                        return true;
                    }
                }

                if (!exception.getErrorMap().isEmpty()) {
                    this.displayShippoError(exception);
                    return false;
                }
            }

            UtilGui.showMessageDialog("No rates were found for this carrier and service.", "Could Not Retrieve Rates", 1);
            return false;
        }
    }

    private void displayShippoError(CommerceShippoException exception) {
        String errors = (String)exception.getErrorMap().values().stream().flatMap(Collection::stream).collect(Collectors.joining("\n"));
        UtilGui.showMessageDialog(errors, "Could Not Retrieve Rates", 1);
    }

    private List<Map<String, Object>> createParcelMap() {
        List<Map<String, Object>> parcelArrayMap = new ArrayList();

        HashMap parcelMap;
        for(Iterator var2 = this.fbShipment.getCartons().iterator(); var2.hasNext(); parcelArrayMap.add(parcelMap)) {
            Carton carton = (Carton)var2.next();
            parcelMap = new HashMap();
            parcelMap.put("length", carton.getLen().equals(Quantity.quantityZero) ? BigDecimal.TEN : carton.getLen().bigDecimalValue());
            parcelMap.put("width", carton.getWidth().equals(Quantity.quantityZero) ? BigDecimal.TEN : carton.getWidth().bigDecimalValue());
            parcelMap.put("height", carton.getHeight().equals(Quantity.quantityZero) ? BigDecimal.TEN : carton.getHeight().bigDecimalValue());
            if (carton.getSizeUOM() != null && !carton.getSizeUOM().isEmpty()) {
                parcelMap.put("distance_unit", carton.getSizeUOM().toLowerCase());
            } else {
                parcelMap.put("distance_unit", AccountingModule.isUs() ? UOMConst.INCH.getCode().toLowerCase() : UOMConst.CENTIMETER.getCode().toLowerCase());
            }

            Map<String, Object> extraMap = new HashMap();
            if (((Boolean) Property.PURCHASE_INSURANCE.get(this.propertyGetter)).booleanValue() && carton.getInsuredValue().doubleValue() > 0.0D) {
                Map<String, Object> insuranceMap = new HashMap();
                insuranceMap.put("amount", carton.getInsuredValue().bigDecimalValue().toString());
                insuranceMap.put("currency", "USD");
                extraMap.put("insurance", insuranceMap);
            }

            parcelMap.put("extra", extraMap);
            Quantity weight;
            if (UOMConst.KILOGRAM.getCode().equalsIgnoreCase(carton.getFreightWeightUOM())) {
                weight = carton.getFreightWeight().multiply(35.27D);
            } else {
                weight = carton.getFreightWeight().multiply(16.0D);
            }

            parcelMap.put("weight", weight.bigDecimalValue().setScale(2, 4));
            parcelMap.put("mass_unit", "oz");
            if (this.fbCarrierService.equalsIgnoreCase(ShipServiceEnum.FbShipServiceEnum.USPS_PRIORITY_FLAT_RATE_ENVELOPE.getFbService())) {
                parcelMap.put("template", "USPS_FlatRateEnvelope");
            }
        }

        return parcelArrayMap;
    }

    private Map<String, Object> createShipFromAddress(QueryRow shipFromAddress) {
        Map<String, Object> addressFrom = new HashMap();
        addressFrom.put("object_purpose", "PURCHASE");
        String name = shipFromAddress.getString("name");
        if (name == null || name.length() == 0) {
            name = this.repository.getCompanyName();
        }

        addressFrom.put("name", name);
        String street = ShippoUtil.StreetFirstLineCheck(shipFromAddress.getString("street"));
        addressFrom.put("street1", street);
        addressFrom.put("city", shipFromAddress.getString("city"));
        addressFrom.put("state", shipFromAddress.getString("stateCode"));
        addressFrom.put("zip", shipFromAddress.getString("zip"));
        addressFrom.put("country", shipFromAddress.getString("countryName"));
        addressFrom.put("phone", this.mainPhoneNumber);
        String email = shipFromAddress.getString("email");
        if (email != null && !email.isEmpty()) {
            addressFrom.put("email", shipFromAddress.getString("email"));
        } else {
            addressFrom.put("email", "support@fishbowlinventory.com");
        }

        this.addValidate(addressFrom, shipFromAddress.getString("countryName"));
        return addressFrom;
    }

    private void addValidate(Map<String, Object> addressMap, String countryName) {
        if (countryName != null && countryName.equals(CountryConst.US.getName())) {
            addressMap.put("validate", "true");
        } else {
            addressMap.put("validate", "false");
            this.canValidateAddress = false;
        }

    }

    private String validateAddress(Map<String, Object> address, CommerceShippoException exception, CommerceShippoException.ErrorType errorType) {
        try {
            Address validateShipToAddress = Address.create(address, this.shippoInfo.getShippoApi());
            if (!validateShipToAddress.getValidationResults().getIsValid()) {
                Map<String, String> shippoErrorMap = (Map)((List)validateShipToAddress.getMessages()).get(0);
                String message = errorType.getValue() + " error - ";
                if (!Util.isEmpty((String)shippoErrorMap.get("code"))) {
                    message = message + " " + (String)shippoErrorMap.get("code");
                }

                if (!Util.isEmpty((String)shippoErrorMap.get("text"))) {
                    message = message + " " + (String)shippoErrorMap.get("text");
                }

                this.mapException(exception, new CommerceShippoException(errorType, message));
            }

            return validateShipToAddress.getObjectId();
        } catch (InvalidRequestException | APIException | APIConnectionException | AuthenticationException var7) {
            CommerceShippoException ex = (new ShippoErrorMessageParser()).parseException(var7, errorType);
            this.mapException(exception, ex);
            return null;
        }
    }

    private Map<String, Object> createShipToAddressMap() {
        com.fbi.fbdata.accounts.Address shipToAddress = this.fbShipment.getShipTo();
        Map<String, Object> addressTo = new HashMap();
        addressTo.put("object_purpose", "PURCHASE");
        String name = shipToAddress.getAttn();
        if (name == null || name.length() == 0) {
            name = this.repository.getCustomerNameFromShipment(this.fbShipment.getSoId());
        }

        addressTo.put("name", name);
        String street = ShippoUtil.StreetFirstLineCheck(shipToAddress.getStreet());
        addressTo.put("street1", street);
        addressTo.put("city", shipToAddress.getCity());
        if (shipToAddress.getState() != null) {
            addressTo.put("state", shipToAddress.getState().getCode());
        }

        addressTo.put("zip", shipToAddress.getZip());
        if (shipToAddress.getCountry() != null) {
            addressTo.put("country", shipToAddress.getCountry().getName());
        }

        String soPhoneNumber = this.repository.getPhoneFromShipment(this.fbShipment);
        addressTo.put("phone", soPhoneNumber);
        String shipToEmail = (String)Stream.concat(shipToAddress.getAddressInformationList().stream().filter((x) -> {
            return AddressInformationTypeConst.EMAIL.equals(x.getType()) && x.isDefaultFlag();
        }), shipToAddress.getAddressInformationList().stream().filter((x) -> {
            return AddressInformationTypeConst.EMAIL.equals(x.getType());
        })).map(AddressInformation::getData).findFirst().orElse("default@fbcommerce.com");
        addressTo.put("email", shipToEmail);
        this.addValidate(addressTo, shipToAddress.getCountry() == null ? null : shipToAddress.getCountry().getName());
        return addressTo;
    }

    private void mapException(CommerceShippoException exception, CommerceShippoException ex) {
        Iterator var3;
        String error;
        if (ex.getErrorMap().get(CommerceShippoException.ErrorType.GENERAL) != null) {
            var3 = ((List)ex.getErrorMap().get(CommerceShippoException.ErrorType.GENERAL)).iterator();

            while(var3.hasNext()) {
                error = (String)var3.next();
                exception.add(CommerceShippoException.ErrorType.GENERAL, error);
            }
        }

        if (ex.getErrorMap().get(CommerceShippoException.ErrorType.ADDRESS_FROM) != null) {
            var3 = ((List)ex.getErrorMap().get(CommerceShippoException.ErrorType.ADDRESS_FROM)).iterator();

            while(var3.hasNext()) {
                error = (String)var3.next();
                exception.add(CommerceShippoException.ErrorType.ADDRESS_FROM, error);
            }
        }

        if (ex.getErrorMap().get(CommerceShippoException.ErrorType.ADDRESS_TO) != null) {
            var3 = ((List)ex.getErrorMap().get(CommerceShippoException.ErrorType.ADDRESS_TO)).iterator();

            while(var3.hasNext()) {
                error = (String)var3.next();
                exception.add(CommerceShippoException.ErrorType.ADDRESS_TO, error);
            }
        }

        if (ex.getErrorMap().get(CommerceShippoException.ErrorType.PARCEL) != null) {
            var3 = ((List)ex.getErrorMap().get(CommerceShippoException.ErrorType.PARCEL)).iterator();

            while(var3.hasNext()) {
                error = (String)var3.next();
                exception.add(CommerceShippoException.ErrorType.PARCEL, error);
            }
        }

    }

    private void setupCarrierService() {
        String carrierService = "";
        if (!Util.isEmpty(this.fbShipment.getCarrier())) {
            carrierService = carrierService + this.fbShipment.getCarrier();
            this.lblUSPSBillMessage.setVisible("USPS".equalsIgnoreCase(this.fbShipment.getCarrier()));
        }

        if (this.repository.getDatabaseVersion() < 100) {
            this.fbCarrierService = this.repository.getCarrierServiceById(this.fbShipment.getCarrierServiceId());
        } else {
            this.fbCarrierService = this.fbShipment.getCarrierService();
        }

        carrierService = carrierService + " " + this.fbCarrierService;
        this.txtRate.setText(this.txtRate.getText().replace("CarrierService", carrierService));
    }

    private void btnPurchaseLabelActionPerformed() {
        new GenericBouncyDialog("Creating Labels", "Processing...", (Image)null, () -> {
            String labelFormat = this.shippoInfo.getLabelFormat();
            if (Util.isEmpty(labelFormat)) {
                labelFormat = "PDF_4X6";
            }

            Map<String, Object> transactionMap = new HashMap();
            transactionMap.put("rate", this.selectedRate.getObjectId());
            transactionMap.put("label_file_type", labelFormat);
            transactionMap.put("async", false);
            transactionMap.put("partner_code", "FISHBOWL");
            transactionMap.put("results", 2147483647);

            TransactionCollection transactionCollection;
            try {
                Shippo.setApiVersion("2016-10-25");
                Transaction.create(transactionMap, this.shippoInfo.getShippoApi());
                transactionCollection = Transaction.all(transactionMap, this.shippoInfo.getShippoApi());
            } catch (InvalidRequestException | APIConnectionException | APIException | AuthenticationException var8) {
                UtilGui.showMessageDialog(var8.getMessage(), "Unable to Purchase Label", 1);
                return;
            }

            if (transactionCollection.getData().size() > 0) {
                this.shippingButton.saveShippingInfo(transactionCollection, this.selectedRate.getObjectId(), NumberUtil.getBigDecimal(this.selectedRate.getAmount()));
                Iterator var4 = transactionCollection.getData().iterator();

                Transaction transaction;
                do {
                    if (!var4.hasNext()) {
                        this.hideDialog();
                        new DisplayLabelDialog(this, transactionCollection, this.cartonName);
                        return;
                    }

                    transaction = (Transaction)var4.next();
                } while(!"INVALID".equals(transaction.getStatus()) && (!"VALID".equals(transaction.getStatus()) || !"ERROR".equals(transaction.getStatus())));

                Map<String, String> shippoErrorMap = (Map)((List)transaction.getMessages()).get(0);
                String message = "transaction error - " + (String)shippoErrorMap.get("text");
                UtilGui.showMessageDialog(message, "Unable to Purchase Label", 1);
                this.hideDialog();
            }
        });
    }

    protected void determineReferences(String orderNumber, List<ShippingItem> orderItems, Map<String, Object> shipmentMap) {
        String labelText = this.shippoInfo.getLabelText();
        if (!Util.isEmpty(labelText)) {
            byte var6 = -1;
            switch(labelText.hashCode()) {
                case 2402104:
                    if (labelText.equals("NONE")) {
                        var6 = 0;
                    }
                    break;
                case 75468590:
                    if (labelText.equals("ORDER")) {
                        var6 = 1;
                    }
            }

            switch(var6) {
                case 0:
                    return;
                case 1:
                    shipmentMap.put("reference_1", orderNumber);
                    return;
                default:
                    if (!Util.isEmpty(orderItems)) {
                        StringBuilder ref1 = new StringBuilder();
                        StringBuilder ref2 = new StringBuilder();
                        boolean ref1IsFull = false;
                        boolean skipped = false;
                        boolean atLeastOnePrinted = false;
                        int i = 0;

                        for(; i < orderItems.size(); ++i) {
                            boolean anotherItem = i < orderItems.size() - 1;
                            ShippingItem orderItem = (ShippingItem)orderItems.get(i);
                            String sku = orderItem.getSKU();
                            if (Util.isEmpty(sku)) {
                                skipped = true;
                            } else {
                                String itemInfo = sku + " x" + orderItem.getQuantityShipped();
                                if (atLeastOnePrinted) {
                                    itemInfo = ", " + itemInfo;
                                }

                                if (!ref1IsFull && ref1.length() + itemInfo.length() > 50) {
                                    ref1IsFull = true;
                                    atLeastOnePrinted = false;
                                    itemInfo = sku + " x" + orderItem.getQuantityShipped();
                                }

                                if (!ref1IsFull) {
                                    ref1.append(itemInfo);
                                    atLeastOnePrinted = true;
                                } else {
                                    int length = 50;
                                    if (skipped || anotherItem) {
                                        length = 45;
                                    }

                                    if (ref2.length() + itemInfo.length() > length) {
                                        skipped = true;
                                    } else {
                                        ref2.append(itemInfo);
                                        atLeastOnePrinted = true;
                                    }
                                }
                            }
                        }

                        String ellipsis = "...";
                        if (atLeastOnePrinted) {
                            ellipsis = ", " + ellipsis;
                        }

                        if (skipped) {
                            ref2.append(ellipsis);
                        }

                        shipmentMap.put("reference_1", ref1.toString());
                        shipmentMap.put("reference_2", ref2.toString());
                    }
            }
        }
    }

    private void hideDialog() {
        this.setVisible(false);
    }

    private void initComponents() {
        this.pnlContent = new JPanel();
        this.btnPurchaseLabel = new JButton();
        this.pnlRateInfo = new JPanel();
        this.txtRate = new JTextPane();
        this.lblUSPSBillMessage = new JLabel();
        this.setTitle("Shipping Confirmation");
        this.setResizable(false);
        this.setMinimumSize(new Dimension(360, 210));
        this.setName("this");
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[]{0, 0};
        ((GridBagLayout)contentPane.getLayout()).rowHeights = new int[]{0, 0};
        ((GridBagLayout)contentPane.getLayout()).columnWeights = new double[]{1.0D, 1.0E-4D};
        ((GridBagLayout)contentPane.getLayout()).rowWeights = new double[]{1.0D, 1.0E-4D};
        this.pnlContent.setPreferredSize(new Dimension(218, 100));
        this.pnlContent.setName("pnlContent");
        this.pnlContent.setLayout(new BorderLayout());
        this.btnPurchaseLabel.setText("Purchase Label");
        this.btnPurchaseLabel.setMinimumSize(new Dimension(50, 40));
        this.btnPurchaseLabel.setMaximumSize(new Dimension(50, 40));
        this.btnPurchaseLabel.setPreferredSize(new Dimension(50, 40));
        this.btnPurchaseLabel.setName("btnPurchaseLabel");
        this.btnPurchaseLabel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PurchaseLabelDialog.this.btnPurchaseLabelActionPerformed();
            }
        });
        this.pnlContent.add(this.btnPurchaseLabel, "South");
        this.pnlRateInfo.setName("pnlRateInfo");
        this.pnlRateInfo.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlRateInfo.getLayout()).columnWidths = new int[]{114, 0};
        ((GridBagLayout)this.pnlRateInfo.getLayout()).rowHeights = new int[]{0, 0, 0};
        ((GridBagLayout)this.pnlRateInfo.getLayout()).columnWeights = new double[]{1.0D, 1.0E-4D};
        ((GridBagLayout)this.pnlRateInfo.getLayout()).rowWeights = new double[]{0.0D, 0.0D, 1.0E-4D};
        this.txtRate.setContentType("text/html");
        this.txtRate.setText("<html>\r   <body>\r     CarrierService: <span style=\"color: green;\"><b>ShipRate</b></span>   </body>\r </html>\r ");
        this.txtRate.setEditable(false);
        this.txtRate.setOpaque(false);
        this.txtRate.setBorder((Border)null);
        this.txtRate.setFont(new Font("Arial", 0, 12));
        this.txtRate.setName("txtRate");
        this.pnlRateInfo.add(this.txtRate, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, 10, 1, new Insets(0, 10, 5, 0), 0, 0));
        this.lblUSPSBillMessage.setText("*Postage will be billed to your Shippo account");
        this.lblUSPSBillMessage.setFont(new Font("Segoe UI", 0, 10));
        this.lblUSPSBillMessage.setName("lblUSPSBillMessage");
        this.pnlRateInfo.add(this.lblUSPSBillMessage, new GridBagConstraints(0, 1, 1, 1, 0.0D, 0.0D, 10, 1, new Insets(0, 15, 0, 0), 0, 0));
        this.pnlContent.add(this.pnlRateInfo, "Center");
        contentPane.add(this.pnlContent, new GridBagConstraints(0, 0, 1, 1, 0.0D, 0.0D, 10, 1, new Insets(20, 12, 20, 12), 0, 0));
        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }
}