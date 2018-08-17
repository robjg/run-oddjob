package org.oddjob.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSpecTest {
	
	private static final Logger logger = LoggerFactory.getLogger(FileSpecTest.class);
	
   @Test
	public void testMatch() {
		
		assertEquals(true, FileSpec.match("apple", "apple", false));
		assertEquals(true, FileSpec.match("*apple*", "apple", false));
		assertEquals(true, FileSpec.match("ap?le", "apple", false));
		assertEquals(true, FileSpec.match("*.jar", "apple.jar", false));
		
		assertEquals(true, FileSpec.match("*.jar", ".jar", false));
		assertEquals(false, FileSpec.match("?.jar", ".jar", false));
		assertEquals(false, FileSpec.match("*?.jar", ".jar", false));
		assertEquals(true, FileSpec.match("*?.jar", "a.jar", false));
	}
	
	// difficult to test since we don't know what directory the
	// test will be run from.
    @Test
	public void testGetFilesNoParent() throws IOException {
		
		logger.info("Running test from " + System.getProperty("user.dir"));
		
		FileSpec test = new FileSpec(new File("*"));
		
		File[] files = test.getFiles();
		
		logger.info(Arrays.toString(files));
		
		assertNotNull(files);
		
		for (int i = 0; i < files.length; ++i) {
			// find and test a directory
			if (files[i].isDirectory()) {
				
				FileSpec test2 = new FileSpec(files[i]);
				
				File[] files2 = test2.getFiles(); 
				logger.info(Arrays.toString(files2));
				
				assertEquals(1, files2.length);
				assertEquals(files[i], files2[0]);
			}
		}		
	}
}
