package cucumber.runtime.ate;

import cucumber.api.java.ObjectFactory;
import cucumber.runtime.JdkPatternArgumentMatcher;
import cucumber.runtime.MethodFormat;
import cucumber.runtime.ParameterInfo;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.Utils;
import gherkin.I18n;
import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bigtester.ate.GlobalUtils;
import org.bigtester.ate.model.casestep.ICucumberTestStep;
import org.bigtester.ate.model.casestep.IStepJumpingEnclosedContainer;

class AteStepDefinition implements StepDefinition {
    private final ICucumberTestStep cucumberStep;
    private final Pattern pattern;
    private final long timeoutMillis;
    private final ObjectFactory objectFactory;

    private final JdkPatternArgumentMatcher argumentMatcher;
    private List<ParameterInfo> parameterInfos;

    public AteStepDefinition(ICucumberTestStep method, Pattern pattern, long timeoutMillis, ObjectFactory objectFactory) {
        this.cucumberStep = method;
        this.pattern = pattern;
        this.timeoutMillis = timeoutMillis;
        this.objectFactory = objectFactory;

        this.argumentMatcher = new JdkPatternArgumentMatcher(pattern);
        try {
			this.parameterInfos = ParameterInfo.fromMethod(GlobalUtils.getTargetObject(cucumberStep).getClass().getMethod("doStep", new Class[]{IStepJumpingEnclosedContainer.class}));
		} catch (NoSuchMethodException e) {
			this.parameterInfos = new ArrayList<ParameterInfo>();
		} catch (SecurityException e) {
			this.parameterInfos = new ArrayList<ParameterInfo>();
		}
    }

    @Override
    public void execute(I18n i18n, Object[] args) throws Throwable {
    	//TODO execution of ate steps will be invoked by ate for now.
    	return;
        //Utils.invoke(objectFactory.getInstance(method.getDeclaringClass()), method, timeoutMillis, args);
    }
    @Override
    public List<Argument> matchedArguments(Step step) {
        return argumentMatcher.argumentsFrom(step.getName());
    }

    @Override
    public String getLocation(boolean detail) {
        MethodFormat format = detail ? MethodFormat.FULL : MethodFormat.SHORT;
        try {
			return format.format(GlobalUtils.getTargetObject(cucumberStep).getClass().getMethod("doStep", new Class[]{IStepJumpingEnclosedContainer.class}));
		} catch (NoSuchMethodException e) {
			return "unknown";
		} catch (SecurityException e) {
			return "unknown";
		}
    }

    @Override
    public Integer getParameterCount() {
        return parameterInfos.size();
    }

    @Override
    public ParameterInfo getParameterType(int n, Type argumentType) {
        return parameterInfos.get(n);
    }

    @Override
    public boolean isDefinedAt(StackTraceElement e) {
        try {
			return e.getClassName().equals(GlobalUtils.getTargetObject(cucumberStep).getClass().getName()) && e.getMethodName().equals(GlobalUtils.getTargetObject(cucumberStep).getClass().getMethod("doStep", new Class[]{IStepJumpingEnclosedContainer.class}).getName());
		} catch (NoSuchMethodException e1) {
			return false;
		} catch (SecurityException e1) {
			return false;
		}
    }

    @Override
    public String getPattern() {
        return pattern.pattern();
    }

    @Override
    public boolean isScenarioScoped() {
        return false;
    }
}
