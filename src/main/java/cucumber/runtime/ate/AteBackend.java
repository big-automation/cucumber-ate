package cucumber.runtime.ate;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.ObjectFactory;
import cucumber.api.java8.GlueBase;
import cucumber.api.java8.HookBody;
import cucumber.api.java8.HookNoArgsBody;
import cucumber.api.java8.StepdefBody;
import cucumber.runtime.Backend;
import cucumber.runtime.ClassFinder;
import cucumber.runtime.CucumberException;
import cucumber.runtime.DuplicateStepDefinitionException;
import cucumber.runtime.Env;
import cucumber.runtime.Glue;
import cucumber.runtime.UnreportedStepExecutor;
import cucumber.runtime.Utils;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import cucumber.runtime.snippets.FunctionNameGenerator;
import cucumber.runtime.snippets.Snippet;
import cucumber.runtime.snippets.SnippetGenerator;
import gherkin.formatter.model.Step;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bigtester.ate.GlobalUtils;
import org.bigtester.ate.TestProjectRunner;
import org.bigtester.ate.model.caserunner.CaseRunner;
import org.bigtester.ate.model.casestep.ICucumberTestStep;
import org.bigtester.ate.model.casestep.ITestCase;
import org.bigtester.ate.model.project.TestProject;
import org.bigtester.ate.model.project.TestSuite;
import org.bigtester.ate.model.project.XmlTestCase;
import org.dbunit.DatabaseUnitException;

import static cucumber.runtime.io.MultiLoader.packageName;

public class AteBackend implements Backend {
	public static final String defaultTestProjectXmlFilePathName = "indeedJobApplication/testproject.xml";
	private final TestProject testProject;
    public static final ThreadLocal<AteBackend> INSTANCE = new ThreadLocal<AteBackend>();
    private final AteSnippetGenerator snippetGenerator = new AteSnippetGenerator(createSnippet());

    private AteSnippet createSnippet() {
      
            return new AteSnippet();
      
    }

    private final ObjectFactory objectFactory;
    private final ClassFinder classFinder;

    private final AteStepScanner ateStepScanner;
    private Glue glue;
    private List<Class<? extends GlueBase>> glueBaseClasses = new ArrayList<Class<? extends GlueBase>>();

    /**
     * The constructor called by reflection by default.
     *
     * @param resourceLoader
     */
    public AteBackend(ResourceLoader resourceLoader) {
    	TestProjectRunner.registerXsdNameSpaceParsers();
    	TestProjectRunner.registerProblemHandlers();
		this.testProject =GlobalUtils
				.findTestProjectBean( TestProjectRunner.loadTestProjectContext(defaultTestProjectXmlFilePathName));
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        ateStepScanner = new AteStepScanner(classFinder);
        objectFactory = ObjectFactoryLoader.loadObjectFactory(classFinder, Env.INSTANCE.get(ObjectFactory.class.getName()));
    }

    public AteBackend(ObjectFactory objectFactory) {
    	TestProjectRunner.registerXsdNameSpaceParsers();
    	TestProjectRunner.registerProblemHandlers();
		this.testProject =GlobalUtils
				.findTestProjectBean( TestProjectRunner.loadTestProjectContext(defaultTestProjectXmlFilePathName));
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        ateStepScanner = new AteStepScanner(classFinder);
        this.objectFactory = objectFactory;
    }

    public AteBackend(ObjectFactory objectFactory, ClassFinder classFinder) {
    	TestProjectRunner.registerXsdNameSpaceParsers();
    	TestProjectRunner.registerProblemHandlers();
		this.testProject =GlobalUtils
				.findTestProjectBean( TestProjectRunner.loadTestProjectContext(defaultTestProjectXmlFilePathName));
        this.objectFactory = objectFactory;
        this.classFinder = classFinder;
        ateStepScanner = new AteStepScanner(classFinder);
    }

    @Override
    public void loadGlue(Glue glue, List<String> gluePaths) {
        this.glue = glue;
        // Scan for Java7 style glue (annotated methods)
        ateStepScanner.scan(this, gluePaths, this.testProject);

       
    }

    /**
     * Convenience method for frameworks that wish to load glue from methods explicitly (possibly
     * found with a different mechanism than Cucumber's built-in classpath scanning).
     *
     * @param glue          where stepdefs and hooks will be added.
     * @param method        a candidate method.
     * @param glueCodeClass the class implementing the method. Must not be a subclass of the class implementing the method.
     */
    public void loadGlue(Glue glue, Method method, Class<?> glueCodeClass) {
        this.glue = glue;
        ateStepScanner.scan(this, method, glueCodeClass);
    }

    @Override
    public void setUnreportedStepExecutor(UnreportedStepExecutor executor) {
        //Not used here yet
    }

    @Override
    public void buildWorld() {
    	try {
			TestProjectRunner.initDB(testProject.getAppCtx());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatabaseUnitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        objectFactory.start();

        // Instantiate all the stepdef classes for java8 - the stepdef will be initialised
        // in the constructor.
        try {
            INSTANCE.set(this);
            glue.removeScenarioScopedGlue();
            for (Class<? extends GlueBase> glueBaseClass : glueBaseClasses) {
                objectFactory.getInstance(glueBaseClass);
            }
        } finally {
            INSTANCE.remove();
        }
    }

    @Override
    public void disposeWorld() {
        objectFactory.stop();
    }

    @Override
    public String getSnippet(Step step, FunctionNameGenerator functionNameGenerator) {
        return snippetGenerator.getSnippet(step, functionNameGenerator);
    }

    void addStepDefinition(ICucumberTestStep cucumberStep, XmlTestCase ateTestCase, TestSuite testSuite, CaseRunner ateCaseRunner) {
        try {
            //if (objectFactory.addClass(method.getDeclaringClass())) {
                glue.addStepDefinition(new AteStepDefinition(cucumberStep, ateTestCase, testSuite, ateCaseRunner, pattern(cucumberStep), timeoutMillis(cucumberStep), objectFactory));
            //}
        } catch (DuplicateStepDefinitionException e) {
            throw e;
        } catch (Throwable e) {
            throw new CucumberException(e);
        }
    }

//    public void addStepDefinition(String regexp, long timeoutMillis, StepdefBody body, TypeIntrospector typeIntrospector) {
//        try {
//            glue.addStepDefinition(new Java8StepDefinition(Pattern.compile(regexp), timeoutMillis, body, typeIntrospector));
//        } catch (Exception e) {
//            throw new CucumberException(e);
//        }
//    }

    void addHook(Annotation annotation, Method method) {
        if (objectFactory.addClass(method.getDeclaringClass())) {
            if (annotation.annotationType().equals(Before.class)) {
                String[] tagExpressions = ((Before) annotation).value();
                long timeout = ((Before) annotation).timeout();
                glue.addBeforeHook(new AteHookDefinition(method, tagExpressions, ((Before) annotation).order(), timeout, objectFactory));
            } else {
                String[] tagExpressions = ((After) annotation).value();
                long timeout = ((After) annotation).timeout();
                glue.addAfterHook(new AteHookDefinition(method, tagExpressions, ((After) annotation).order(), timeout, objectFactory));
            }
        }
    }

//    public void addBeforeHookDefinition(String[] tagExpressions, long timeoutMillis, int order, HookBody body) {
//        glue.addBeforeHook(new Java8HookDefinition(tagExpressions, order, timeoutMillis, body));
//    }
//
//    public void addAfterHookDefinition(String[] tagExpressions, long timeoutMillis, int order, HookBody body) {
//        glue.addAfterHook(new Java8HookDefinition(tagExpressions, order, timeoutMillis, body));
//    }
//
//    public void addBeforeHookDefinition(String[] tagExpressions, long timeoutMillis, int order, HookNoArgsBody body) {
//        glue.addBeforeHook(new Java8HookDefinition(tagExpressions, order, timeoutMillis, body));
//    }
//
//    public void addAfterHookDefinition(String[] tagExpressions, long timeoutMillis, int order, HookNoArgsBody body) {
//        glue.addAfterHook(new Java8HookDefinition(tagExpressions, order, timeoutMillis, body));
//    }

    private Pattern pattern(ICucumberTestStep cucumberStep) throws Throwable {
        //Method regexpMethod = annotation.getClass().getMethod("value");
        //String regexpString = (String) Utils.invoke(annotation, regexpMethod, 0);
    	String regexpString = cucumberStep.getStepDescription();
        return Pattern.compile(regexpString);
    }

    private long timeoutMillis(ICucumberTestStep cucumberStep) throws Throwable {
        //Method regexpMethod = annotation.getClass().getMethod("timeout");
        //return (Long) Utils.invoke(annotation, regexpMethod, 0);
    	return cucumberStep.getTimeOutMillis();
    }

}
