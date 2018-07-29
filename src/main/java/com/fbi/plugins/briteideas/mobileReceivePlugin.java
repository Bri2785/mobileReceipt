// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas;

import com.evnt.client.common.EVEManager;
import com.evnt.client.common.EVEManagerUtil;
import com.evnt.common.swing.swingutil.RefreshTitlePanel;
import com.fbi.gui.misc.GUIProperties;
import com.fbi.gui.misc.GUISavable;
import com.fbi.gui.misc.IconTitleBorderPanel;
import com.fbi.gui.panel.FBSplitPane;
import com.fbi.gui.table.FBTable;
import com.fbi.gui.table.FBTableModel;
import com.fbi.plugins.briteideas.data.MobileReceiptItem;
import com.fbi.plugins.briteideas.panels.MobileReceiptSearchPanel;
import com.fbi.plugins.briteideas.util.rowdata.MobileReceiptItemRowData;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.fbi.fbo.message.response.ResponseBase;
import com.fbi.fbo.message.request.RequestBase;
import com.fbi.fbo.impl.ApiCallType;
import com.fbi.plugins.briteideas.exception.FishbowlException;
//import com.fbi.commerce.shipping.util.Compatibility;

//import com.fbi.commerce.shipping.type.ShipService;
//import com.fbi.commerce.shipping.type.ShipServiceEnum;
import com.fbi.fbdata.setup.FbScheduleFpo;
//import SyncShipments;
import com.fbi.fbo.impl.dataexport.QueryRow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fbi.gui.util.UtilGui;

//import com.fbi.commerce.shipping.connection.HubApi;
//import com.fbi.commerce.shipping.util.CommerceUtil;
import com.fbi.sdk.constants.MenuGroupNameConst;
import com.fbi.gui.button.FBMainToolbarButton;

import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;

import com.fbi.gui.panel.TitlePanel;
import org.slf4j.Logger;
import com.fbi.plugins.briteideas.fbapi.ApiCaller;
import com.fbi.plugins.briteideas.repository.MRRepository;
import com.fbi.plugins.briteideas.util.property.PropertyGetter;
import com.fbi.plugins.FishbowlPlugin;

public class mobileReceivePlugin extends FishbowlPlugin implements PropertyGetter, MRRepository.RunSql, ApiCaller
{

    private static final Logger LOGGER;
    private MRRepository MRRepository;


    //main container
    private FBSplitPane splMRSearch;

    //left component
    private JPanel leftPanel;
    private IconTitleBorderPanel iconTitleBorderPanel1;
    private JPanel searchPanel;
    private JPanel searchFooter;
    private JButton editButton;
    private MobileReceiptSearchPanel mobileReceiptSearchPanel;

    //right component
    private JPanel rightPanel;
    private RefreshTitlePanel pnlTitle; //title panel at top of right side
    private JPanel contentPanel;
    private JPanel itemDetailsPanel; //our final table goes in here

    private JPanel pnlRightContainer;
    private JScrollPane scrRightScrollPane;

    private FBTable tblReceiptItems;
    private FBTableModel<MobileReceiptItemRowData> mdlReceiptItems;



    EVEManager eveManager = EVEManagerUtil.getEveManager(); //get access to eve manager


    private TitlePanel label1;
    private JToolBar mainToolBar;

    private FBMainToolbarButton btnSave;

    
    public mobileReceivePlugin() {
        this.MRRepository = new MRRepository(this);
        this.setModuleName("Mobile Receive");
        this.setMenuListLocation(4);
        this.setMenuGroup(MenuGroupNameConst.PURCHASING);
        this.setMenuListLocation(1000); //bottom of the list
        this.setIconClassPath("/images/receive48x48.png");
        //this.setDefaultHelpPath("https://www.fishbowlinventory.com/wiki/ShipExpress");
//        this.addAccessRight("Rates Button");
//        this.addAccessRight("Ship Button");
//        this.addAccessRight("View Label Button");
    }

    public void registerSources() {
        //ProductSearchDialog.clearCache();
        //this.eveManager.getRepository().registerSource(ProductModule.SOURCE_TREE_LIST, ProductModule.SOURCE_TREE_TRIGGER, "Product", "getProductTree");
        //this.eveManager.getCacheFBData().registerList(CacheListTypeConst.PRODUCT, "Product", "getListProducts");
    }
    
    
    protected void initModule() {
        super.initModule();
        this.initComponents();
        //LOGGER.error("Made it after the InitComponents method");
        this.setMainToolBar(this.mainToolBar);
        this.initLayout();

        this.mobileReceiptSearchPanel.init();//this.eveManager);

        this.mobileReceiptSearchPanel.getMobileReceiptSearchResults().addKeyListener((KeyListener)new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent evt) {
            }

            @Override
            public void keyPressed(final KeyEvent evt) {
                if (evt.getKeyCode() == 10) {
                    mobileReceivePlugin.this.loadSelectedReceipt();
                    final JTable dataTable = (JTable)mobileReceivePlugin.this.mobileReceiptSearchPanel.getMobileReceiptSearchResults().getTblData();
                    int selectedRowIndex = dataTable.getSelectedRow();
                    if (selectedRowIndex < dataTable.getRowCount() - 1) {
                        ++selectedRowIndex;
                    }
                    else {
                        selectedRowIndex = 0;
                    }
//                    final int selectedID = mobileReceivePlugin.this.mobileReceiptSearchPanel.getMobileReceiptSearchResults().getTblModel().getTableData().get(selectedRowIndex).getID();
//                    mobileReceivePlugin.this.mobileReceiptSearchPanel.getInventoryStatsPanel().setSelectedID(selectedID);
                    dataTable.requestFocus();
                }
            }

            @Override
            public void keyReleased(final KeyEvent evt) {
            }
        });


        GUIProperties.registerComponent((GUISavable)this.splMRSearch, this.getModuleName());
        GUIProperties.registerComponent((GUISavable)this.tblReceiptItems, this.getModuleName());

        GUIProperties.loadComponents(this.getModuleName());

        //blank init to show columns
        this.populateTable(tblReceiptItems, new ArrayList<>(), 0);

        //LOGGER.error("Made it after the initLayout method");
    }

    public boolean activateModule(){
        if (this.eveManager.isConnected()) {
            super.activateModule();
            if (this.isInitialized()) {
                this.executeSearch();
            }
            return this.isInitialized();
        }
        return false;
    }
    
    private void initLayout() {
        //this.pnlLogin = new LoginPanel(this, this.repository);

        //this.pnlBrowserSettings = new BrowserSettingsPanel(this);

        //this.pnlNoAccess = new NoAccessPanel();

        //this.pnlCards.add(this.pnlLogin, "login");
        //this.pnlCards.add(this.pnlBrowserSettings, "browserSettings");
        //this.pnlCards.add(this.pnlNoAccess, "noAccess");


        //this.hideShowPanels();
    }


    private void mobileReceiptSearchPanelMouseClicked(MouseEvent evt) {
        if (evt.getComponent() instanceof JTable) {
            switch (evt.getButton()) {
                case 1: {
                    if (evt.getClickCount() == 2) {
                        this.loadSelectedReceipt();
                        break;
                    }
                    break;
                }
                case 2:
                case 3: {
                    final boolean itemsSelected = !this.mobileReceiptSearchPanel.getMobileReceiptSearchResults().getSelectedRowsData().isEmpty();
                    //this.mniInactivate.setEnabled(itemsSelected && this.hasEditRights);
                    //this.mnuSearch.show(evt.getComponent(), evt.getX(), evt.getY());
                    break;
                }
            }
        }

    }

    private void editButtonActionPerformed() {
        this.loadSelectedReceipt();
    }

    private void loadSelectedReceipt() {
        final int selectedID = this.mobileReceiptSearchPanel.getMobileReceiptSearchResults().getSelectedID();
        if (selectedID == -1) {
            //UtilGui.showErrorMessageDialog(this.bundle.getString("ProductModuleClient.loadSelectedProduct.message", new Object[0]), this.bundle.getString("ProductModuleClient.loadSelectedProduct.title", new Object[0]));
            UtilGui.showErrorMessageDialog("No receipt selected", "Error");

            return;
        }
        //if (this.saveAnyChanges()) {
            this.loadData(selectedID);
        //}
    }

    public int getObjectId() {
        return 1;
    }
    
    public void loadData(final int objectId) {
        //load all of the items in the right pane table
        //tabbed browser with list on one tab and the receiving match up on the second?

        GUIProperties.saveComponents(this.getModuleName());

        try {
            this.mdlReceiptItems = this.populateTable(tblReceiptItems, MRRepository.getMobileReceiptItemsbyReceipt(objectId), objectId);
        }
        catch (Exception e){
            LOGGER.error("Error loading data", e);
        }
        //UtilGui.showMessageDialog("Selected receipt id " + objectId);

//        if (this.pnlBrowserSettings == null) {
//            return;
//        }
//        this.hideShowPanels();
//        this.pnlBrowserSettings.reload();
    }

    private FBTableModel<MobileReceiptItemRowData> populateTable(FBTable tblReceiptItems, List<MobileReceiptItem> mobileReciptItems, int objectId) {
        try{
            FBTableModel<MobileReceiptItemRowData> model = new FBTableModel<MobileReceiptItemRowData>(tblReceiptItems, MobileReceiptItemRowData.getSettings());

            //tblReceiptItems.getColumnModel().getColumn(0).
            //tblReceiptItems.getColumnModel().getColumn(0).setPreferredWidth(MobileReceiptItemRowData.colMobileReceiptId.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(0).setPreferredWidth(MobileReceiptItemRowData.colUpc.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(1).setPreferredWidth(MobileReceiptItemRowData.colLastScan.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(2).setPreferredWidth(MobileReceiptItemRowData.colProductNum.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(3).setPreferredWidth(MobileReceiptItemRowData.colQtyScanned.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(4).setPreferredWidth(MobileReceiptItemRowData.colUomMultiply.getColDefWidth());
//            tblReceiptItems.getColumnModel().getColumn(5).setPreferredWidth(MobileReceiptItemRowData.colTotalReceived.getColDefWidth());


            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

            //tblReceiptItems.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
            tblReceiptItems.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);


            //populate data
            if (mobileReciptItems.size() > 0){

                for (MobileReceiptItem item: mobileReciptItems) {
                    model.addRow( new MobileReceiptItemRowData((item)));
                }

                return model;
            }
            else{
                LOGGER.error("Items list is blank");
            }



        }
        catch (Exception e){
            LOGGER.error("Error populating table", e);
        }
        return null;
    }

    private void btnSetupActionPerformed() {
//        final SetupConnectionDialog setupConnectionDialog = new SetupConnectionDialog(this, true, this.repository);
//        if (setupConnectionDialog.getResult() == 1) {
//            this.hideShowPanels();
//        }
    }
    
    void hideShowPanels() {
//        final String username = Property.USERNAME.get(this);
//        final String password = Property.PASS.get(this);
//        final CardLayout layout = (CardLayout)this.pnlCards.getLayout();
//
//        this.enableControls(true);
//        layout.show(this.pnlCards, "browserSettings");

//        if (!Util.isEmpty(username) && !Util.isEmpty(password)) {
//            new Thread(() -> {
//                try {
//                    LOGGER.error("Starting to connect");
//                    //HubApi.checkCredentials(CommerceUtil.decrypt(username), CommerceUtil.decrypt(password));
//
//                    this.pnlBrowserSettings.showLoginPage();
//                    this.enableControls(true);
////                    LOGGER.error("ShipExpree about to be loaded");
////                    LOGGER.error("shippoAPIToken: " + (String)Property.SHIPPO_API_TOKEN.get(this));
//                    this.pnlBrowserSettings.showPnlShipExpress(!Util.isEmpty((String)Property.SHIPPO_API_TOKEN.get(this)));
//                    layout.show(this.pnlCards, "browserSettings");
//                }
////                catch (AuthenticationException | CommerceException ex2) {
////                    LOGGER.error("Authentication Error");
////                    this.displayConnectionErrors(ex2, "We are unable to connect to Fishbowl ShipExpress. ", "Please check your email and password");
////                }
////                catch (DownForMaintenanceException e2) {
////                    this.displayConnectionErrors(e2, "The ShipExpress plugin is shutting down for maintenance. ", "Please try again later.");
////                }
////                catch (OutOfDateException e3) {
////                    this.displayConnectionErrors(e3, "You need to update your ShipExpress plugin. ", null);
////                }
////                catch (ConnectionException e4) {
////                    this.displayConnectionErrors(e4, "We are unable to connect to Fishbowl ShipExpress. ", "Please try again later.");
////                }
//                catch (Exception e){
//                    LOGGER.error(e.getMessage());
//                }
//            }).start();
//        }
//        else {
//            this.enableControls(false);
//            layout.show(this.pnlCards, "login");
////            this.pnlBrowserSettings.showLoginPage();
////            this.enableControls(true);
////            this.pnlBrowserSettings.showPnlShipExpress(!Util.isEmpty((String)Property.SHIPPO_API_TOKEN.get(this)));
////            layout.show(this.pnlCards, "browserSettings");
//        }
    }
    
    private void enableControls(final boolean enable) {
        this.btnSave.setEnabled(enable);
        //this.btnSync.setEnabled(enable);
        //this.btnBack.setEnabled(enable);
        //this.btnForward.setEnabled(enable);
    }
    
    private void displayConnectionErrors(final Exception e, final String line1Text, final String line2Text) {
//        ShippingPlugin.LOGGER.error(e.getMessage(), (Throwable)e);
//        this.pnlNoAccess.setMessage(line1Text, line2Text);
//        final CardLayout layout = (CardLayout)this.pnlCards.getLayout();
//        layout.show(this.pnlCards, "noAccess");
    }
    
    void saveProperties(final String username, final String password) {
//        try {
//            final String encryptedUsername = CommerceUtil.encrypt(username);
//            final String encryptedPassword = CommerceUtil.encrypt(password);
//            final Map<String, String> properties = new HashMap<String, String>();
//            properties.put(Property.USERNAME.getKey(), encryptedUsername);
//            properties.put(Property.PASS.getKey(), encryptedPassword);
//            this.savePluginProperties((Map)properties);
//        }
//        catch (CommerceException e) {
//            __Plugin.LOGGER.error("Can't save", (Throwable)e);
//            final String message = "Unable to save your ShipExpress integration.";
//            UtilGui.showMessageDialog(message, "Save Error", 1);
//        }
    }
    
    public String getProperty(final String key) {
        return this.MRRepository.getProperty(key);
    }
    
    public List<QueryRow> executeSql(final String query) {
        return (List<QueryRow>)this.runQuery(query);
    }
    
    private void btnSaveActionPerformed() {
//        this.createLabelIdCustomField(); //Good and Done
//        this.addCarrierAndServices();
        //this.pnlBrowserSettings.saveSettings();
        //final Random r = new Random();
        //this.createScheduledTask("ShipExpress Shipment Sync", SyncShipments.class, r.nextInt(60) + " " + r.nextInt(15) + "-59/15 * * * *");
        //this.pnlBrowserSettings.showPnlShipExpress(!Util.isEmpty((String)Property.SHIPPO_API_TOKEN.get(this)));
    }
    
    private void createScheduledTask(final String name, final Class clazz, final String cron) {
        try {
            if (this.getScheduledTask(name) == null) {
                final FbScheduleFpo schedule = this.createSchedule(name, "DO NOT DELETE", clazz, "", cron);
                this.updateScheduledTask(schedule);
            }
        }
        catch (Exception e) {
            mobileReceivePlugin.LOGGER.error(e.getMessage(), (Throwable)e);
            throw new IllegalStateException(e);
        }
    }

    
//    private ImportRequest createImportRequest(final Map<String, List<String>> servicesToAdd, final List<QueryRow> carriersAndServices, final String fbFedEx, final String fbFedExDesc, final String fbUsps, final String fbUspsDesc, final String fbUps, final String fbUpsDesc) {
//        final ArrayList<String> importRows = new ArrayList<String>();
//        final StringRowData headerRow = new StringRowData(IECarrierImpl.defaultHeader);
//        importRows.add(headerRow.toString());
//        Integer carrierId = carriersAndServices.get(0).getInt("carrierId");
//        for (final Map.Entry<String, List<String>> entry : servicesToAdd.entrySet()) {
//            for (final String service : entry.getValue()) {
//                final StringRowData dataRow = new StringRowData(IECarrierImpl.defaultHeader);
//                dataRow.setColumnNames(headerRow);
//                dataRow.set("Name", this.getCarrierName(entry.getKey(), fbFedEx, fbUsps, fbUps));
//                dataRow.set("ServiceName", service);
//
//                final String s = "ServiceCode";
//
//                final String carrierName = entry.getKey();
//                final Integer n = carrierId;
//                ++carrierId;
//                dataRow.set(s, this.getServiceCode(service, carrierName, n));
//                dataRow.set("Active", Boolean.TRUE.toString());
//                dataRow.set("ServiceActive", Boolean.TRUE.toString());
//                Compatibility.setCarrierDescription(this.repository, dataRow, entry.getKey(), fbFedExDesc, fbUspsDesc, fbUpsDesc);
//                importRows.add(dataRow.toString());
//            }
//        }
//        final ImportRequest importRequest = (ImportRequest)new ImportRequestImpl();
//        importRequest.setImportType("ImportCarriers");
//        importRequest.setRows((ArrayList)importRows);
//        return importRequest;
//    }

    
//    void enableBack(final boolean browserIsShowing) {
//        this.btnBack.setEnabled(browserIsShowing);
//    }
//
//    void enableForward(final boolean browserIsShowing) {
//        this.btnForward.setEnabled(browserIsShowing);
//    }
    private FBTable createTable(final Supplier<FBTableModel<MobileReceiptItemRowData>> model) {
        final FBTable table = new FBTable();
        table.setFont(new Font("Tahoma", 0, 12));
        table.setRowHeight(28);
    //        table.addMouseListener((MouseClickListener)e -> {
    //            //LOGGER.error("Column Clicked: " + table.getColumnModel().getColumnIndex(CarrierRowData.colAction.getColName()));
    //            if (table.getSelectedColumn() == table.getColumnModel().getColumnIndex(CarrierRowData.colAction.getColName())) {
    //                //TODO: for freight make sure carriers exist in FB first
    //                this.shipRatesDialog.addShippingLineToSo(((CarrierRowData) model.get().getSelectedRowData()).getData());
    //            }
    //            return;
    //        });
        return table;
    }

    public ResponseBase call(final ApiCallType requestType, final RequestBase requestBase) throws FishbowlException {
        try {
            return this.runApiRequest(requestType, requestBase);
        }
        catch (Exception e) {
            throw new FishbowlException(e);
        }
    }
    
    private void initComponents() {
        //main screen
        this.tblReceiptItems = this.createTable(() -> this.mdlReceiptItems);

        this.setName("this");
        this.setLayout((LayoutManager)new BorderLayout());

        this.splMRSearch = new FBSplitPane();

        //left component
        this.leftPanel = new JPanel();
        this.iconTitleBorderPanel1 = new IconTitleBorderPanel();
        this.searchPanel = new JPanel();
        this.mobileReceiptSearchPanel = new MobileReceiptSearchPanel(this.MRRepository);

        this.searchFooter = new JPanel();
        this.editButton = new JButton();

        //right component
        this.rightPanel = new JPanel();
        this.pnlTitle = new RefreshTitlePanel();
        this.contentPanel = new JPanel();
        this.itemDetailsPanel = new JPanel();

        this.scrRightScrollPane = new JScrollPane();
        this.pnlRightContainer = new JPanel();


        //main split panel
        this.splMRSearch.setDividerLocation(220);
        this.splMRSearch.setOneTouchExpandable(true);
        this.splMRSearch.setFocusable(false);
        this.splMRSearch.setName("splMRSearch");
    //left side parts
        //left panel
        this.leftPanel.setMinimumSize(new Dimension(1, 1));
        this.leftPanel.setName("leftPanel");
        this.leftPanel.setLayout(new BorderLayout());

        //left panel inside
        this.iconTitleBorderPanel1.setTitle("Search");
        this.iconTitleBorderPanel1.setType(IconTitleBorderPanel.IconConst.Search);
        this.iconTitleBorderPanel1.setName("iconTitleBorderPanel1");
        this.iconTitleBorderPanel1.setLayout((LayoutManager)new GridBagLayout());
        ((GridBagLayout)this.iconTitleBorderPanel1.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.iconTitleBorderPanel1.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout)this.iconTitleBorderPanel1.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.iconTitleBorderPanel1.getLayout()).rowWeights = new double[] { 1.0, 1.0E-4 };

        //icon panel inside
        this.searchPanel.setName("searchPanel");
        this.searchPanel.setLayout(new BorderLayout());

        this.mobileReceiptSearchPanel.setName("mobileReceiptSearchPanel");
        this.mobileReceiptSearchPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                mobileReceivePlugin.this.mobileReceiptSearchPanelMouseClicked(e);
            }
        });
        //add productSearchpanel to search panel
        this.searchPanel.add(this.mobileReceiptSearchPanel, "Center");

        this.searchFooter.setName("search");
        this.searchFooter.setLayout(new FlowLayout(2));
        this.editButton.setMnemonic('i');
        this.editButton.setText("View");
        this.editButton.setToolTipText("View the selected mobile receipt");
        this.editButton.setName("MobileReceiptSearchEditButton");
        this.editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                mobileReceivePlugin.this.editButtonActionPerformed();
            }
        });
        this.searchFooter.add(this.editButton);

        //add view button below search panel
        this.searchPanel.add(this.searchFooter, "South");
        //add search panel to icon panel
        this.iconTitleBorderPanel1.add((Component)this.searchPanel, (Object)new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        //add icon panel to the left panel
        this.leftPanel.add((Component)this.iconTitleBorderPanel1, "Center");
        //add left lanel to the left component of hte split panel
        this.splMRSearch.setLeftComponent((Component)this.leftPanel);



    //right side parts
        //right panel
        this.rightPanel.setMinimumSize(new Dimension(1, 1));
        this.rightPanel.setName("rightPanel");
        this.rightPanel.setLayout(new GridBagLayout());


        this.pnlTitle.setBorder(new EtchedBorder());
        this.pnlTitle.setModuleIcon(new ImageIcon(this.getClass().getResource("/images/receive32x32.png")));
        this.pnlTitle.setModuleTitle("Mobile Receipt Items");
        this.pnlTitle.setName("pnlTitle");

        this.contentPanel.setName("contentPanel");
        this.contentPanel.setLayout(new GridBagLayout());

        this.itemDetailsPanel.setPreferredSize(new Dimension(10, 325));
        this.itemDetailsPanel.setName("itemDetailsPanel");
        this.itemDetailsPanel.setLayout(new GridBagLayout());


    //table
        this.scrRightScrollPane.setName("scrRightPane");

        this.tblReceiptItems.setName("tblItems");
//        this.tblReceiptItems.addMouseListener(new MouseAdapter(){
//            @Override
//            public void mouseClicked(final MouseEvent e){
//                mobileReceivePlugin.this.convItemsTableMouseClicked(e);
//            }
//        });



        this.scrRightScrollPane.setViewportView((Component)this.tblReceiptItems);

        this.pnlRightContainer.setName("pnlConversions");
        this.pnlRightContainer.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlRightContainer.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlRightContainer.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlRightContainer.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlRightContainer.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.pnlRightContainer.add(this.scrRightScrollPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));

        //add table panel to the details panel
        this.itemDetailsPanel.add(this.pnlRightContainer, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));








        //add item detail panel to content panel
        this.contentPanel.add(this.itemDetailsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        //add title panel to the right panel
        this.rightPanel.add(this.pnlTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        //add cintent panel under the title panel
        this.rightPanel.add(this.contentPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        //add right panel to the split view as the right component
        this.splMRSearch.setRightComponent((Component)this.rightPanel);

     //add the title panel to the main screen
        //Title menu
        this.label1 = new TitlePanel();
        this.label1.setModuleIcon((Icon)new ImageIcon(this.getClass().getResource("/images/receive32x32.png")));
        this.label1.setModuleTitle("Mobile Receipts");
        this.label1.setBackground(new Color(44, 94, 140));
        this.label1.setName("label1");
        this.add((Component)this.label1, (Object)"North");

        //finally add the split panel to the main screen in the center
        this.add((Component)this.splMRSearch, (Object)"Center");


     //toolbar items
        this.mainToolBar = new JToolBar();

        this.mainToolBar.setFloatable(false);
        this.mainToolBar.setRollover(true);
        this.mainToolBar.setName("mainToolBar");

        this.btnSave = new FBMainToolbarButton();
//        this.btnSetup.setIcon((Icon)new ImageIcon(this.getClass().getResource("/icon24/textanddocuments/documents/document_new.png")));
//        this.btnSetup.setText("Setup");
//        this.btnSetup.setHorizontalTextPosition(0);
//        this.btnSetup.setIconTextGap(0);
//        this.btnSetup.setMargin(new Insets(0, 2, 0, 2));
//        this.btnSetup.setName("PartToolbarNewbtn");
//        this.btnSetup.setVerticalTextPosition(3);
//        this.btnSetup.setToolTipText("Setup integration");
//        this.btnSetup.addActionListener((ActionListener)new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                upcPlugin.this.btnSetupActionPerformed();
//            }
//        });
//        this.mainToolBar.add((Component)this.btnSetup);
        this.btnSave.setIcon((Icon)new ImageIcon(this.getClass().getResource("/icon24/filesystem/disks/disk_gold.png")));
        this.btnSave.setText("Save");
        this.btnSave.setToolTipText("Save");
        this.btnSave.setHorizontalTextPosition(0);
        this.btnSave.setIconTextGap(0);
        this.btnSave.setMargin(new Insets(0, 2, 0, 2));
        this.btnSave.setName("btnSave");
        this.btnSave.setVerticalTextPosition(3);
        this.btnSave.addActionListener((ActionListener)new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                mobileReceivePlugin.this.btnSaveActionPerformed();
            }
        });
        this.mainToolBar.add((Component)this.btnSave);


        //seperator in between the button as we add them

//        jSeparator1.setOrientation(1);
//        jSeparator1.setMaximumSize(new Dimension(10, 50));
//        jSeparator1.setName("jSeparator1");
//        this.mainToolBar.add(jSeparator1);

//        this.btnBack.setIcon((Icon)new ImageIcon(this.getClass().getResource("/images/nav_left24.png")));
//        this.btnBack.setButtonSize(new Dimension(53, 50));
//        this.btnBack.setToolTipText("Click to go back");
//        this.btnBack.setText("Back");
//        this.btnBack.setName("btnBack");
//        this.btnBack.addActionListener((ActionListener)new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                upcPlugin.this.btnBackActionPerformed();
//            }
//        });
//        this.mainToolBar.add((Component)this.btnBack);
//        this.btnForward.setIcon((Icon)new ImageIcon(this.getClass().getResource("/images/nav_right24.png")));
//        this.btnForward.setToolTipText("Click to go forward");
//        this.btnForward.setText("Forward");
//        this.btnForward.setName("btnForward");
//        this.btnForward.addActionListener((ActionListener)new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                upcPlugin.this.btnForwardActionPerformed();
//            }
//        });
//        this.mainToolBar.add((Component)this.btnForward);
    }




    static {
        LOGGER = LoggerFactory.getLogger((Class)mobileReceivePlugin.class);
    }
}
