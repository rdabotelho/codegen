package com.m2r.codegen;

import com.m2r.codegen.parser.*;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Codegen {

    static final String GLOBAL_SCOPE = "global";
    static Logger logger = Logger.getLogger(Codegen.class.getSimpleName());

    private Consumer<DomainList> startEvent;

    public void setStartEvent(Consumer<DomainList> startEvent) {
        this.startEvent = startEvent;
    }

    public void generate(String projectName, String basePackage, List<Template> templates, File ... scriptFiles) {

        try {

            DomainList context = new DomainList();
            context.setProjectName(StringWrapper.of(projectName));
            context.setBasePackage(StringWrapper.of(basePackage));
            for (File scriptFile : scriptFiles) {
                Reader reader = new FileReader(scriptFile);
                DomainList domainList = ScriptParser.parse(reader);
                context.getDomains().addAll(domainList.getDomains());
                reader.close();
            }
            if (this.startEvent != null) {
                this.startEvent.accept(context);
            }
            context.finallyProcess();

            // Global scope
            for (Template template : templates) {
                if (GLOBAL_SCOPE.equals(template.getScope())) {
                    ScriptFields scriptFields = parseScriptFields(template, context, null);
                    mergeTemplate(template, context, null, scriptFields);
                }
            }

            // Domain scope
            List<StringWrapper> genList = new ArrayList<>();
            for (Domain domain : context.getDomains()) {
                boolean genAll = !domain.hasParam("generations");
                genList.clear();
                if (!genAll) {
                    ParamValue generations = domain.getParam("generations", true);
                    genList.addAll(generations.getValues());
                }
                for (Template template : templates) {
                    if (!genAll) {
                        boolean _continue = true;
                        for (StringWrapper sw : genList) {
                            if (sw.toString().equalsIgnoreCase(template.getName())) {
                                _continue = false;
                                break;
                            }
                        }
                        if (_continue) {
                            continue;
                        }
                    }
                    if (domain.getType().toString().equals(template.getScope())) {
                        ScriptFields scriptFields = parseScriptFields(template, context, domain);
                        mergeTemplate(template, context, domain, scriptFields);
                    }
                }
            }

        }
        catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    private ScriptFields parseScriptFields(Template template, DomainList context, Domain domain) {
        Template templateScriptFields = new Template();
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = "tmpScriptFields.vm";
        templateScriptFields.setDirectory(tempDir);
        templateScriptFields.setFileName(fileName);
        File tempFile = new File(tempDir, fileName);
        try {
            String script = String.format("%s;%s;#if(%s)true#{else}false#{end}", template.getOutputDir(), template.getOutputFileName(), template.getCreateIf());
            Files.write(tempFile.toPath(), script.getBytes());
            StringWriter writer = new StringWriter();
            mergeTemplate(templateScriptFields, context, domain, writer);
            return new ScriptFields(writer.toString());
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return new ScriptFields(String.format("%s;%s.txt;%s", template.getOutputDir(), domain.getName(), "true"));
    }

    private void mergeTemplate(Template template, DomainList context, Domain domain, ScriptFields scriptFields) throws Exception {
        if (scriptFields.createIf.equals("false")) {
            return;
        }
        File file = new File(scriptFields.dir, scriptFields.fileName);
        if (template.getStartEvent() != null) {
            if (!template.getStartEvent().on(template, domain, file)) {
                return;
            }
        }
        File dir = file.toPath().getParent().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Writer writer = new FileWriter(file);
        mergeTemplate(template, context, domain, writer);
        writer.close();
        if (template.getEndEvent() != null) {
            template.getEndEvent().on(template, domain, file);
        }
    }

    private void mergeTemplate(Template template, DomainList context, Domain domain, Writer writer) throws Exception {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.addProperty("file.resource.loader.path", template.getDirectory());
        velocityEngine.init();
        VelocityContext velocityContext = new VelocityContext();
        if (context != null) {
            velocityContext.put("context", context);
        }
        if (domain != null) {
            velocityContext.put("domain", domain);
        }
        org.apache.velocity.Template tmp = velocityEngine.getTemplate(template.getFileName());
        tmp.merge(velocityContext, writer);
    }

    static class ScriptFields {
        String dir;
        String fileName;
        String createIf;

        public ScriptFields(String strFields) {
            String[] fields = strFields.split(";");
            dir = fields[0];
            fileName = fields[1];
            createIf = fields[2];
        }

    }
    
}
