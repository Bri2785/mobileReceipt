package com.fbi.plugins.briteideas.panels;

import com.fbi.gui.combobox.FBConstComboBox;
import com.fbi.gui.textfield.FBTextField;
import com.fbi.plugins.briteideas.data.MobileReceipt;

import com.fbi.plugins.briteideas.repository.MRRepository;
import com.fbi.plugins.briteideas.util.rowdata.MobileReceiptSearchRowData;
import com.fbi.sdk.constants.SearchStatusConst;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class MobileReceiptSearchPanel extends JPanel{

    private BIPnlPagedSearch<MobileReceiptSearchRowData, MobileReceipt> mobileReceiptSearchResults;

    private JPanel pnlSearch; //hold our paged search panel
    private JPanel pnlCriteria; //holds the criteria fields
    private JLabel descriptionLabel; //the description label
    private FBTextField descriptionSearchTextField; //the description to search for
    private JLabel statusLabel;
    private FBConstComboBox<SearchStatusConst> cboStatus; //TODO: change to our status list later
    private JPanel pnlButtons;
    private JPanel hSpacer1;
    private JButton searchButton;
    private MRRepository MRRepository;

    public MobileReceiptSearchPanel(MRRepository MRRepository){
        this.initComponents();
        this.MRRepository = MRRepository;
    }

    public void init(){
        this.mobileReceiptSearchResults.init(MobileReceiptSearchRowData.getSettings(), "id", this.MRRepository);

        this.cboStatus.init(SearchStatusConst.values());
        this.cboStatus.setSelectedID(SearchStatusConst.ACTIVE.getId());
    }

    public void executeSearch(){
        this.mobileReceiptSearchResults.executeSearch();
    }


    public BIPnlPagedSearch<MobileReceiptSearchRowData, MobileReceipt> getMobileReceiptSearchResults(){
        return this.mobileReceiptSearchResults;
    }


    private void descriptionSearchTextFieldKeyPressed(final KeyEvent e) {
        if (e.getKeyCode() == 10) {
            this.executeSearch();
        }
    }
    private void searchButtonActionPerformed() {
        this.executeSearch();
    }
    private void cboStatusStateChanged() {
        this.executeSearch();
    }

    private void initComponents() {

        this.pnlSearch = new JPanel();
        this.pnlCriteria = new JPanel();
        this.descriptionLabel = new JLabel();
        this.descriptionSearchTextField = new FBTextField();
        this.statusLabel = new JLabel();
        this.cboStatus = (FBConstComboBox<SearchStatusConst>)new FBConstComboBox();
        this.pnlButtons = new JPanel();
        this.hSpacer1 = new JPanel(null);
        this.searchButton = new JButton();
        this.mobileReceiptSearchResults = new BIPnlPagedSearch<MobileReceiptSearchRowData, MobileReceipt>() {

            protected ArrayList<MobileReceiptSearchRowData> getRowData(final ArrayList<MobileReceipt> dataList) {

                final ArrayList<MobileReceiptSearchRowData> list = new ArrayList<MobileReceiptSearchRowData>();
                for (final MobileReceipt data : dataList) {

                    final MobileReceiptSearchRowData row = new MobileReceiptSearchRowData(data);

                    list.add(row);
                }
                return list;
            }
        };
        this.setName("this");
        this.setLayout(new BorderLayout());
        this.pnlSearch.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.pnlSearch.setName("pnlSearch");
        this.pnlSearch.setLayout(new BorderLayout(5, 5));
        this.pnlCriteria.setName("MobileReceiptSearchCriteraPanel");
        this.pnlCriteria.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlCriteria.getLayout()).columnWidths = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlCriteria.getLayout()).rowHeights = new int[] { 0, 0, 0, 0 };
        ((GridBagLayout)this.pnlCriteria.getLayout()).columnWeights = new double[] { 0.0, 1.0, 1.0E-4 };
        ((GridBagLayout)this.pnlCriteria.getLayout()).rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0E-4 };
        this.descriptionLabel.setText("Description:  ");
        this.descriptionLabel.setName("nameLabel");
        this.pnlCriteria.add(this.descriptionLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 5), 0, 0));
        this.descriptionSearchTextField.setMaxCharacters(31);
        this.descriptionSearchTextField.setHorizontalAlignment(2);
        this.descriptionSearchTextField.setName("MobileReceiptSearchNameTextField");
        this.descriptionSearchTextField.addKeyListener((KeyListener)new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                MobileReceiptSearchPanel.this.descriptionSearchTextFieldKeyPressed(e);
            }
        });
        this.pnlCriteria.add((Component)this.descriptionSearchTextField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 5, 0), 0, 0));

        this.statusLabel.setText("Status:");
        this.statusLabel.setName("statusLabel");
        this.pnlCriteria.add(this.statusLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.cboStatus.setName("MobilereceiptSearchStatusComboBox");
        this.cboStatus.addChangeListener((ChangeListener)new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                MobileReceiptSearchPanel.this.cboStatusStateChanged();
            }
        });
        this.pnlCriteria.add((Component)this.cboStatus, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.pnlSearch.add(this.pnlCriteria, "Center");
        this.pnlButtons.setName("pnlButtons");
        this.pnlButtons.setLayout(new GridBagLayout());
        ((GridBagLayout)this.pnlButtons.getLayout()).columnWidths = new int[] { 0, 0, 0 };
        ((GridBagLayout)this.pnlButtons.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout)this.pnlButtons.getLayout()).columnWeights = new double[] { 1.0, 0.0, 1.0E-4 };
        ((GridBagLayout)this.pnlButtons.getLayout()).rowWeights = new double[] { 0.0, 1.0E-4 };
        this.hSpacer1.setName("hSpacer1");
        this.pnlButtons.add(this.hSpacer1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 5), 0, 0));
        this.searchButton.setMnemonic('r');
        this.searchButton.setText("Search");
        this.searchButton.setToolTipText("Execute search for mobile Receipts.");
        this.searchButton.setMaximumSize(new Dimension(75, 25));
        this.searchButton.setMinimumSize(new Dimension(70, 23));
        this.searchButton.setPreferredSize(new Dimension(75, 23));
        this.searchButton.setName("searchButton");
        this.searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                MobileReceiptSearchPanel.this.searchButtonActionPerformed();
            }
        });
        this.pnlButtons.add(this.searchButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.pnlSearch.add(this.pnlButtons, "South");
        this.add(this.pnlSearch, "North");
        this.mobileReceiptSearchResults.setName("MobileReceiptSearchResultsTablePanel");
        this.add((Component)this.mobileReceiptSearchResults, "Center");
    }


}
