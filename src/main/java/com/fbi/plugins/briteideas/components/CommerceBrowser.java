// 
// Decompiled by Procyon v0.5.30
// 

package com.fbi.plugins.briteideas.components;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.Component;
import javax.swing.JDialog;
import com.fbi.gui.util.UtilGui;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import com.teamdev.jxbrowser.chromium.PopupContainer;
import com.teamdev.jxbrowser.chromium.PopupParams;
import com.teamdev.jxbrowser.chromium.PopupHandler;
import com.teamdev.jxbrowser.chromium.BrowserType;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.Browser;

public final class CommerceBrowser extends Browser
{
    public CommerceBrowser() {
        this(new BrowserContext(new BrowserContextParams(BrowserContext.defaultContext().getCacheDir())));
    }

    public CommerceBrowser(BrowserContext browserContext) {
        super(BrowserType.LIGHTWEIGHT, browserContext);
        Runtime.getRuntime().addShutdownHook(new Thread(this::dispose));
        this.setPopupHandler(new CommerceBrowser.CommercePopupHandler());
    }
    
    private final class CommercePopupHandler implements PopupHandler
    {
        public PopupContainer handlePopup(final PopupParams popupParams) {
            return (PopupContainer)new CommercePopupContainer();
        }
    }
    
    private final class CommercePopupContainer implements PopupContainer
    {
        public void insertBrowser(final Browser browser, final Rectangle rectangle) {
            SwingUtilities.invokeLater(new ShowPopupDialog(browser, rectangle));
        }
    }
    
    private final class ShowPopupDialog implements Runnable
    {
        private final Browser browser;
        private final Rectangle rectangle;
        
        ShowPopupDialog(final Browser browser, final Rectangle rectangle) {
            this.browser = browser;
            this.rectangle = rectangle;
        }
        
        @Override
        public void run() {
            final BrowserView browserView = new BrowserView(this.browser);
            browserView.setPreferredSize(this.rectangle.getSize());
            final JDialog dialog = new JDialog(UtilGui.getParentFrame());
            dialog.setDefaultCloseOperation(2);
            dialog.add((Component)browserView, "Center");
            dialog.pack();
            dialog.setVisible(true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent e) {
                    ShowPopupDialog.this.browser.dispose();
                }
            });
            this.browser.addDisposeListener(event -> dialog.setVisible(false));
        }
    }
}
