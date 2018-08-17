package org.oddjob.launch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SystemPropertyArgParserTest {

   @Test
	public void testArgs() {
		
		SystemPropertyArgParser test = new SystemPropertyArgParser();
		
		String before = System.getProperty("favourite.fruit");
		
		String[] after = test.processArgs(new String[] { 
				"-Dfavourite.fruit=", "-Dfavourite.fruit=apples"
		});
		
		assertEquals(0, after.length);
		
		assertEquals("apples", System.getProperty("favourite.fruit"));
		
		if (before == null) {
			System.getProperties().remove("favourite.fruit");
		}
		else {
			System.setProperty("favourite.fruit", before);
		}
	}
	
   @Test
	public void testContinue() {
		
		SystemPropertyArgParser test = new SystemPropertyArgParser();
		
		String[] after = test.processArgs(new String[] { 
				"--", "-Dfavourite.fruit=apples"
		});
		
		assertEquals(2, after.length);
	}
	
   @Test
	public void testNoneMatches() {
		
		SystemPropertyArgParser test = new SystemPropertyArgParser();
		
		String[] after = test.processArgs(new String[] { 
				"-D", "-D=apples"
		});
		
		assertEquals(2, after.length);
	}
}
