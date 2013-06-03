package edu.jhu.hltcoe.ilp.decomp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.ilp.IlpSolver;
import edu.jhu.hltcoe.util.Command;
import edu.jhu.hltcoe.util.Files;

/**
 * TODO: add handling of initial solution
 * 
 * TODO: use numThreads and workMemMegs. Check log (with high log level to
 * see if the parameters are listed there)
 *
 * @author mgormley
 */
public abstract class AbstractDipSolver implements IlpSolver {

    private static final Logger log = Logger.getLogger(AbstractDipSolver.class);

    protected static final String dipDir;
    static {
        File dipDir1 = new File("/Users/mgormley/installed/Dip-0.83.0/Dip");
        File dipDir2 = new File("/home/hltcoe/mgormley/installed/Dip-0.83.0/build/Dip");
        if (dipDir1.exists()) {
            dipDir = dipDir1.getAbsolutePath();
        } else if (dipDir2.exists()) {
            dipDir = dipDir2.getAbsolutePath();
        } else {
            dipDir = "";
        }
    }
    private static final String PARAM_TEMPLATE = "/edu/jhu/hltcoe/ilp/decomp/milpblock.parm";
    private static final String paramTemplate = readParamTemplateFromResource(PARAM_TEMPLATE);

    private static final Pattern spaceRegex = Pattern.compile("\\s+");
    private static final Pattern dipVarRegex = Pattern.compile("\\d+");

    private File tempDir;
    private Map<String, Double> result;
    private int numThreads;
    private double workMemMegs;
    private double objective;
    private BlockFileWriter bfw;
    private File tblFile;
    private int doCut;
    private int doPriceAndCut;

    public AbstractDipSolver(File tempDir, int numThreads, double workMemMegs, BlockFileWriter bfw, int doCut, int doPriceAndCut) {
        this.tempDir = tempDir;
        this.numThreads = numThreads;
        this.workMemMegs = workMemMegs;
        this.bfw = bfw;
        this.doCut = doCut;
        this.doPriceAndCut = doPriceAndCut;
    }

    @Override
    public boolean solve(File mpsFile) {
        if (!mpsFile.getPath().endsWith("." + getType())) {
            throw new IllegalStateException("Expecting mpsFile to end with .mps: " + mpsFile.getPath());
        }
        try {
            result = null;
            objective = 0;

            // Create the .block file
            File blockFile = new File(tempDir, mpsFile.getName().replace(".mps", ".block"));
            bfw.writeBlockFile(blockFile, mpsFile, tblFile);

            // Create the param file
            File paramFile = new File(tempDir, mpsFile.getName().replace(".mps", ".parm"));
            String paramStr = paramTemplate;
            paramStr = paramStr.replace("SECTION_HERE", getSectionHeader());
            paramStr = paramStr.replace("DATA_DIR_HERE", tempDir.getAbsolutePath());
            paramStr = paramStr.replace("DATA_DIR_HERE", tempDir.getAbsolutePath());
            paramStr = paramStr.replace("BLOCK_FILE_HERE", blockFile.getName());
            paramStr = paramStr.replace("MPS_PREFIX_HERE", mpsFile.getName().replace(".mps", ""));
            paramStr = paramStr.replace("LOG_LEVEL_HERE", "5");
            paramStr = paramStr.replace("DO_CUT_HERE", String.valueOf(doCut));
            paramStr = paramStr.replace("DO_PRICE_CUT_HERE", String.valueOf(doPriceAndCut));
            paramStr = paramStr.replace("ZIMPL_TBL_FILE_HERE", tblFile.getAbsolutePath());
            FileWriter paramWriter = new FileWriter(paramFile);
            paramWriter.write(paramStr);
            paramWriter.close();

            // Run DIP's MILPBlock solver
            File solFile = new File(tempDir, mpsFile.getName().replace(".mps", ".sol"));
            String[] cmdArray = new String[] { getDipBinary(), "--param", paramFile.getAbsolutePath() };
            // TODO: handle infeasible case
            File milpBlockLog = new File(tempDir, "dipSolver.log");
            Command.runCommand(cmdArray, milpBlockLog, tempDir);

            // Throw exception if optimal solution not found
            if (!Files.fileContains(milpBlockLog, "Optimal Solution")) {
                return false;
            }

            // Read .sol file for result map and objective
            readDipSol(solFile, mpsFile);

            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getSectionHeader();

    protected abstract String getDipBinary();

    private void printColumns(File mpsFile) throws FileNotFoundException, IOException {
        ArrayList<String> columns = readColumns(mpsFile);
        for (int i=0; i<columns.size(); i++) {
            log.debug(String.format("x(%d) -> %s", i, columns.get(i)));
        }
    }

    @Override
    public double getObjective() {
        return objective;
    }

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    public String getType() {
        return "mps";
    }

    public void setTblFile(File tblFile) {
        this.tblFile = tblFile;
    }

    private void readDipSol(File solFile, File mpsFile) throws IOException {
        ArrayList<String> columns = readColumns(mpsFile);

        // Read in the column values from the solution file
        result = new HashMap<String, Double>();
        
        String line;
        BufferedReader solReader = new BufferedReader(new FileReader(solFile));
        while ((line = solReader.readLine()) != null) {
            String[] splits = spaceRegex.split(line);
            String var = splits[0];
            Double value = Double.valueOf(splits[1]);
            if (var.equals("=obj=")) {
                // HACK: there should be some way to figure out that this is
                // negated
                objective = -value;
            } else {
                // Map the column number to the zimpl variable name
                Matcher matcher = dipVarRegex.matcher(var);
                if (matcher.find()) {
                    String colNumStr = matcher.group(0);
                    int colNum = Integer.parseInt(colNumStr);
                    var = columns.get(colNum);
                    result.put(var, value);
                } else {
                    solReader.close();
                    throw new IllegalStateException("No match fround" + var);
                }
            }
        }
        solReader.close();
    }

    private ArrayList<String> readColumns(File mpsFile) throws FileNotFoundException, IOException {
        String line;
        // Read in the mps file to create a mapping from columns to variables
        BufferedReader mpsReader = new BufferedReader(new FileReader(mpsFile));

        // Read COLUMNS in order into an ArrayList
        ArrayList<String> columns = new ArrayList<String>();
        Files.readUntil(mpsReader, "COLUMNS");
        while ((line = mpsReader.readLine()) != null) {
            if (line.contains("'MARKER'")) {
                // Skip header rows
                // " MARK0000  'MARKER'                 'INTORG'"
                continue;
            }
            if (line.equals("RHS")) {
                break;
            }
            String[] lineSplits = spaceRegex.split(line);
            String var = lineSplits[1];
            if (columns.size() > 0 && columns.get(columns.size() - 1).equals(var)) {
                continue;
            }
            columns.add(var);
        }
        return columns;
    }

    private static String readParamTemplateFromResource(String resourceName) {
        InputStream inputStream = DipMilpBlockSolver.class.getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new RuntimeException("Unable to find resource: " + resourceName);
        }
        try {
            return readParamTemplateFromInputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readParamTemplateFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        reader.close();
        return sb.toString();
    }

}
