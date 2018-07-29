package com.fbi.plugins.briteideas.buttons.customerbutton;


import com.evnt.client.common.EVEManager;
import com.evnt.client.common.EVEManagerUtil;
import com.evnt.common.ErrorHandler;
import com.evnt.eve.event.EVEvent;
import com.evnt.util.KeyConst;
import com.fbi.plugins.briteideas.repository.Repository;
import com.fbi.commerce.shipping.util.ShippingPluginProperty;
import com.fbi.commerce.shipping.util.fedex.FedExUtil;
import com.fbi.plugins.briteideas.util.property.Property;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import com.fbi.commerce.shipping.util.shippo.ShippoUtil;
import com.fbi.fbdata.accounts.Address;
import com.fbi.fbdata.accounts.State;
import com.fbi.fbdata.customer.CustomerFPO;
import com.fbi.fbo.CustomField;
import com.fbi.fbo.impl.dataexport.QueryRow;
import com.fbi.gui.util.UtilGui;
import com.fbi.plugins.FishbowlPluginButton;
import com.fbi.util.exception.ExceptionMainFree;
import com.fedex.ws.addressvalidation.v4.*;
//import com.shippo.exception.APIConnectionException;
//import com.shippo.exception.APIException;
//import com.shippo.exception.AuthenticationException;
//import com.shippo.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

public final class AddressValidateButton extends FishbowlPluginButton implements PropertyGetter, Repository.RunSql {
    private static final Logger LOGGER;

    private Repository repository;

    private int customerID;
    private CustomerFPO customerFPO;
    private ArrayList<Address> addresses;
    private EVEManager eveManager;
    private java.util.List<CustomField> customFields;
    private ShippingPluginProperty shippingPluginProperty;

    private Map<String, ArrayList<String>> messageList;
    private Map<String, String> validStatus;

    private AddressValidationRequest request;



    public AddressValidateButton(){

        this.repository = new Repository(this);

        this.setModuleName("Customer");
        this.setPluginName("ShipExtend");
        this.setIcon(new ImageIcon(this.getClass().getResource("/images/address.png")));
        this.setText("Validate Addresses");

        this.eveManager = EVEManagerUtil.getEveManager();

        this.addActionListener((event) -> {
            this.validateAddress();
        });
    }

    private void validateAddress() {
        this.customerFPO = new CustomerFPO();
        this.addresses = new ArrayList<Address>();
        //get the customer selected id
        //create get request for that customer
        //decode the address arrays
        //send each address to shippo and then update the address if necessary
        //on failure, show dialog of address names that have errors. Allow copy of the errors to clipboard
        this.customerID = this.getObjectId();

        if (this.customerID < 0){
            UtilGui.showMessageDialog("Please select a customer first.", "Unable to get customer", 1);
        }
        else   {

            this.shippingPluginProperty = this.getShippingProperties();

            try {
                loadData(this.customerID);

                if (this.addresses.size() > 0) {
                    this.messageList = new HashMap<String, ArrayList<String>>();
                    this.validStatus = new HashMap<String, String>();

                    LOGGER.error(this.customerFPO.getName());

                    //create FedEx request object here
                    this.setupRequest();
                    LOGGER.error("Request created");


                    //pass addresses to FedEx request
                    this.setFedExAddresses(addresses);


                    //AddressValidationServiceLocator service;
                    //AddressValidationPortType port;
                    //
                    //service = new AddressValidationServiceLocator();
                    //updateEndPoint(service);
                    //port = service.getAddressValidationServicePort();


                    AddressValidationServiceStub stub = new AddressValidationServiceStub();

                    AddressValidationRequestE requestE = new AddressValidationRequestE();
                    requestE.setAddressValidationRequest(request);
                    // This is the call to the web service
                    AddressValidationReplyE replyE = stub.addressValidation(requestE); //port.addressValidation(request);

                    AddressValidationReply reply = replyE.getAddressValidationReply();

                    System.out.println("Transactions successful");

                    if (isResponseOk(reply.getHighestSeverity())) {



                        if (shippingPluginProperty.isDebugMode()) {
                            AddressValidationResult[] avr = reply.getAddressResults();
                            for (int i = 0; i < avr.length; i++) {
                                System.out.println("Address Id - " + avr[i].getClientReferenceId());
                                com.fedex.ws.addressvalidation.v4.Address address = avr[i].getEffectiveAddress();
                                System.out.println("--- Effective Address--- ");
                                String[] streetlines = address.getStreetLines();
                                for (int j = 0; j < streetlines.length; j++) {
                                    System.out.println("  Street: " + streetlines[j]);
                                }
                                if (address.getCity() != null) System.out.println("    City: " + address.getCity());
                                if (address.getStateOrProvinceCode() != null)
                                    System.out.println("   ST/PR: " + address.getStateOrProvinceCode());
                                if (address.getPostalCode() != null)
                                    System.out.println("  Postal: " + address.getPostalCode());
                                if (address.getCountryCode() != null)
                                    System.out.println(" Country: " + address.getCountryCode());
                                System.out.println();
                                System.out.println("--- Address Attributes ---");
                                AddressAttribute[] attributes = avr[i].getAttributes();
                                for (int j = 0; j < attributes.length; j++) {
                                    System.out.println("  " + attributes[j].getName() + ": " + attributes[j].getValue());
                                }
                                System.out.println();
                                System.out.println();
                            }
                        }

                        //update the addresses with the returned values
                        for (Address address : this.addresses ) {
                            if (this.shippingPluginProperty.isDebugMode()) {
                                LOGGER.error("Name: " + address.getName());
                                LOGGER.error("Street: " + address.getStreet());
                                LOGGER.error("Full: " + address.toStringAddressFull());
                            }

//                            ArrayList<String> addressMessages = new ArrayList<String>();
//                            addressMessages.add(this.compareAndUpdateAddressFields(Arrays.stream(reply.getAddressResults())
//                                    .filter(x -> x.getClientReferenceId().equals(String.valueOf(address.getId())))
//                                    .findFirst()
//                                    .orElse(null), address));
//
//                            this.messageList.put(address.getName(), addressMessages);
                        }

                        AddressValidationResult[] avr = reply.getAddressResults();
                        for (int i = 0; i < avr.length; i++) {
                            boolean isResolved;

                            isResolved = Boolean.parseBoolean(
                                    Arrays.stream(avr[i].getAttributes())
                                    .filter(x -> x.getName().equals("Resolved"))
                                    .findFirst()
                                    .orElse(null).getValue()
                            );

                            if (isResolved){

                                int finalI = i;

                                this.compareAndUpdateAddressFields(avr[i],
                                        this.addresses.stream()
                                                .filter(x -> x.getId() == Integer.valueOf(avr[finalI].getClientReferenceId()))
                                                .findFirst()
                                                .orElse(null));


                            }
                            else {
                                int finalI1 = i;
                                this.messageList.put(this.addresses.stream()
                                        .filter(x -> x.getId() == Integer.valueOf(avr[finalI1].getClientReferenceId()))
                                        .findFirst()
                                        .orElse(null).getName(),

                                        new ArrayList<String>(){{ add("Unable to resolve Address");}});
                            }


                        }




                        printNotifications(reply.getNotifications());


                        UtilGui.showMessageDialog(this.getOutputMessage());


                        //if changes have been made to the addresses then save them back to the customer
                        final EVEvent event = this.getEveManager().createRequest("Customer", "updateCustomer");
                        event.getSource().setModule(this.getModuleName());
                        event.getSource().setHandler("Edit");
                        try {
                            event.addObject((Object) "customer", (Serializable) this.customerFPO);
                            //this.memoPanel.store(event);
                            //event.addList((Object)KeyConst.CUSTOM_FIELDS, (List)this.customFieldPanel.getData());
                            event.addObject((Object) KeyConst.ADDRESS, (Serializable) this.addresses);
                            //event.addObject((Object)"account groups", (Serializable)this.pnlGroups.getMemberItems());
                            //event.addObject((Object)"group list", (Serializable)this.pnlGroups.getAvailableItems());

                            this.handleSaveEvent(this.getEveManager().sendAndWait(event));
                            this.reloadObject();
                            //this.dbSearchCustomer.executeSearch();
                            //return true;
                        } catch (ExceptionMainFree e) {
                            LOGGER.error(e.getMessage(), (Throwable) e);
                            //return false;
                        }
                    }
                    else {
                        //error with request?
                        this.messageList.put("Request Error", this.getNotifications(reply.getNotifications()) );
                    }

                } else {
                    UtilGui.showMessageDialog("No addresses found for this customer", "Unable to get addresses", 1);
                }


            }
            catch (Exception ex){
                LOGGER.error(ex.getMessage(), ex);
            }


            //clean up
            this.customerFPO = null;
            this.addresses = null;
        }

    }

    private void setupRequest(){
        LOGGER.error("request started to be created");

        this.request = new AddressValidationRequest();
        LOGGER.error("request step2");
        request.setClientDetail(createClientDetail());
        LOGGER.error("request step3");
        request.setWebAuthenticationDetail(createWebAuthenticationDetail());
        //
        LOGGER.error("request partial created");
        VersionId versionId = new VersionId();
        versionId.setServiceId("aval");
        versionId.setMajor(4);
        versionId.setMinor(0);
        request.setVersion(versionId);
        //
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setCustomerTransactionId("java sample - AddressValidationRequest"); //This is a reference field for the customer.  Any value can be used and will be provided in the return.
        request.setTransactionDetail(transactionDetail);
        //
        LOGGER.error("Transaction set");
        Calendar c = Calendar.getInstance();
        request.setInEffectAsOfTimestamp(c);
    }


//    private boolean validateAddress(Address address) {
//
//        //this.isValidAddress = false;
//        ArrayList<String> addressMessages = new ArrayList<String>();
//        com.shippo.model.Address shippoAddress;
//
//        final Map<String, Object> AddressMap = new HashMap<String, Object>();
//        final String street = ShippoUtil.StreetFirstLineCheck(address.getStreet());
//
//        AddressMap.put("name", address.getName());
//        AddressMap.put("street1", street);
//        AddressMap.put("zip", address.getZip());
//        AddressMap.put("country", address.getCountry().getCode());
//        AddressMap.put("state", address.getState().getCode());
//        AddressMap.put("city", address.getCity());
//        AddressMap.put("validate", true);
//
//        try {
//            //final com.shippo.model.Address addressTo
//            shippoAddress = com.shippo.model.Address.create(AddressMap, this.shippingPluginProperty.getShippoApiToken());
//
//            //shippoAddress = com.shippo.model.Address.validate(addressTo.getObjectId(), this.shippingPluginProperty.getShippoApiToken());
//
//            if (this.shippingPluginProperty.isDebugMode()) {
//                Gson temp = new Gson();
//                LOGGER.error("Request: " + temp.toJson(AddressMap));
//                LOGGER.error("Return: " + temp.toJson(shippoAddress));
//            }
//            if (shippoAddress.getValidationResults() == null){
//                throw new APIConnectionException("Check Shippo API version");
//            }
//            boolean isValidAddress = shippoAddress.getValidationResults().getIsValid();
//            this.validStatus.put(address.getName(), String.valueOf(isValidAddress));
//
//
//            //LOGGER.error("To address validation: " + shippoAddress + " - " + isValidAddress);
//
//            if (!isValidAddress) {
//                addressMessages.add(address.getName() + " cannot be verified.");
//
//                for (com.shippo.model.Address.ValidationMessage message : shippoAddress.getValidationResults().getValidationMessages()) {
//                    addressMessages.add(message.getCode() + " - " + message.getText());
//
//                }
//
//                this.messageList.put(address.getName(), addressMessages);
//
//                return false;
//            }
//            else {
//                //is valid but shippo made some adjustments
//                if (shippoAddress.getValidationResults().getValidationMessages().size() > 0) {
//
//                    for (com.shippo.model.Address.ValidationMessage message : shippoAddress.getValidationResults().getValidationMessages()) {
//                        addressMessages.add(message.getCode() + " - " + message.getText());
//
//                        //LOGGER.error(message.getCode() + " - " + message.getText());
//                    }
//
//                    String changes = this.compareAndUpdateAddressFields(shippoAddress, address);
//                    addressMessages.add(changes);
//                    //this.updateSoAddress(this.orderInfo.getString("num"), addressTo);
//                    //this.reloadObject(); //shows changes on the SO module
//
//                    //we need to upadate our soInfo now that we have changes the address
//                    //this.orderInfo = (QueryRow) this.repository.getFbOrderInfo(this.soId, UOMConst.POUND.getId()).get(0);
//
//                    //this.isValidToAddress = false;
//
//
//                }
//                else {
//                    addressMessages.add("Valid Address, No changes made");
//                }
//            }
//
//
//
//        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException ex3) {
//            LOGGER.error(ex3.getMessage(), (Throwable) ex3);
//            addressMessages.add(ex3.getMessage());
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//
//        this.messageList.put(address.getName(), addressMessages);
//
//        return true;
//    }

    private boolean setFedExAddresses(ArrayList<Address> addresses) {

        //this.isValidAddress = false;
        ArrayList<String> addressMessages = new ArrayList<String>();
        //com.fedex.addressvalidation.stub.Address shippoAddress;

        ArrayList<AddressToValidate> addressesToValidate = new ArrayList<>();

        for (Address singleAddress : addresses) {
            AddressToValidate av1 = new AddressToValidate();

            com.fedex.ws.addressvalidation.v4.Address a1 = new com.fedex.ws.addressvalidation.v4.Address();
            a1.setCity(singleAddress.getCity());
            a1.setPostalCode(singleAddress.getZip());
            a1.setStreetLines(FedExUtil.StreetFirstLineCheck(singleAddress.getStreet()));
            a1.setCountryCode(singleAddress.getCountry().getCode());
            a1.setStateOrProvinceCode(singleAddress.getState().getCode());

            av1.setAddress(a1);
            av1.setClientReferenceId(String.valueOf(singleAddress.getId()));

            addressesToValidate.add(av1);
        }

        this.request.setAddressesToValidate(addressesToValidate.toArray(new AddressToValidate[0]));


        //this.messageList.put(address.getName(), addressMessages);

        return true;
    }
    
    private String getOutputMessage(){
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, ArrayList<String>> entry : this.messageList.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> value = entry.getValue();

            sb.append(key + ": ");
//            sb.append(System.getProperty("line.separator"));
            LOGGER.error("Array Size: " + value.size());
            LOGGER.error("Array: " + value.toString());
            if (value != null) {
                for (String message : value) {
                    sb.append(message);
                    sb.append(System.getProperty("line.separator"));
                }
            }
            else{
                sb.append("Address Validated");
                sb.append(System.getProperty("line.separator"));
            }

        }

        return sb.toString();
    }

    private String compareAndUpdateAddressFields(com.fedex.ws.addressvalidation.v4.AddressValidationResult addressReturned, Address address) {
        StringBuilder sb = new StringBuilder();
        final String street = ShippoUtil.StreetFirstLineCheck(address.getStreet());
        final String returnedStreet = ShippoUtil.StreetFirstLineCheck(addressReturned.getEffectiveAddress().getStreetLines());

        if (!returnedStreet.equalsIgnoreCase(street)) {
            sb.append("Street: ");
            sb.append(street);
            sb.append(" -> ");
            sb.append(returnedStreet);
            sb.append(System.getProperty("line.separator"));

            address.setStreet(returnedStreet);
        }

        if (!addressReturned.getEffectiveAddress().getCity().toString().equalsIgnoreCase(address.getCity())) {
            sb.append("City: ");
            sb.append(address.getCity());
            sb.append(" -> ");
            sb.append(addressReturned.getEffectiveAddress().getCity().toString());
            sb.append(System.getProperty("line.separator"));

            address.setCity(addressReturned.getEffectiveAddress().getCity().toString());
        }
        if (!addressReturned.getEffectiveAddress().getStateOrProvinceCode().toString().equalsIgnoreCase(address.getState().getCode())) {
            sb.append("State: ");
            sb.append(address.getState().getCode());
            sb.append(" -> ");
            sb.append(addressReturned.getEffectiveAddress().getStateOrProvinceCode().toString());
            sb.append(System.getProperty("line.separator"));

            State newState = new State();
            newState.setCode(addressReturned.getEffectiveAddress().getStateOrProvinceCode().toString());
            address.setState(newState); //might have to get the name of the state
        }
        if (!addressReturned.getEffectiveAddress().getPostalCode().toString().equalsIgnoreCase(address.getZip())) {
            sb.append("Zip: ");
            sb.append(address.getZip());
            sb.append(" -> ");
            sb.append(addressReturned.getEffectiveAddress().getPostalCode().toString());
            sb.append(System.getProperty("line.separator"));

            address.setZip(addressReturned.getEffectiveAddress().getPostalCode().toString());
        }

        if (addressReturned.getClassification() == FedExAddressClassificationType.RESIDENTIAL) {

            address.setResidential(true);
        }
        else{
            address.setResidential(false);
        }

        ArrayList<String> addressMessages = new ArrayList<String>();
        addressMessages.add(sb.toString());
        this.messageList.put(address.getName(), addressMessages);

        return sb.toString();

    }


    public void loadData(final int customerID) {
        final EVEvent event = this.getEveManager().createRequest("Customer", "getCustomerDetails");
        event.getSource().setModule(this.getModuleName());
        event.getSource().setHandler("Edit");
        event.add((Object)"customer ID", customerID);
        this.handleEvent(this.getEveManager().sendAndWait(event));
    }

    public EVEManager getEveManager() {
        return this.eveManager;
    }

    private boolean loadData(final EVEvent event) {
        try {
            if (event.getMessageType() == 201) {
                UtilGui.showMessageDialog(this.retrieveErrorMessage(event), "Error", 0);
            }
            else {
                final String handler = event.getDestination().getHandler();
                if (!handler.equals("Edit")) {
                    return false;
                }
                this.clearData();
                this.customerFPO = (CustomerFPO)event.getObject((Object)"customer", (Class)CustomerFPO.class);
//                final String parentName = event.getString((Object)"customerParent");
//                this.setParent(parentName);
                if (event.contains((Object) KeyConst.ADDRESS)) {
                    this.addresses = (ArrayList<Address>)event.getDecodedInfo((Object)KeyConst.ADDRESS, (Object)new ArrayList());
                }
                else {
                    this.addresses = new ArrayList<Address>();
                }
//                if (event.contains((Object)"account groups")) {
//                    this.memberOf = (ArrayList<Object>)event.getObject((Object)"account groups");
//                    this.updateAvailable();
//                }
//                else {
//                    this.memberOf = new ArrayList<Object>();
//                }
//                this.memoPanel.load(event, this.customerFPO.getId(), "Customer: " + this.customerFPO.getName());
//                if (this.customerTabbedPane.indexOfComponent((Component)this.pnlDropbox) == this.customerTabbedPane.getSelectedIndex()) {
//                    this.pnlDropbox.loadData(this.customerFPO.getName());
//                    this.showWallet = false;
//                }
//                if (this.showWallet) {
//                    this.pnlWallet.loadData(this.customerFPO.getId());
//                }
                this.customFields = (List<CustomField>)event.getList((Object)KeyConst.CUSTOM_FIELDS, (Class)CustomField.class);

//                this.customFieldPanel.setCustomFields(customFields);
//                this.setData();
                //this.enableControls(true);
            }
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage(), (Throwable)e);
        }
        return true;
    }

    private void clearData() {
        this.customerFPO = new CustomerFPO();
        this.addresses = new ArrayList<Address>();
//        this.memoPanel.clearMemos();
//        this.pnlAddressEdit.clearPanel();
//        this.pnlGroups.clear();
//        this.pnlDropbox.clearData();
//        this.pnlWallet.clearData();
//        this.lblCurrencyRate.setToolTipText(CurrencyModuleClient.getDefaultCurrencySummary(this.getEveManager()));
    }

    public boolean handleEvent(final EVEvent event) {
        boolean success = false;
        if (event.getMessageType() == 102) {
            final String handler = event.getDestination().getHandler();
            if (handler.equals("Edit")) {
                success = this.loadData(event);
                //this.getController().setModified(false);
            }
            else {
                success = handler.equals("Delete");
            }
        }
        else if (event.getMessageType() == 201) {
            success = this.handleException(event);
        }
        return success;
    }

    public boolean handleSaveEvent(final EVEvent event) {
        boolean success = false;
        if (event.getMessageType() == 102) {
            return true;
        }
        else if (event.getMessageType() == 201) {
            success = this.handleException(event);
        }
        return success;
    }

    public boolean handleException(final EVEvent event) {
        if (event.getMessageType() != 201) {
            return false;
        }
        final String message = this.retrieveErrorMessage(event);
        if (this.isShowing()) {
            this.showError(message);
        }
        else {
            final ExceptionMainFree exception = new ExceptionMainFree();
            exception.setMsgErr(message);
            //this.setLoadException(exception);
        }
        return true;
    }

    private boolean showError(final String message) {
        if (this.eveManager != null) {
            UtilGui.showMessageDialog(message, "Error", 0);
        }
        return true;
    }

    private ShippingPluginProperty getShippingProperties() {
        try {
            final ShippingPluginProperty property = new ShippingPluginProperty();

            property.setShippoApiToken(Property.SHIPPO_API_TOKEN.get(this));
            property.setDebugMode(Property.DEBUG_MODE.get(this));

            return property;
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public String getProperty(final String key) {
        return this.repository.getProperty(key);
    }


    //TODO: set the 4 key values in the shipping plugin settings
    private static ClientDetail createClientDetail() {
        ClientDetail clientDetail = new ClientDetail();
        String accountNumber = null;//System.getProperty("accountNumber");
        String meterNumber = null;//System.getProperty("meterNumber");
        //
        // See if the accountNumber and meterNumber properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (accountNumber == null) {
            accountNumber = "510087640"; // Replace "XXX" with clients account number
        }
        if (meterNumber == null) {
            meterNumber = "119012100"; // Replace "XXX" with clients meter number
        }
        clientDetail.setAccountNumber(accountNumber);
        clientDetail.setMeterNumber(meterNumber);
        return clientDetail;
    }

    private static WebAuthenticationDetail createWebAuthenticationDetail() {
        WebAuthenticationCredential userCredential = new WebAuthenticationCredential();
        String key = null;//System.getProperty("key");
        String password = null;//System.getProperty("password");
        //
        // See if the key and password properties are set,
        // if set use those values, otherwise default them to "XXX"
        //
        if (key == null) {
            key = "QoeRtgdolu1ykZdW"; // Replace "XXX" with clients key
        }
        if (password == null) {
            password = "9brtNXqsJbdWphdZrQjUvl1Ga"; // Replace "XXX" with clients password
        }
        userCredential.setKey(key);
        userCredential.setPassword(password);

        WebAuthenticationCredential parentCredential = null;
        Boolean useParentCredential=false; //Set this value to true is using a parent credential
        if(useParentCredential){

            String parentKey = null;//System.getProperty("parentkey");
            String parentPassword = null;//System.getProperty("parentpassword");
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
        return webAuthenticationDetail; //WebAuthenticationDetail(parentCredential, userCredential);
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

//    private static void updateEndPoint(AddressValidationServiceLocator serviceLocator) {
//        String endPoint = System.getProperty("endPoint");
//        if (endPoint != null) {
//            serviceLocator.setAddressValidationServicePortEndpointAddress(endPoint);
//        }
//    }

    private static void printNotifications(Notification[] notifications) {
        System.out.println("Notifications:");
        if (notifications == null || notifications.length == 0) {
            System.out.println("  No notifications returned");
        }
        for (int i=0; i < notifications.length; i++){
            Notification n = notifications[i];
            System.out.print("  Notification no. " + i + ": ");
            if (n == null) {
                System.out.println("null");
                continue;
            } else {
                System.out.println("");
            }
            NotificationSeverityType nst = n.getSeverity();

            System.out.println("    Severity: " + (nst == null ? "null" : nst.getValue()));
            System.out.println("    Code: " + n.getCode());
            System.out.println("    Message: " + n.getMessage());
            System.out.println("    Source: " + n.getSource());
        }
    }

    private ArrayList<String> getNotifications(Notification[] notifications) {
        //System.out.println("Notifications:");
        ArrayList<String> notificationList = new ArrayList<>();

        if (notifications == null || notifications.length == 0) {
            notificationList.add("  No notifications returned");
        }
        for (int i=0; i < notifications.length; i++){
            Notification n = notifications[i];
            //System.out.print("  Notification no. " + i + ": ");
//            if (n == null) {
//                System.out.println("null");
//                continue;
//            } else {
//                System.out.println("");
//            }
            NotificationSeverityType nst = n.getSeverity();

            notificationList.add("    Severity: " + (nst == null ? "null" : nst.getValue()));
            notificationList.add("    Code: " + n.getCode());
            notificationList.add("    Message: " + n.getMessage());
            //System.out.println("    Source: " + n.getSource());
        }

        return notificationList;
    }

    public List<QueryRow> executeSql(final String query) {
        return (List<QueryRow>)this.runQuery(query);
    }

    public final String retrieveErrorMessage(final EVEvent event) {
        String[] params = null;
        String message;
        if (event.getBoolean((Object)"isThrowable")) {
            message = "Fishbowl Server Error:  " + event.getString((Object)"errorMessage");
        }
        else {
            params = (String[])event.get((Object)"errorParams");
            message = ErrorHandler.getExceptionMsg(event);
        }
        if (params != null) {
            try {
                message = MessageFormat.format(message, (Object[])params);
            }
            catch (Exception e) {
                message = "Unable to format error message from server: {0} The unformatted message was: " + message;
                params = new String[] { e.getClass().getName() };
                message = MessageFormat.format(message, (Object[])params);
            }
        }
        return message;
    }

    static {
        LOGGER = LoggerFactory.getLogger((Class)AddressValidateButton.class);
    }

}
