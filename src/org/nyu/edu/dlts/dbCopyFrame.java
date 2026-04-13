/*
 * Created by JFormDesigner on Tue Jul 31 10:12:49 EDT 2012
 */

package org.nyu.edu.dlts;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.nyu.edu.dlts.utils.ASpaceClient;
import org.nyu.edu.dlts.utils.ASpaceCopyUtil;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.nyu.edu.dlts.utils.ArchonClient;
import org.nyu.edu.dlts.utils.uiuc.UIUCPropertiesReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple class to test the database transfer code without starting of the AT client application
 *
 * @author Nathan Stevens
 */
public class dbCopyFrame extends JFrame {

    // stores any migration errors
    private String migrationErrors = "";

    // stores any notable data changes
    private String migrationDataChanges = "";

    // store any mismatch errors
    private String repositoryMismatchErrors = "";

    // the database copy util for AT to archives space
    private ASpaceCopyUtil ascopy;

    // the database copy until for check for repository mismatches
    private ASpaceCopyUtil ascopyREC;

    // used to connect connect to apace backend for testing
    private ASpaceClient aspaceClient;

    private ArchonClient archonClient;

    private boolean copyStopped = false;

    // used to find and attempt to resolve repository mismatch
    private boolean checkRepositoryMismatch = false;
    private HashMap<String, String> repositoryMismatchMap = null;

    // used to specify that the GUI is in basic mode
    private boolean isBasicUI = false;

    // running in standalone mode
    public dbCopyFrame(boolean basic) {
        initComponents();

        // hide this check box for now since we not
        useSaveURIMapsCheckBox.setVisible(false);

        if(basic) {
            hideAdvanceFeatures();
        }
    }

    /**
     * Method to hide advance UI features to make it easier for users to run the tool
     */
    private void hideAdvanceFeatures() {
        tracerPanel.setVisible(false);
        simulateCheckBox.setVisible(false);
        deleteResourcesCheckBox.setVisible(false);
        numResourceToCopyLabel.setVisible(false);
        numResourceToCopyTextField.setVisible(false);
        deleteResourcesCheckBox.setVisible(false);
        recordURIComboBox.setVisible(false);
        paramsLabel.setVisible(false);
        paramsTextField.setVisible(false);
        viewRecordButton.setVisible(false);
        basicUIButton.setVisible(false);

        if (UIUCPropertiesReader.getUIUCProperties() != null) {
            sourceTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("archon.source"));
            archonAdminTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("archon.user"));
            archonPasswordTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("archon.password"));
            hostTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.host"));
            adminTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.admin"));
            adminPasswordTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("aspace.password"));
            collectionPrefixTextField.setText(UIUCPropertiesReader.getUIUCProperties().getProperty("archon.prefix"));
        } else {
            // clear out some defaults used when in development
            sourceTextField.setText("http://localhost/archon");
            hostTextField.setText("http://localhost:8089");
        }
        // clear out some defaults used when in development
//        sourceTextField.setText("http://localhost/archon");
        downloadFolderTextField.setText("");
//        hostTextField.setText("http://localhost:8089");

        isBasicUI = true;
    }

    /**
     * Close this window, and only exit if we are running in standalone mode
     */
    private void okButtonActionPerformed() {
        setVisible(false);
        System.exit(0);
    }

    /**
     * Method to copy data from AR to archive space. NO longer Used
     */
    private void CopyToASpaceButtonActionPerformed() {
        // reset the error count and error messages
        errorCountLabel.setText("N/A");
        migrationErrors = "";

        String sourceSession = getArchonSession();

        if (sourceSession != null) {
            startASpaceCopyProcess(archonClient);
        } else {
            archonClient = null;
            consoleTextArea.setText("Source connection couldn't be established ...");
        }
    }

    /**
     * Get a Session from the Archon backend
     *
     * @return
     */
    private String getArchonSession() {
        String sourceUrl = getArchonSourceUrl();

        // load the source and destinations database connections
        archonClient = new ArchonClient(sourceUrl, archonAdminTextField.getText(), archonPasswordTextField.getText());
        return archonClient.getSession();
    }

    /**
     * Method to start the a thread that actually copied ASpace records
     *
     * @param archonClient
     */
    private void startASpaceCopyProcess(final ArchonClient archonClient) {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                // first disable/enable the relevant buttons
                copyToASpaceButton.setEnabled(false);
                //errorLogButton.setEnabled(false);
                stopButton.setEnabled(true);

                // clear text area and show progress bar
                consoleTextArea.setText("");
                copyProgressBar.setStringPainted(true);
                copyProgressBar.setString("Copying Records ...");
                copyProgressBar.setIndeterminate(true);

                try {
                    // print the connection message
                    consoleTextArea.append(archonClient.getConnectionMessage());

                    String host = hostTextField.getText().trim();
                    String admin = adminTextField.getText();
                    String adminPassword = adminPasswordTextField.getText();
                    boolean simulateRESTCalls = simulateCheckBox.isSelected();
                    String collectionPrefix = collectionPrefixTextField.getText();

                    ascopy = new ASpaceCopyUtil(archonClient, host, admin, adminPassword);
                    ascopy.setSimulateRESTCalls(simulateRESTCalls);
                    String[] testCollectionsArray = {};
                    if (UIUCPropertiesReader.getUIUCProperties() != null && UIUCPropertiesReader.getUIUCProperties().getProperty("testing.collections") != null) {
                        String testCollections =  UIUCPropertiesReader.getUIUCProperties().getProperty("testing.collections");
                        testCollectionsArray = testCollections.split(",");
                    }
                    ArrayList<String> readCollArchonIDsList = new ArrayList<String>();
                    for (String id : testCollectionsArray) {
                        readCollArchonIDsList.add(id);
                    }
                    ascopy.setCollArchonIDToCopyList(readCollArchonIDsList);

                    // set the reset password, and output console and progress bar
                    ascopy.setResetPassword(resetPasswordTextField.getText().trim());
                    ascopy.setOutputConsole(consoleTextArea);
                    ascopy.setProgressIndicators(copyProgressBar, errorCountLabel);
                    ascopy.setCopying(true);

                    // set the base uri for digital objects if downloading the files from Archon
                    if(downloadCheckBox.isSelected()) {
                        ascopy.setDigitalObjectBaseURI(doURLTextField.getText().trim());
                    }

                    //set identifier prefix if indicated in properties file
                    ascopy.setIdentifierPrefix(collectionPrefix);

                    // try getting the session and only continue if a valid session is return;
                    if(!ascopy.getSession()) {
                        consoleTextArea.append("No session, nothing to do ...\n");
                        reEnableCopyButtons();
                        return;
                    } else {
                        consoleTextArea.append("Administrator authenticated ...\n");
                    }

                    // check the current aspace version to make sure
                    String aspaceVersion = ascopy.getASpaceVersion();
                    double aspaceVersionDouble = new Double(aspaceVersion.replaceAll("[^0-9.]", ""));

                    if (!aspaceVersion.isEmpty() && aspaceVersionDouble < 2.1) {
                        String message = "Unsupported Archivesspace Version\nSupport Versions: v2.1 and higher ...\n";

                        consoleTextArea.append(message);
                        reEnableCopyButtons();
                        return;
                    }

                    // now check to make sure we have a lid directory if we want to save the
                    // downloaded digital object files
                    File downloadDirectory = null;

                    if(downloadCheckBox.isSelected()) {
                        downloadDirectory = verifyDownloadDirectory();
                    }

                    // process special options here. This could be done better but its the
                    // quickest way to do it for now
                    String ids = resourcesToCopyTextField.getText().trim();
                    ArrayList<String> collectionsIDsList = new ArrayList<String>();

                    if (!ids.isEmpty()) {
                        String[] sa = ids.split("\\s*,\\s*");
                        for (String id : sa) {
                            // check to see if we are dealing with a special command
                            // or an id to copy
                            if (id.startsWith("-")) {
                                processSpecialOption(ascopy, id);
                            } else {
                                collectionsIDsList.add(id);
                            }
                        }
                    }

                    // set the progress bar from doing it's thing since the ascopy class is going to take over
                    copyProgressBar.setIndeterminate(false);

                    // check to see if we need to set the default repository
                    if (defaultRepositoryCheckBox.isSelected()) {
                        ascopy.setDefaultRepositoryId(defaultRepositoryComboBox.getSelectedItem().toString());
                    }

                    if (!copyStopped) ascopy.copyEnumRecords();
                    if (!copyStopped) ascopy.copyRepositoryRecords();
                    if (!copyStopped) ascopy.mapRepositoryGroups();
                    if (!copyStopped) ascopy.copyUserRecords();
                    if (!copyStopped) ascopy.copySubjectRecords();
                    if (!copyStopped) ascopy.copyCreatorRecords();
                    if (!copyStopped) ascopy.copyClassificationRecords();
                    if (!copyStopped) ascopy.copyAccessionRecords();
                    if (!copyStopped) ascopy.copyDigitalObjectRecords();

                    // get the number of resources to copy here to allow it to be reset while the migration
                    // has been started, but migration of resources has not yet started
                    int collectionsToCopy = 1000000;

                    try {
                        boolean deleteSavedResources = deleteResourcesCheckBox.isSelected();
                        ascopy.setDeleteSavedResources(deleteSavedResources);

                        // get the number of collections/resource to copy
                        collectionsToCopy = Integer.parseInt(numResourceToCopyTextField.getText());
                    } catch (NumberFormatException nfe) { }

                    // check to make sure we didn't stop the copy process or resource to copy is
                    // not set to zero. Setting resources to copy to zero is a convenient way
                    // to generate a URI map which contains no resource records for testing purposes
                    if(!copyStopped && collectionsToCopy != 0) {
                        ascopy.setCollectionsToCopyList(collectionsIDsList);
                        ascopy.copyCollectionRecords(collectionsToCopy);
                    }

                    if (!copyStopped) ascopy.copyDigitalObjectStragglers();

                    // now download the digital object files
                    if(downloadCheckBox.isSelected()) {
                        ascopy.downloadDigitalObjectFiles(downloadDirectory);
                    }

                    ascopy.cleanUp();

                    // set the number of errors and message now
                    String errorCount = "" + ascopy.getASpaceErrorCount();
                    errorCountLabel.setText(errorCount);
                    migrationErrors = ascopy.getSaveErrorMessages() + "\n\nTotal errors/warnings: " + errorCount;
                    migrationDataChanges = ascopy.getChangeLogMessages();
                } catch (Exception e) {
                    consoleTextArea.setText("Unrecoverable exception, migration stopped ...\n\n");

                    if(ascopy != null) {
                        consoleTextArea.append(ascopy.getCurrentRecordInfo() + "\n\n");
                    }

                    consoleTextArea.append(getStackTrace(e));
                }

                reEnableCopyButtons();
            }
        });

        performer.start();
    }

    /**
     * Method to verify that the directory selected for download files those exists and is
     * writerable
     *
     * @return
     */
    private File verifyDownloadDirectory() {
        String filename = downloadFolderTextField.getText();
        File directory = new File(filename);

        if(directory.exists() && directory.isDirectory() && directory.canWrite()) {
            return directory;
        } else {
            JOptionPane.showMessageDialog(this,
                "Please specify a valid directory to\ndownload digital object files to.",
                "Invalid Download Directory",
                JOptionPane.ERROR_MESSAGE);

            return null;
        }
    }

    /**
     * Method to process special commands
     */
    private void processSpecialOption(ASpaceCopyUtil ascopy, String option) {
        if (option.contains("-bbcode_")) {
            ascopy.setBBCodeOption(option);
        } else {
            consoleTextArea.setText("Unknown migration option ...\n");
        }
    }

    /**
     * Method to return the archon host url
     *
     * @return
     */
    private String getArchonSourceUrl() {
        String sourceUrl = sourceTextField.getText();

        // we may be testing the developer system so check
        if(useTracerCheckBox.isSelected()) {
            String devHost = "http://archives-dev.library.illinois.edu/archondev/";
            String archonInstance = (String)tracerComboBox.getSelectedItem();
            sourceUrl = devHost + archonInstance + "/";
        }

        return sourceUrl;
    }

    /**
     * Method to re-enable the copy buttons
     */
    private void reEnableCopyButtons() {
        // re-enable the buttons the relevant buttons
        copyToASpaceButton.setEnabled(true);
        copyProgressBar.setValue(0);

        if (copyStopped) {
            copyStopped = false;
            copyProgressBar.setString("Cancelled Copy Process ...");
        } else {
            copyProgressBar.setString("Done");
        }
    }

    /**
     * Method to display the error log dialog
     */
    private void errorLogButtonActionPerformed() {
        ImportExportLogDialog logDialog;

        if(ascopy != null && ascopy.isCopying()) {
            logDialog = new ImportExportLogDialog(this, ascopy.getCurrentProgressMessage());
            logDialog.setTitle("Current Data Transfer Errors");
        } else {
            logDialog = new ImportExportLogDialog(this, migrationErrors);
            logDialog.setTitle("Data Transfer Errors");
        }

        logDialog.showDialog();
    }

    /**
     * Method to display the change log dialog
     */
    private void changeLogButtonActionPerformed() {
        ImportExportLogDialog logDialog;

        if(ascopy != null && ascopy.isCopying()) {
            logDialog = new ImportExportLogDialog(this, ascopy.getChangeLogMessages());
            logDialog.setTitle("Current Data Transfer Changes");
        } else {
            logDialog = new ImportExportLogDialog(this, migrationDataChanges);
            logDialog.setTitle("Data Transfer Changes");
        }

        logDialog.showDialog();
    }

    /**
     * Method to stop the copy process. Only works when resource are being copied
     */
    private void stopButtonActionPerformed() {
        if(ascopy != null) {
            ascopy.stopCopy();
        }

        if(ascopyREC != null) {
            ascopyREC.stopCopy();
        }

        copyStopped = true;
        stopButton.setEnabled(false);
    }

    /**
     * A convenient method for view the ASpace json records. It meant to be used for development purposes only
     */
    private void viewRecordButtonActionPerformed() {
        String uri = recordURIComboBox.getSelectedItem().toString();
        String recordJSON = "";

        try {
            if(aspaceClient == null) {
                String host = hostTextField.getText().trim();
                String admin = adminTextField.getText();
                String adminPassword = adminPasswordTextField.getText();

                aspaceClient = new ASpaceClient(host, admin, adminPassword);
                aspaceClient.getSession();
            }

            recordJSON = aspaceClient.getRecordAsJSON(uri, paramsTextField.getText());

            if(recordJSON == null || recordJSON.isEmpty()) {
                recordJSON = aspaceClient.getErrorMessages();
            }
        } catch (Exception e) {
            recordJSON = e.toString();
        }


        CodeViewerDialog codeViewerDialog = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, recordJSON, true);
        codeViewerDialog.setTitle("REST ENDPOINT URI: " + uri);
        codeViewerDialog.pack();
        codeViewerDialog.setVisible(true);
    }

    /**
     * Method to display the basic UI.
     */
    private void basicUIButtonActionPerformed() {
        dbCopyFrame basicFrame = new dbCopyFrame(true);
        basicFrame.pack();
        basicFrame.setVisible(true);
    }

    /**
     * Method to get the string from a stack trace
     * @param throwable The exception
     * @return the string representation of the stack trace
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Method to set the directory where the downloaded files should be placed
     */
    private void downloadFolderButtonActionPerformed() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fc.showOpenDialog(dbCopyFrame.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            //This is where a real application would open the file.
            downloadFolderTextField.setText(file.getAbsolutePath());
        }
    }

    /**
     * Method to load repositories from Archon backend
     */
    private void defaultRepositoryCheckBoxActionPerformed() {
        if(!defaultRepositoryCheckBox.isSelected()) {
            return;
        }

        // get the connection url for the
        String sourceUrl = getArchonSourceUrl();

        // load the source and destinations database connections
        archonClient = new ArchonClient(sourceUrl, archonAdminTextField.getText(), archonPasswordTextField.getText());
        String sourceSession = archonClient.getSession();

        if (sourceSession != null) {
            defaultRepositoryComboBox.removeAllItems();
            defaultRepositoryComboBox.addItem("Based On Linked Collection");

            ArrayList<String> repositoryList = archonClient.getRepositoryRecordsList();

            for(String repositoryName: repositoryList) {
                defaultRepositoryComboBox.addItem(repositoryName);
            }
        } else {
            archonClient = null;
            consoleTextArea.setText("Source connection couldn't be established ...");
        }

    }

    /**
     * Method to show the url of the selected tracer database
     *
     * @param e
     */
    private void tracerComboBoxActionPerformed(ActionEvent e) {
        String tracer = tracerComboBox.getSelectedItem().toString();

        if(useTracerCheckBox.isSelected()) {
            String sourceURL = getArchonSourceUrl();
            sourceTextField.setText(sourceURL);
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        apiLabel = new JLabel();
        sourceLabel = new JLabel();
        sourceTextField = new JTextField();
        archonAdminLabel = new JLabel();
        archonAdminTextField = new JTextField();
        archonPasswordLabel = new JLabel();
        archonPasswordTextField = new JTextField();
        downloadCheckBox = new JCheckBox();
        doURLTextField = new JTextField();
        downloadFolderButton = new JButton();
        downloadFolderTextField = new JTextField();
        defaultRepositoryCheckBox = new JCheckBox();
        defaultRepositoryComboBox = new JComboBox();
        copyToASpaceButton = new JButton();
        hostLabel = new JLabel();
        hostTextField = new JTextField();
        tracerPanel = new JPanel();
        useTracerCheckBox = new JCheckBox();
        tracerComboBox = new JComboBox();
        adminLabel = new JLabel();
        adminTextField = new JTextField();
        adminPasswordLabel = new JLabel();
        adminPasswordTextField = new JTextField();
        useSaveURIMapsCheckBox = new JCheckBox();
        resetPassswordLabel = new JLabel();
        resetPasswordTextField = new JTextField();
        simulateCheckBox = new JCheckBox();
        numResourceToCopyLabel = new JLabel();
        numResourceToCopyTextField = new JTextField();
        deleteResourcesCheckBox = new JCheckBox();
        resourcesToCopyLabel = new JLabel();
        resourcesToCopyTextField = new JTextField();
        outputConsoleLabel = new JLabel();
        copyProgressBar = new JProgressBar();
        scrollPane1 = new JScrollPane();
        consoleTextArea = new JTextArea();
        recordURIComboBox = new JComboBox();
        panel1 = new JPanel();
        paramsLabel = new JLabel();
        paramsTextField = new JTextField();
        viewRecordButton = new JButton();
        buttonBar = new JPanel();
        errorLogButton = new JButton();
        changeLogButton = new JButton();
        saveErrorsLabel = new JLabel();
        errorCountLabel = new JLabel();
        stopButton = new JButton();
        basicUIButton = new JButton();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();
        collectionPrefixLabel = new JLabel();
        collectionPrefixTextField = new JTextField();

        //======== this ========
        setTitle("Archon Data Migrator v2.x (11-2017)");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW)
                    },
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- apiLabel ----
                apiLabel.setText("  Archives Space Version: v2.x");
                apiLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                contentPanel.add(apiLabel, cc.xy(1, 1));

                //---- sourceLabel ----
                sourceLabel.setText("Archon Source");
                contentPanel.add(sourceLabel, cc.xywh(3, 1, 2, 1));

                //---- sourceTextField ----
                sourceTextField.setColumns(4);
                sourceTextField.setText("http://localhost/~nathan/archon");
                contentPanel.add(sourceTextField, cc.xywh(5, 1, 7, 1));

                //---- archonAdminLabel ----
                archonAdminLabel.setText("Archon User");
                contentPanel.add(archonAdminLabel, cc.xywh(3, 3, 2, 1));

                //---- archonAdminTextField ----
                archonAdminTextField.setText("sa");
                contentPanel.add(archonAdminTextField, cc.xy(5, 3));

                //---- archonPasswordLabel ----
                archonPasswordLabel.setText("Password");
                contentPanel.add(archonPasswordLabel, cc.xy(9, 3));

                //---- archonPasswordTextField ----
                archonPasswordTextField.setText("admin");
                contentPanel.add(archonPasswordTextField, cc.xy(11, 3));

                //---- downloadCheckBox ----
                downloadCheckBox.setText("Download Digital Object Files");
                contentPanel.add(downloadCheckBox, cc.xy(1, 5));

                //---- doURLTextField ----
                doURLTextField.setText("http://digital_object.base.url/files");
                contentPanel.add(doURLTextField, cc.xywh(3, 5, 9, 1));

                //---- downloadFolderButton ----
                downloadFolderButton.setText("Set Download Folder");
                downloadFolderButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        downloadFolderButtonActionPerformed();
                    }
                });
                contentPanel.add(downloadFolderButton, cc.xy(1, 7));

                //---- downloadFolderTextField ----
                downloadFolderTextField.setText("/Users/nathan/temp/archon_files");
                contentPanel.add(downloadFolderTextField, cc.xywh(3, 7, 9, 1));

                //---- defaultRepositoryCheckBox ----
                defaultRepositoryCheckBox.setText("Set Default Repository");
                defaultRepositoryCheckBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        defaultRepositoryCheckBoxActionPerformed();
                    }
                });
                contentPanel.add(defaultRepositoryCheckBox, cc.xy(1, 9));

                //---- defaultRepositoryComboBox ----
                defaultRepositoryComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "Based On Linked Collection"
                }));
                contentPanel.add(defaultRepositoryComboBox, cc.xywh(3, 9, 9, 1));

                //---- copyToASpaceButton ----
                copyToASpaceButton.setText("Copy To Archives Space");
                copyToASpaceButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        CopyToASpaceButtonActionPerformed();
                    }
                });
                contentPanel.add(copyToASpaceButton, cc.xy(1, 11));

                //---- hostLabel ----
                hostLabel.setText("Host");
                contentPanel.add(hostLabel, cc.xywh(3, 11, 2, 1));

                //---- hostTextField ----
                hostTextField.setText("http://54.227.35.51:8089");
                contentPanel.add(hostTextField, cc.xywh(5, 11, 7, 1));

                //======== tracerPanel ========
                {
                    tracerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- useTracerCheckBox ----
                    useTracerCheckBox.setText("Use Test Archon");
                    tracerPanel.add(useTracerCheckBox);

                    //---- tracerComboBox ----
                    tracerComboBox.setModel(new DefaultComboBoxModel(new String[] {
                        "tracer",
                        "luther",
                        "miami",
                        "nwu",
                        "uiuctest",
                        "ihlc",
                        "ala",
                        "rbml"
                    }));
                    tracerComboBox.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            tracerComboBoxActionPerformed(e);
                        }
                    });
                    tracerPanel.add(tracerComboBox);
                }
                contentPanel.add(tracerPanel, cc.xy(1, 13));

                //---- adminLabel ----
                adminLabel.setText("ASpace admin");
                contentPanel.add(adminLabel, cc.xy(3, 13));

                //---- adminTextField ----
                adminTextField.setText("admin");
                contentPanel.add(adminTextField, cc.xy(5, 13));

                //---- adminPasswordLabel ----
                adminPasswordLabel.setText("Password");
                contentPanel.add(adminPasswordLabel, cc.xy(9, 13));

                //---- adminPasswordTextField ----
                adminPasswordTextField.setText("admin");
                contentPanel.add(adminPasswordTextField, cc.xy(11, 13));

                //---- useSaveURIMapsCheckBox ----
                useSaveURIMapsCheckBox.setText("Continue From Resource Records");
                contentPanel.add(useSaveURIMapsCheckBox, cc.xy(1, 15));

                //---- resetPassswordLabel ----
                resetPassswordLabel.setText("Reset Password");
                contentPanel.add(resetPassswordLabel, cc.xy(3, 15));

                //---- resetPasswordTextField ----
                resetPasswordTextField.setText("archive");
                contentPanel.add(resetPasswordTextField, cc.xy(5, 15));

                //---- simulateCheckBox ----
                simulateCheckBox.setText("Simulate REST Calls");
                contentPanel.add(simulateCheckBox, cc.xy(1, 17));

                //---- numResourceToCopyLabel ----
                numResourceToCopyLabel.setText("Resources To Copy");
                contentPanel.add(numResourceToCopyLabel, cc.xywh(3, 17, 3, 1));

                //---- numResourceToCopyTextField ----
                numResourceToCopyTextField.setText("100000");
                contentPanel.add(numResourceToCopyTextField, cc.xy(5, 17));

                //---- deleteResourcesCheckBox ----
                deleteResourcesCheckBox.setText("Delete Previously Saved Resources");
                contentPanel.add(deleteResourcesCheckBox, cc.xy(1, 19));

                //---- collectionPrefixLabel ----
                collectionPrefixLabel.setText("Collection Prefix");
                contentPanel.add(collectionPrefixLabel, cc.xy(3, 19));

                //---- collectionPrefixTextField ----
                collectionPrefixTextField.setText("prefix here");
                contentPanel.add(collectionPrefixTextField, cc.xy(5, 19));

                //---- resourcesToCopyLabel ----
                resourcesToCopyLabel.setText("Migration Options");
                contentPanel.add(resourcesToCopyLabel, cc.xy(3, 21));

                // //---- resourcesToCopyTextField ----
                resourcesToCopyTextField.setText("-bbcode_html");
                resourcesToCopyTextField.setColumns(40);
                contentPanel.add(resourcesToCopyTextField, cc.xywh(5, 21, 7, 1));

                //---- outputConsoleLabel ----
                outputConsoleLabel.setText("Output Console:");
                contentPanel.add(outputConsoleLabel, cc.xy(1, 23));
                contentPanel.add(copyProgressBar, cc.xywh(3, 23, 9, 1));

                //======== scrollPane1 ========
                {

                    //---- consoleTextArea ----
                    consoleTextArea.setRows(12);
                    scrollPane1.setViewportView(consoleTextArea);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 25, 11, 1));

                //---- recordURIComboBox ----
                recordURIComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "/repositories",
                    "/users",
                    "/subjects",
                    "/agents/families/1",
                    "/agents/people/1",
                    "/agents/corporate_entities/1",
                    "/repositories/2/accessions/1",
                    "/repositories/2/resources/1",
                    "/repositories/2/archival_objects/1",
                    "/config/enumerations"
                }));
                recordURIComboBox.setEditable(true);
                contentPanel.add(recordURIComboBox, cc.xy(1, 27));

                //======== panel1 ========
                {
                    panel1.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- paramsLabel ----
                    paramsLabel.setText("Params");
                    panel1.add(paramsLabel);

                    //---- paramsTextField ----
                    paramsTextField.setColumns(20);
                    paramsTextField.setText("page=1");
                    panel1.add(paramsTextField);

                    //---- viewRecordButton ----
                    viewRecordButton.setText("View");
                    viewRecordButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            viewRecordButtonActionPerformed();
                        }
                    });
                    panel1.add(viewRecordButton);
                }
                contentPanel.add(panel1, cc.xywh(3, 27, 9, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.GLUE_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.BUTTON_COLSPEC
                    },
                    RowSpec.decodeSpecs("pref")));

                //---- errorLogButton ----
                errorLogButton.setText("View Error Log");
                errorLogButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        errorLogButtonActionPerformed();
                    }
                });
                buttonBar.add(errorLogButton, cc.xy(2, 1));

                //---- saveErrorsLabel ----
                saveErrorsLabel.setText(" Errors/Warnings: ");
                buttonBar.add(saveErrorsLabel, cc.xy(4, 1));

                //---- errorCountLabel ----
                errorCountLabel.setText("N/A ");
                errorCountLabel.setForeground(Color.red);
                errorCountLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                buttonBar.add(errorCountLabel, cc.xy(6, 1));

                //---- changeLogButton ----
                changeLogButton.setText("View Change Log");
                changeLogButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        changeLogButtonActionPerformed();
                    }
                });
                buttonBar.add(changeLogButton, cc.xy(7, 1));

                //---- stopButton ----
                stopButton.setText("Cancel Copy");
                stopButton.setEnabled(false);
                stopButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed();
                        stopButtonActionPerformed();
                    }
                });
                buttonBar.add(stopButton, cc.xy(9, 1));

                //---- basicUIButton ----
                basicUIButton.setText("Basic UI");
                basicUIButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        basicUIButtonActionPerformed();
                    }
                });
                buttonBar.add(basicUIButton, cc.xy(10, 1));

                //---- okButton ----
                okButton.setText("Close");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, cc.xy(12, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel apiLabel;
    private JLabel sourceLabel;
    private JTextField sourceTextField;
    private JLabel archonAdminLabel;
    private JTextField archonAdminTextField;
    private JLabel archonPasswordLabel;
    private JTextField archonPasswordTextField;
    private JCheckBox downloadCheckBox;
    private JTextField doURLTextField;
    private JButton downloadFolderButton;
    private JTextField downloadFolderTextField;
    private JCheckBox defaultRepositoryCheckBox;
    private JComboBox defaultRepositoryComboBox;
    private JButton copyToASpaceButton;
    private JLabel hostLabel;
    private JTextField hostTextField;
    private JPanel tracerPanel;
    private JCheckBox useTracerCheckBox;
    private JComboBox tracerComboBox;
    private JLabel adminLabel;
    private JTextField adminTextField;
    private JLabel adminPasswordLabel;
    private JTextField adminPasswordTextField;
    private JCheckBox useSaveURIMapsCheckBox;
    private JLabel resetPassswordLabel;
    private JTextField resetPasswordTextField;
    private JCheckBox simulateCheckBox;
    private JLabel numResourceToCopyLabel;
    private JTextField numResourceToCopyTextField;
    private JCheckBox deleteResourcesCheckBox;
    private JLabel resourcesToCopyLabel;
    private JTextField resourcesToCopyTextField;
    private JLabel outputConsoleLabel;
    private JProgressBar copyProgressBar;
    private JScrollPane scrollPane1;
    private JTextArea consoleTextArea;
    private JComboBox recordURIComboBox;
    private JPanel panel1;
    private JLabel paramsLabel;
    private JTextField paramsTextField;
    private JButton viewRecordButton;
    private JPanel buttonBar;
    private JButton errorLogButton;
    private JButton changeLogButton;
    private JLabel saveErrorsLabel;
    private JLabel errorCountLabel;
    private JButton stopButton;
    private JButton basicUIButton;
    private JButton okButton;
    private JLabel collectionPrefixLabel;
    private JTextField collectionPrefixTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Main method for testing in stand alone mode
     */
    public static void main(String[] args) {
        dbCopyFrame frame;
        if(args.length != 0 && args[0].equals("advance")) {
            frame = new dbCopyFrame(false);
        } else {
            frame = new dbCopyFrame(true);
        }

        frame.pack();
        frame.setVisible(true);
    }
}
