/*
BingoSync JSON File Generator
Version 1.1
Author: Devin

NEW THIS VERSION: Debug welcome message

This program takes a list of new-line-delimited terms and spits out JSON code usable on bingosync.com
It is capable of correctly formatting 3×3, 4×4, & 5×5 Bingo boards to look nice and pretty.
It's ugly as sin because I can't get a jar to export with JFX and CSS doesn't like swing.
I need to learn like, 3 new libraries to get the UI to look as clean as I'd like it.
If you have any questions, reach out to devini15 on Discord.
You should also probably follow me on Twitch, just sayin'.
 */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BingosyncBuilder {

    private static final String VERSION = "1.1"; //If this doesn't match my comment, I'm a clown.

    private static final int DEFAULT_WIDTH = 500; //Width of window
    private static final int DEFAULT_HEIGHT = 510; //Height of window
    private static final int FIVE_BY_FIVE = 25; //5×5 Grid size
    private static final int FOUR_BY_FOUR = 16; //4×4 Grid size
    private static final int THREE_BY_THREE = 9; //3×3 Grid size
    private static final int[] FOUR_GRID = {
             0, 1, 2, 3,
             5, 6, 7, 8,
            10,11,12,13,
            15,16,17,18
    }; //Indexes to populate on a 4×4 Grid
    private static final int[] THREE_GRID = {
             0, 1, 2,
             5, 6, 7,
            10,11,12
    }; //Indexes to populate on a 3×3 Grid

    private static final JCheckBox randomizationBox = new JCheckBox("Randomize cell locations"); //Cells will be in a random order if selected
    private static final JTextArea listArea = new JTextArea(); //Area for entering terms, I don't think you can put hint text in these
    private static final JComboBox<String> dropDown = new JComboBox<>(); //Dropdown menu to select grid size

    private static String[] objectives;

    public static void main(String[] args) {
        //Intro message
        System.out.println("Bingosync " + VERSION + " debug log\n=======================");
        System.out.println("If this command prompt is the only thing that opened, something is broken!");
        JOptionPane.showMessageDialog(null, "Bingosync JSON Generator\nv" + VERSION + "\n\nBy: Devini15");

        //Set up layout of main window and display it
        JFrame mainView;
        mainView = setUpFrame();
        Box vBox = Box.createVerticalBox();
        Box hBox1 = Box.createHorizontalBox();
        Box hBox2 = Box.createHorizontalBox();
        Box hBox3 = Box.createHorizontalBox();
        JLabel instructionLabel = new JLabel("Input each term on it's own line");
        //these 2 lines are literally just for spacing I have no idea how Java decides where to center things
        hBox3.add(instructionLabel);
        hBox3.add(new JLabel("\t\t\t"));
        JButton generateButton = new JButton(onGenerateButtonClicked());
        generateButton.setEnabled(true);
        generateButton.setText("GENERATE");
        generateButton.setBackground(Color.CYAN);
        JButton clearButton = new JButton(onClearButtonClicked());
        clearButton.setEnabled(true);
        clearButton.setText("CLEAR");
        clearButton.setBackground(Color.RED);
        dropDown.addItem("3×3");
        dropDown.addItem("4×4");
        dropDown.addItem("5×5");
        dropDown.setMaximumSize(new Dimension(50,20));
        dropDown.setSelectedIndex(2);
        hBox2.add(dropDown);
        hBox2.add(randomizationBox);
        vBox.add(hBox2);
        vBox.add(hBox3);
        vBox.add(listArea);
        hBox1.add(generateButton);
        hBox1.add(clearButton);
        vBox.add(hBox1);
        mainView.add(vBox);
        mainView.setVisible(true);

    }

    /**
     * Configures basic settings for the main view
     * @return Frame that will contain all UI elements
     */
    private static JFrame setUpFrame(){
        JFrame f = new JFrame();
        f.setTitle("Custom Bingosync Board Generator" + VERSION);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        f.setLocationRelativeTo(null);
        return f;
    }

    /**
     * This is called when the generate button is clicked, I could've put it in the Action handler but there's so much
     * code down there I'm not using.
     * Yes I should have broken this up into like,, 5 smaller methods. Cry about it.
     */
    private static void handleGenerateButton(){
        //Split input into an array of terms
        String[] terms = listArea.getText().split("\n");
        //Set grid to the appropriate size based on dropdown value (2=5×5, 1=4×4, 0=3×3)
        int expectedValue = dropDown.getSelectedIndex() == 2 ?
                FIVE_BY_FIVE : dropDown.getSelectedIndex() == 1 ?
                    FOUR_BY_FOUR : THREE_BY_THREE;
        objectives = new String[expectedValue];
        //Handles cases where user has not input the correct number of terms for the selected grid size
        String errorMessage = ("Expected " + expectedValue + " terms but found " + terms.length);
        if(terms.length != expectedValue) {
            String[] buttons = {"Edit List", "Generate Anyway"};
            int errorResponse = JOptionPane.showOptionDialog(null, errorMessage, "WARNING", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, null);
            //If the user opts to edit the list, we stop processing it here.
            if (errorResponse == 0) {
                return;
            }
        }
        //Populate the objectives array with the terms that will appear on the board.
        //If the user input less than the requisite number of terms, the remaining spaces will be filled with "NULL"
        //If the user input more than the requisite number of terms, the list will be truncated to fit.
        for(int x = 0; x < expectedValue; x++){
            objectives[x] = x < terms.length ? terms[x] : "NULL";
        }
        //Print complete term list for debug reasons
        for(String o : objectives) System.out.println(o);
        //shuffles terms if the randomization box is selected
        if(randomizationBox.isSelected()) {
            List<String> objList = Arrays.asList(objectives);
            Collections.shuffle(objList);
            objList.toArray(objectives);
        }
        //This is the last time I copy everything to a new array, I promise. See fillGrid for details
        String[] grid = fillGrid();

        //Building the output string, the contents of grid are inserted and formatted with JSON
        //This should account for wrong user input, but I imagine the user will find a way to be more wrong than me
        StringBuilder finalOutput = new StringBuilder("[");
        for(String o : grid){
            finalOutput.append("{\"name\":\"").append(o.replaceAll("\"","\\\\\"")).append("\"},");
        }
        finalOutput.setCharAt(finalOutput.lastIndexOf(","), ']');
        //Maybe I should have used a few more methods for this...
        String[] useOptions = {"Copy to Clipboard", "Export as File", "Cancel"};
        //Once output is ready, prompt user to copy to clipboard or export to a file.
        int useCase = JOptionPane.showOptionDialog(null, "JSON text generated", "JSON Done", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, useOptions, null);
        //Clipboard copy
        if(useCase == 0) {
            StringSelection outSelection = new StringSelection(finalOutput.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(outSelection, null);
        }else if(useCase == 1){
            //Save as file. User will need to specify extension as JSON or TXT because I cba to implement forcing it atm
            try {
                JFileChooser outputFileChooser = new JFileChooser();
                FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON files (*.json)");
                outputFileChooser.addChoosableFileFilter(jsonFilter);
                outputFileChooser.setFileFilter(jsonFilter);
                outputFileChooser.showDialog(null, "Save");
                File outFile = outputFileChooser.getSelectedFile();
                FileOutputStream outStream = new FileOutputStream(outFile);
                PrintWriter fileWriter = new PrintWriter(outStream);
                fileWriter.print(finalOutput);
                fileWriter.close();
                outStream.close();
            }catch(IOException e){
                //You fucked up if you get this one
                JOptionPane.showMessageDialog(null, "Unable to save file. Contact developer if assistance is needed", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
        //debug
        System.out.println(finalOutput);
    }

    /**
     * Populates the grid that will be used for the final output
     * @return Grid containing all terms in the correct pattern. Cells with missing terms will say "NULL" and unused
     * cells will contain spaces
     */
    private static String[] fillGrid(){
        //if the grid is 5×5, the objectives array already holds the correct values
        if(dropDown.getSelectedIndex() == 2) return objectives;
        String[] grid = new String[25];
        //Fill the whole thing with spaces so no values are null
        for(int x = 0; x < 25; x++) grid[x] = " ";
        //copy the array of what indexes to fill for the appropriately sized grid
        int[] fillables = dropDown.getSelectedIndex() == 1 ? FOUR_GRID : THREE_GRID;
        //fill in the terms only at indexes contained in the relevant constant array
        for(int x = 0; x < fillables.length; x++) grid[fillables[x]] = objectives[x];
        return grid;
    }
    /*
    Remaining 2 methods have 1 line of code each, if you really can't figure out what they do, you can ask me on
    Discord (devini15)
     */
    private static Action onGenerateButtonClicked(){
        return new Action() {
            @Override
            public Object getValue(String key) {
                return null;
            }

            @Override
            public void putValue(String key, Object value) {

            }

            @Override
            public void setEnabled(boolean b) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void actionPerformed(ActionEvent e) {
               handleGenerateButton();
            }
        };
    }
    private static Action onClearButtonClicked(){
        return new Action() {
            @Override
            public Object getValue(String key) {
                return null;
            }

            @Override
            public void putValue(String key, Object value) {

            }

            @Override
            public void setEnabled(boolean b) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                listArea.setText("");
            }
        };
    }
}
