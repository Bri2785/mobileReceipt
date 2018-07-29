// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.util.property;

import com.fbi.plugins.briteideas.util.property.reader.LongReader;
import com.fbi.plugins.briteideas.util.property.reader.IntegerReader;
import com.fbi.plugins.briteideas.util.property.reader.DoubleReader;
import com.fbi.plugins.briteideas.util.property.reader.BooleanReader;
import com.fbi.plugins.briteideas.util.property.reader.StringReader;

public final class Property
{
    private static final StringReader STRING;
    private static final BooleanReader BOOLEAN_DEFAULT_FALSE;
    private static final BooleanReader BOOLEAN_DEFAULT_TRUE;
    private static final DoubleReader DOUBLE;
    private static final IntegerReader INTEGER;
    private static final LongReader LONG;
    public static final GlobalProperty<String> USERNAME;
    public static final GlobalProperty<String> PASS;
    public static final GlobalProperty<Long> LAST_SYNC_DATE;

    public static final GlobalProperty<Boolean> DEBUG_MODE;
    public static final GlobalProperty<String> SHIPPO_API_TOKEN;

    public static final GlobalProperty<String> LABEL_TEXT;
    public static final GlobalProperty<String> LABEL_FORMAT;
    public static final GlobalProperty<Boolean> SHOW_NO_VALIDATE_WARNING;
    public static final GlobalProperty<Boolean> AUTO_FULFILL_ORDERS;
    public static final GlobalProperty<Boolean> SET_CARTON_COST;
    public static final GlobalProperty<Boolean> PURCHASE_INSURANCE;
    public static final GlobalProperty<Boolean> RETAIL_RATES_ONLY;
    public static final GlobalProperty<Boolean> USE_UPS;
    public static final GlobalProperty<Boolean> USE_FEDEX;
    public static final GlobalProperty<Boolean> USE_USPS;
    public static final GlobalProperty<Boolean> USE_MARKUP;
    public static final GlobalProperty<Boolean> USE_PERCENT;
    public static final GlobalProperty<Double> MARKUP_PERCENT;
    public static final GlobalProperty<Double> MARKUP_RATE;
    public static final GlobalProperty<Integer> SHIPPING_ITEM_ID;
    public static final GlobalProperty<Boolean> SHOW_ZERO_QTY_WARING;
    
    static {
        STRING = new StringReader();
        BOOLEAN_DEFAULT_FALSE = new BooleanReader(false);
        BOOLEAN_DEFAULT_TRUE = new BooleanReader(true);
        DOUBLE = new DoubleReader(0.0);
        INTEGER = new IntegerReader();
        LONG = new LongReader();
        USERNAME = new GlobalProperty<String>("ShippingUsername", Property.STRING);
        PASS = new GlobalProperty<String>("ShippingPassword", Property.STRING);
        LAST_SYNC_DATE = new GlobalProperty<Long>("lastSyncDate", Property.LONG);
        SHIPPO_API_TOKEN = new GlobalProperty<String>("shippoApiToken", Property.STRING);
        LABEL_TEXT = new GlobalProperty<String>("labelText", Property.STRING);
        LABEL_FORMAT = new GlobalProperty<String>("labelFormat", Property.STRING);
        SHOW_NO_VALIDATE_WARNING = new GlobalProperty<Boolean>("showNoValidateWarning", Property.BOOLEAN_DEFAULT_TRUE);
        AUTO_FULFILL_ORDERS = new GlobalProperty<Boolean>("autoFulfillOrders", Property.BOOLEAN_DEFAULT_TRUE);
        SET_CARTON_COST = new GlobalProperty<Boolean>("setCartonCost", Property.BOOLEAN_DEFAULT_TRUE);
        PURCHASE_INSURANCE = new GlobalProperty<Boolean>("purchaseInsurance", Property.BOOLEAN_DEFAULT_FALSE);
        RETAIL_RATES_ONLY = new GlobalProperty<Boolean>("retailRatesOnly", Property.BOOLEAN_DEFAULT_FALSE);
        USE_UPS = new GlobalProperty<Boolean>("useUps", Property.BOOLEAN_DEFAULT_FALSE);
        USE_FEDEX = new GlobalProperty<Boolean>("useFedex", Property.BOOLEAN_DEFAULT_FALSE);
        USE_USPS = new GlobalProperty<Boolean>("useUsps", Property.BOOLEAN_DEFAULT_FALSE);
        USE_MARKUP = new GlobalProperty<Boolean>("useMarkup", Property.BOOLEAN_DEFAULT_FALSE);
        USE_PERCENT = new GlobalProperty<Boolean>("usePercent", Property.BOOLEAN_DEFAULT_TRUE);
        MARKUP_PERCENT = new GlobalProperty<Double>("markupPercent", Property.DOUBLE);
        MARKUP_RATE = new GlobalProperty<Double>("markupRate", Property.DOUBLE);
        SHIPPING_ITEM_ID = new GlobalProperty<Integer>("shippingItemId", Property.INTEGER);
        SHOW_ZERO_QTY_WARING = new GlobalProperty<Boolean>("showZeroItemWarning", Property.BOOLEAN_DEFAULT_TRUE);

        DEBUG_MODE = new GlobalProperty<Boolean>("debugMode", Property.BOOLEAN_DEFAULT_FALSE);
    }
}
