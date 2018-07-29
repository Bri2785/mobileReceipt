// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.buttons.soratesbutton;

import java.awt.*;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.util.function.Supplier;

import com.fbi.commerce.shipping.util.MouseClickListener;
import com.fbi.fbo.impl.dataexport.QueryRow;

import javax.swing.table.DefaultTableCellRenderer;

import com.fbi.commerce.shipping.util.rowdata.CarrierRate;
import java.util.List;
import java.util.Map;

import com.fbi.commerce.shipping.util.ShippingPluginProperty;
import com.fbi.gui.table.FBTable;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import com.jidesoft.swing.JideTabbedPane;
import com.fbi.commerce.shipping.util.rowdata.CarrierRowData;
import com.fbi.gui.table.FBTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

public class RatesPanel extends JPanel
{
    private static final String NO_RATES_MESSAGE = "No rates available for this carrier";
    private FBTableModel<CarrierRowData> tblFedExModel;
    private FBTableModel<CarrierRowData> tblUspsModel;
    private FBTableModel<CarrierRowData> tblUpsModel;
    private ShipRatesDialog shipRatesDialog;
    private String cartonName;
    private JideTabbedPane tbPnlRates;
    private JPanel pnlFedExRates;
    private JLabel lblFedExNoRates;
    private JScrollPane scrFedex;
    private FBTable tblFedExRates;
    private JPanel pnlUspsRates;
    private JLabel lblUspsNoRates;
    private JScrollPane scrUsps;
    private FBTable tblUspsRates;

    private JPanel pnlUpsRates;
    private JLabel lblUpsNoRates;
    private JScrollPane scrUps;
    private FBTable tblUpsRates;

    //freight tables
    private JPanel pnlFLIRates;
    private JLabel lblFLINoRates;
    private JScrollPane scrFLI;
    private FBTable tblFLIRates;
    private FBTableModel<CarrierRowData> tblFLIModel;
    private int totalCubes;

    private static final Logger LOGGER;

    RatesPanel() {
        this.initComponents();
    }
    
    RatesPanel(final ShippingPluginProperty shippingPluginProperty, final ShipRatesDialog shipRatesDialog, final String cartonName) {
        this();
        this.cartonName = cartonName;
        this.shipRatesDialog = shipRatesDialog;
        if (!shippingPluginProperty.isUseFedEx()) {
            this.tbPnlRates.remove((Component)this.pnlFedExRates);
        }
        if (!shippingPluginProperty.isUseUsps()) {
            this.tbPnlRates.remove((Component)this.pnlUspsRates);
        }
        if (!shippingPluginProperty.isUseUps()) {
            this.tbPnlRates.remove((Component)this.pnlUpsRates);
        }

        //remove FLI panel if necessary
    }
    
    void populateTables(final Map<String, List<CarrierRate>> carrierRateMap, final int packListSize, QueryRow orderSize) {
        //LOGGER.error("Populating tables");
        this.tblFedExModel = this.populateTable(this.tblFedExRates, this.lblFedExNoRates, this.scrFedex, carrierRateMap.get("FedEx"), packListSize,orderSize);
        this.tblUspsModel = this.populateTable(this.tblUspsRates, this.lblUspsNoRates, this.scrUsps, carrierRateMap.get("USPS"), packListSize,orderSize);
        this.tblUpsModel = this.populateTable(this.tblUpsRates, this.lblUpsNoRates, this.scrUps, carrierRateMap.get("UPS"), packListSize,orderSize);

        this.tblFLIModel = this.populateTable(this.tblFLIRates, this.lblFLINoRates, this.scrFLI, carrierRateMap.get("FLI"),packListSize,orderSize);
    }

    private FBTableModel<CarrierRowData> populateTable(FBTable table, JLabel lblNoRates, JScrollPane scrollPane, List<CarrierRate> rates, int packageListSize, QueryRow orderSize) {

        if (rates != null && rates.size() != 0) {
            //LOGGER.error("rate list is full");
            //LOGGER.error("Rate List size: " + rates.size());
            lblNoRates.setVisible(false);
            scrollPane.setVisible(true);

//            try {
                FBTableModel<CarrierRowData> model = new FBTableModel(table, CarrierRowData.getSettings());

                table.getColumnModel().getColumn(0).setPreferredWidth(CarrierRowData.colCarrier.getColDefWidth());
                table.getColumnModel().getColumn(1).setPreferredWidth(CarrierRowData.colService.getColDefWidth());
                table.getColumnModel().getColumn(2).setPreferredWidth(CarrierRowData.colDuration.getColDefWidth());
                table.getColumnModel().getColumn(3).setPreferredWidth(CarrierRowData.colRate.getColDefWidth());

                table.getColumnModel().getColumn(4).setPreferredWidth(CarrierRowData.colRateMarkup.getColDefWidth());

                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(0);
                table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
                table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
                table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
                //table.setTableHeader(new JTableHeader());

                rates.forEach((carrierRate) -> {
//                    LOGGER.error("CarrierRate: " + carrierRate.getCarrier().toString());
//                    LOGGER.error("CarrierRate: " + carrierRate.getAmount().toString());
//                    LOGGER.error("CarrierRate: " + carrierRate.getDays().toString());
//                    LOGGER.error("CarrierRate: " + carrierRate.getShipService().toString());
//                    LOGGER.error("CarrierRate: " + carrierRate.getToken().toString());
//                    LOGGER.error("CarrierRate: " + carrierRate.getDurationTerms().toString());
                    model.addRow(new CarrierRowData(carrierRate));
                });


//                LOGGER.error("TableModel Size: " + model.getTableData().size());


                table.sortColumn(3);
                LOGGER.error("Model built");
                return model;

//            }
//            catch (Exception e){
//                LOGGER.error(e.getMessage());
//            }
//            return null;

        } else {
            //TODO: add in FLI check, rates will be empty if the total cubes dont meet minimum size

            LOGGER.error("rate list is empty");
            if ("scrUsps".equals(scrollPane.getName()) && packageListSize > 1) {
                lblNoRates.setText("USPS rates cannot be displayed for multiple " + this.cartonName + "s");
            }
            else if ("scrFLI".equals(scrollPane.getName())){
                //no fli rates, just show default message
                lblNoRates.setText("Order cuft = " + orderSize.getInt("totalOrderCubes")/1728 + ", Freight Threshold = 64 cuft");
            }
            else {
                lblNoRates.setText("No rates available for this carrier");
            }

            lblNoRates.setVisible(true);
            scrollPane.setVisible(false);
            return null;
        }
    }
    
    private void createUIComponents() {
        this.tblFedExRates = this.createTable(() -> this.tblFedExModel);
        this.tblUspsRates = this.createTable(() -> this.tblUspsModel);
        this.tblUpsRates = this.createTable(() -> this.tblUpsModel);

        this.tblFLIRates = this.createTable(() -> this.tblFLIModel);
    }
    
    private FBTable createTable(final Supplier<FBTableModel<CarrierRowData>> model) {
        final FBTable table = new FBTable();
        table.setFont(new Font("Tahoma", 0, 14));
        table.setRowHeight(45);
        table.addMouseListener((MouseClickListener)e -> {
            //LOGGER.error("Column Clicked: " + table.getColumnModel().getColumnIndex(CarrierRowData.colAction.getColName()));
            if (table.getSelectedColumn() == table.getColumnModel().getColumnIndex(CarrierRowData.colAction.getColName())) {
                //TODO: for freight make sure carriers exist in FB first
                this.shipRatesDialog.addShippingLineToSo(((CarrierRowData) model.get().getSelectedRowData()).getData());
            }
            return;
        });
        return table;
    }
    
    private void initComponents() {
        this.createUIComponents();
        this.tbPnlRates = new JideTabbedPane();
        this.pnlFedExRates = new JPanel();
        this.lblFedExNoRates = new JLabel();
        this.scrFedex = new JScrollPane();
        this.pnlUspsRates = new JPanel();
        this.lblUspsNoRates = new JLabel();
        this.scrUsps = new JScrollPane();
        this.pnlUpsRates = new JPanel();
        this.lblUpsNoRates = new JLabel();
        this.scrUps = new JScrollPane();
//freight
        this.pnlFLIRates = new JPanel();
        this.lblFLINoRates = new JLabel();
        this.scrFLI = new JScrollPane();


        this.setName("this");
        this.setLayout(new GridBagLayout());
        ((GridBagLayout)this.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout)this.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.getLayout()).rowWeights = new double[] { 1.0, 1.0E-4 };
        this.tbPnlRates.setFocusable(false);
        this.tbPnlRates.setRequestFocusEnabled(false);
        this.tbPnlRates.setVerifyInputWhenFocusTarget(false);
        this.tbPnlRates.setTabShape(10);
        this.tbPnlRates.setBoldActiveTab(true);
        this.tbPnlRates.setMinimumSize(new Dimension(502, 330));
        this.tbPnlRates.setPreferredSize(new Dimension(502, 330));
        this.tbPnlRates.setMaximumSize(new Dimension(502, 330));
        this.tbPnlRates.setName("tbPnlRates");

        this.pnlFedExRates.setName("pnlFedExRates");
        this.pnlFedExRates.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlFedExRates.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlFedExRates.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlFedExRates.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlFedExRates.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.lblFedExNoRates.setText("No rates available for this carrier");
        this.lblFedExNoRates.setFont(new Font("Segoe UI", 0, 16));
        this.lblFedExNoRates.setName("lblFedExNoRates");
        this.pnlFedExRates.add(this.lblFedExNoRates, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 3, new Insets(20, 0, 5, 0), 0, 0));
        this.scrFedex.setName("scrFedex");
        this.tblFedExRates.setName("tblFedExRates");
        this.scrFedex.setViewportView((Component)this.tblFedExRates);
        this.pnlFedExRates.add(this.scrFedex, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.tbPnlRates.addTab("FedEx", (Icon)new ImageIcon(this.getClass().getResource("/images/FedEx.png")), (Component)this.pnlFedExRates);


        this.pnlUspsRates.setName("pnlUspsRates");
        this.pnlUspsRates.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlUspsRates.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlUspsRates.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlUspsRates.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlUspsRates.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.lblUspsNoRates.setText("No rates available for this carrier");
        this.lblUspsNoRates.setFont(new Font("Segoe UI", 0, 16));
        this.lblUspsNoRates.setName("lblUspsNoRates");
        this.pnlUspsRates.add(this.lblUspsNoRates, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 3, new Insets(20, 0, 5, 0), 0, 0));
        this.scrUsps.setName("scrUsps");
        this.tblUspsRates.setName("tblUspsRates");
        this.scrUsps.setViewportView((Component)this.tblUspsRates);
        this.pnlUspsRates.add(this.scrUsps, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.tbPnlRates.addTab("USPS", (Icon)new ImageIcon(this.getClass().getResource("/images/usps2.png")), (Component)this.pnlUspsRates);
        this.pnlUpsRates.setName("pnlUpsRates");
        this.pnlUpsRates.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlUpsRates.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlUpsRates.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlUpsRates.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlUpsRates.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.lblUpsNoRates.setText("No rates available for this carrier");
        this.lblUpsNoRates.setFont(new Font("Segoe UI", 0, 16));
        this.lblUpsNoRates.setName("lblUpsNoRates");
        this.pnlUpsRates.add(this.lblUpsNoRates, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 3, new Insets(20, 0, 5, 0), 0, 0));
        this.scrUps.setName("scrUps");
        this.tblUpsRates.setName("tblUpsRates");
        this.scrUps.setViewportView((Component)this.tblUpsRates);
        this.pnlUpsRates.add(this.scrUps, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.tbPnlRates.addTab("UPS", (Icon)new ImageIcon(this.getClass().getResource("/images/ups24.png")), (Component)this.pnlUpsRates);

     //Freight
        this.pnlFLIRates.setName("pnlFLIRates");
        this.pnlFLIRates.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlFLIRates.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlFLIRates.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlFLIRates.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlFLIRates.getLayout()).rowWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        this.lblFLINoRates.setText("Shipment doesn't meet freight size");
        this.lblFLINoRates.setFont(new Font("Segoe UI", 0, 16));
        this.lblFLINoRates.setName("lblFLINoRates");
        this.pnlFLIRates.add(this.lblFLINoRates, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 3, new Insets(20, 0, 5, 0), 0, 0));
        this.scrFLI.setName("scrFLI");
        this.tblFLIRates.setName("tblFLIRates");
        this.scrFLI.setViewportView((Component)this.tblFLIRates);
        this.pnlFLIRates.add(this.scrFLI, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));

        this.tbPnlRates.addTab("Freight", (Icon)new ImageIcon(this.getClass().getResource("/images/FLI.png")), (Component)this.pnlFLIRates);




        this.add((Component)this.tbPnlRates, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(2, 20, 1, 20), 0, 0));
    }
    static {
        LOGGER = LoggerFactory.getLogger((Class)RatesPanel.class);
    }
}
