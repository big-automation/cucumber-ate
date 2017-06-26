package cucumber.runtime.ate;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Utils;
import cucumber.runtime.ClassFinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.bigtester.ate.GlobalUtils;
import org.bigtester.ate.model.caserunner.CaseRunner;
import org.bigtester.ate.model.casestep.ICucumberTestStep;
import org.bigtester.ate.model.casestep.ICucumberTestStep.CucumberStepType;
import org.bigtester.ate.model.casestep.ITestStep;
import org.bigtester.ate.model.casestep.TestCase;
import org.bigtester.ate.model.data.TestParameters;
import org.bigtester.ate.model.project.TestProject;
import org.bigtester.ate.model.project.TestSuite;
import org.bigtester.ate.model.project.XmlTestCase;

import static cucumber.runtime.io.MultiLoader.packageName;

class AteStepScanner {
    private final Collection<Class<? extends Annotation>> cucumberAnnotationClasses;

    private final ClassFinder classFinder;

    public AteStepScanner(ClassFinder classFinder) {
        this.classFinder = classFinder;
        cucumberAnnotationClasses = findCucumberAnnotationClasses();
    }

    /**
     * Registers step definitions and hooks for all steps in ate test project
     *
     * @param ateBackend the backend where stepdefs and hooks will be registered
     * @param gluePaths   where to look
     */
    public void scan(AteBackend ateBackend, List<String> gluePaths, TestProject ateTestProj) {
//        for (String gluePath : gluePaths) {
//            for (Class<?> glueCodeClass : classFinder.getDescendants(Object.class, packageName(gluePath))) {
//                while (glueCodeClass != null && glueCodeClass != Object.class && !Utils.isInstantiable(glueCodeClass)) {
//                    // those can't be instantiated without container class present.
//                    glueCodeClass = glueCodeClass.getSuperclass();
//                }
//                if (glueCodeClass != null) {
//                    for (Method method : glueCodeClass.getMethods()) {
//                        scan(ateBackend, method, glueCodeClass, ateTestProj);
//                    }
//                }
//            }
//        }
    	scan(ateBackend, ateTestProj);
    }

    /**
     * Registers step definitions and hooks.
     *
     * @param javaBackend   the backend where stepdefs and hooks will be registered.
     * @param method        a candidate for being a stepdef or hook.
     * @param glueCodeClass the class where the method is declared.
     */
    public void scan(AteBackend javaBackend, TestProject ateTestProject) {
        for (TestSuite testSuite: ateTestProject.getSuiteList()) {
        	for (XmlTestCase testCase: testSuite.getTestCaseList()) {
        		CaseRunner caseRunner = new CaseRunner();
				caseRunner.initializeTestCase(new TestParameters(testCase
						.getTestCaseFilePathName(), testCase
						.getTestCaseFilePathName(), ateTestProject
						.getStepThinkTime(), ateTestProject.getAppCtx(),
						ateTestProject));
				for (ITestStep step:caseRunner.getMyTestCase().getTestStepList()) {
					if (GlobalUtils.getTargetObject(step) instanceof ICucumberTestStep){
						ICucumberTestStep cucumberStep = (ICucumberTestStep) GlobalUtils.getTargetObject(step);
		                if (cucumberStep.getCucumberStepType().equals(CucumberStepType.HOOK)) {
		                    javaBackend.addHook(null, null);
		                } else if (isRegularStepdef(null)) {
		                    javaBackend.addStepDefinition(null, null);
		                }
					}
				}
            }
        }
    }
    
    public void scan(AteBackend javaBackend, Method method, Class<?> glueCodeClass) {
        for (Class<? extends Annotation> cucumberAnnotationClass : cucumberAnnotationClasses) {
            Annotation annotation = method.getAnnotation(cucumberAnnotationClass);
            if (annotation != null) {
                if (!method.getDeclaringClass().isAssignableFrom(glueCodeClass)) {
                    throw new CucumberException(String.format("%s isn't assignable from %s", method.getDeclaringClass(), glueCodeClass));
                }
                if (!glueCodeClass.equals(method.getDeclaringClass())) {
                    throw new CucumberException(String.format("You're not allowed to extend classes that define Step Definitions or hooks. %s extends %s", glueCodeClass, method.getDeclaringClass()));
                }
                if (isHookStep(null)) {
                    javaBackend.addHook(annotation, method);
                } else if (isRegularStepdef(null)) {
                    javaBackend.addStepDefinition(annotation, method);
                }
            }
        }
    }

    private Collection<Class<? extends Annotation>> findCucumberAnnotationClasses() {
        return classFinder.getDescendants(Annotation.class, "cucumber.api");
    }

    private boolean isHookStep(ICucumberTestStep cucumberStep) {
//        Class<? extends Annotation> annotationClass = annotation.annotationType();
//        return annotationClass.equals(Before.class) || annotationClass.equals(After.class);
    	return cucumberStep.getCucumberStepType().equals(CucumberStepType.HOOK);
    }

    private boolean isRegularStepdef(ICucumberTestStep cStep) {
        return !isHookStep(cStep);
    }
}
