package edu.jhu.hltcoe.util.cli;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ArgParserTest {

    @Opt(hasArg = true, description = "my intVal")
    public static int intVal = 1;

    @Opt(hasArg = true, description = "my doubleVal")
    public static double doubleVal = 1e10;

    @Opt(hasArg = true, description = "my boolArg")
    public static boolean boolArg = true;

    @Opt(hasArg = false, description = "my boolNoArg")
    public static boolean boolNoArg = false;

    @BeforeClass
    public static void setUp() {
        BasicConfigurator.configure();
    }

    @Test
    public void testArgParser() throws ParseException {
        {
            String[] args = "--intVal=2".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(2, intVal);
            Assert.assertEquals(1e10, doubleVal, 1e-13);
        }
        {
            String[] args = "--intVal 3 --doubleVal=3e10".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(3, intVal);
            Assert.assertEquals(3e10, doubleVal, 1e-13);
        }
    }

    @Test
    public void testHasArg() throws ParseException {
        String[] args = "--boolArg FALSE --boolNoArg".split(" ");

        ArgParser parser = new ArgParser();
        parser.addClass(ArgParserTest.class);
        parser.parseArgs(args);

        Assert.assertEquals(false, boolArg);
        Assert.assertEquals(true, boolNoArg);
    }

    public static class RequiredOpts {

        @Opt(hasArg = true, description = "my requiredInt", required = true)
        public static int requiredInt = 1;

    }

    @Test
    public void testRequired() {
        {
            String[] args = "--requiredInt 2".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
            } catch (ParseException e) {
                Assert.fail();
            }

            Assert.assertEquals(2, RequiredOpts.requiredInt);
        }
        {
            String[] args = "".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
                Assert.fail();
            } catch (ParseException e) {
                // success
            }

        }
    }

    public void testName() {
        // TODO: write this test.
    }

    public void testUsage() {
        // TODO: write this test.
    }
}
