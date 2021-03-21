package com.m2r.codegen;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.m2r.codegen.parser.Template;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class CodegenTest {

	@Test
	public void test() throws Exception {

		// define output dir
		File outputFile = new File(System.getProperty("java.io.tmpdir"), "/codegen-test");
		if (outputFile.exists()) {
			FileUtils.deleteDirectory(outputFile);
		}
		outputFile.mkdir();
		String outputDir = outputFile.getAbsolutePath();
		System.out.println("OUTPUT DIR: " + outputDir);

		// define input dir
		String inputDir = System.getProperty("user.dir") + "/src/test/java/com/m2r/codegen";

		// list of templates
		List<Template> templates = new ArrayList<>();

		// configuration of class template
		Template template = new Template();
		template.setName("Model");
		template.setScope("class");
		template.setDirectory(inputDir + "/template");
		template.setFileName("template-class.vm");
		template.setOutputDir(outputDir);
		template.setOutputFileName("out/${domain.name.toPascalCase()}.java");
		templates.add(template);

		// configuration of enum template
		template = new Template();
		template.setName("Enum");
		template.setScope("enum");
		template.setDirectory(inputDir + "/template");
		template.setFileName("template-enum.vm");
		template.setOutputDir(outputDir);
		template.setOutputFileName("out/${domain.name.toPascalCase()}.java");
		templates.add(template);

		// process the templates
		Codegen codegen = new Codegen();
		codegen.generate("teste", null, templates, new File(inputDir + "/script/script.txt"));

		assertTrue(new File(outputDir, "out/User.java").exists());
		assertTrue(new File(outputDir, "out/City.java").exists());
		assertTrue(new File(outputDir, "out/RoleEnum.java").exists());
	}
	
}
