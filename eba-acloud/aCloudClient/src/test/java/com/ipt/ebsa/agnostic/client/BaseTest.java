package com.ipt.ebsa.agnostic.client;

import java.lang.reflect.Method;
import java.security.SecureRandom;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;

import com.ipt.ebsa.agnostic.cloud.command.v1.CmdCommand;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdDetail;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdEnvironmentType;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdErrorStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdExecute.CmdEnvironmentContainer;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdStrategy;
import com.ipt.ebsa.agnostic.cloud.command.v1.CmdVirtualApplication;
import com.ipt.ebsa.agnostic.cloud.command.v1.Execute;
import com.jcabi.aspects.Loggable;

/**
 * Base testing class for using use with the test framework
 * 
 * Test contains performance monitoring by default, test data isolation utility and properties loading.
 * 
 *
 */
@Loggable(prepend=true)
public class BaseTest {
	
	private static Logger logger = LogManager.getLogger(BaseTest.class);
	Instant timer = null;
	
	public static SecureRandom random = new SecureRandom();
	
	protected static Weld weld = new Weld();
	protected static WeldContainer container = null;
	
	@Before
	public void setUpBaseTest() throws Exception {
		timer = startTimer();
	}
	
	@After
	public void tearDownBaseTest() throws Exception {
		finishTimer(timer);
		timer = null;
	}
	
	private Instant startTimer() {
		return new Instant();
	}
	
	private void finishTimer(Instant startInstant) {
		Interval interval = new Interval(startInstant, new Instant());
		logger.info("Operation took "+interval.toDuration().getStandardSeconds()+ " seconds");
	}
	
	public static String getTestPrefixIdent() {
		String ident =  "UNITTEST"+random.nextInt();
		logger.debug("Generated Test Ident "+ident);
		return ident;
	}
	
	public CmdExecute getBaseInstruction() {
			CmdExecute baseInstructionExecute = new CmdExecute();
			return baseInstructionExecute;
	}
	
	public CmdEnvironmentContainer getBaseEnvironmentContainerInstruction(CmdCommand command, CmdStrategy strategy) {
		CmdEnvironmentContainer baseInstructionEnvironmentContainer = new CmdEnvironmentContainer();
		baseInstructionEnvironmentContainer.setCommand(command);
		baseInstructionEnvironmentContainer.setErrorStrategy(CmdErrorStrategy.OPTIMISTIC);
		baseInstructionEnvironmentContainer.setStrategy(strategy);
		baseInstructionEnvironmentContainer.setIncludes(".*");
		return baseInstructionEnvironmentContainer;
	}
	
	public CmdEnvironmentType getBaseEnvironmentInstruction(CmdCommand command, CmdStrategy strategy) {
		CmdEnvironmentType baseInstructionEnvironment = new CmdEnvironmentType();
		baseInstructionEnvironment.setCommand(command);
		baseInstructionEnvironment.setErrorStrategy(CmdErrorStrategy.OPTIMISTIC);
		baseInstructionEnvironment.setStrategy(strategy);
		baseInstructionEnvironment.setIncludes(".*");
		return baseInstructionEnvironment;
	}
	
	public CmdVirtualApplication getBasicVMC(CmdCommand command, CmdStrategy strategy, String includes) {
		CmdVirtualApplication vmc = new CmdVirtualApplication();
		vmc.setCommand(command);
		vmc.setStrategy(strategy);
		vmc.setIncludes(includes);
		vmc.setErrorStrategy(CmdErrorStrategy.OPTIMISTIC);
		return vmc;
	}
	
	public CmdVirtualApplication getBasicVMC(CmdCommand command, CmdStrategy strategy) {
		return getBasicVMC(command, strategy, ".*");
	}
	
	public CmdDetail getBasicCmdDetail(CmdCommand command, CmdStrategy strategy, String includes) {
		CmdDetail vm = new CmdDetail();
		vm.setCommand(command);
		vm.setStrategy(strategy);
		vm.setIncludes(includes);
		vm.setErrorStrategy(CmdErrorStrategy.OPTIMISTIC);
		return vm;
	}
	
	public CmdDetail getBasicCmdDetail(CmdCommand command, CmdStrategy strategy) {
		return getBasicCmdDetail(command, strategy, ".*");
	}
	
	public CmdExecute getBaseInstruction(CmdCommand command, CmdStrategy strategy) {
		Execute execute = new Execute();
		//CmdExecute execute = getBaseInstruction();
		CmdEnvironmentContainer container = getBaseEnvironmentContainerInstruction(command,strategy);
		execute.setEnvironmentContainer(container);
		CmdEnvironmentType environment = getBaseEnvironmentInstruction(command,strategy);
		container.setEnvironment(environment);
		CmdVirtualApplication vmc = getBasicVMC(command,strategy);
		environment.getVirtualMachineContainer().add(vmc);
		vmc.getApplicationNetwork().add(getBasicCmdDetail(command,strategy));
		vmc.getVirtualMachine().add(getBasicCmdDetail(command,strategy));
		return execute;
	}
	
//	<ac:EnvironmentContainer  includes=".*" errorStrategy="exit" strategy="exists" command="confirm">
//	<ac:Environment  includes=".*" errorStrategy="exit" strategy="exists" command="confirm">
//		<ac:options>
//			<ac:option name="overwriteEmptyTemplateMachines" value="true"/>
//		</ac:options>
//		<ac:VirtualMachineContainer  includes=".*" errorStrategy="exit" strategy="exists" command="start">
//		</ac:VirtualMachineContainer>
//		<ac:Overrides>
//			<ac:override xpath="/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm1']/VMOrder" value="1"/>
//			<ac:override xpath="/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm2']/VMOrder" value="2"/>
//			<ac:override xpath="/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm3']/VMOrder" value="3"/>
//			<ac:override xpath="/GeographicContainer/EnvironmentContainer/Environment/EnvironmentDefinition/VirtualMachineContainer[name='UNITTEST_SUSD_TEST_VMC']/VirtualMachine[vmName='UNITTEST_SUSD_testvm4']/VMOrder" value="4"/>
//		</ac:Overrides>
//	</ac:Environment>
//</ac:EnvironmentContainer>
	
	/**
	 * Method the do a shallow copy of an object.
	 * 
	 * Copies set/get/is methods. Copies get methods that are collections into the cloned object using the addAll method.
	 * 
	 * Intended to shallow copy the XML model objects so the object can be changed in the unit test with minimal effort.
	 * 
	 * @param objectToClone
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T shallowClone(T objectToClone) throws Exception {
		Method[] gettersAndSetters = objectToClone.getClass().getMethods();
		T cloned = (T) objectToClone.getClass().newInstance();

		for (int i = 0; i < gettersAndSetters.length; i++) {
			String methodName = gettersAndSetters[i].getName();
			try {
				if (methodName.startsWith("get")) {
					if(gettersAndSetters[i].getGenericReturnType().toString().startsWith("java.util.List")) {
						logger.debug(String.format("Calling addAll for list method equivilent for %s on class %s", methodName,cloned.getClass().getName()));
						Object list = cloned.getClass().getMethod(methodName).invoke(cloned,new Object[0]);
						list.getClass().getMethod("addAll", this.getClass().getClassLoader().loadClass("java.util.Collection")). 
						invoke(list, gettersAndSetters[i].invoke(objectToClone, null));
					} else {
						logger.debug(String.format("Calling set method equivilent for %s on class %s", methodName,cloned.getClass().getName()));
						cloned.getClass().getMethod(methodName.replaceFirst("get", "set"), gettersAndSetters[i].getReturnType())
							.invoke(cloned, gettersAndSetters[i].invoke(objectToClone, null));
					}
				} else if (methodName.startsWith("is")) {
					cloned.getClass().getMethod(methodName.replaceFirst("is", "set"), gettersAndSetters[i].getReturnType())
							.invoke(cloned, gettersAndSetters[i].invoke(objectToClone, null));
				}

			} catch (NoSuchMethodException e) {
				logger.error(String.format("Calling set method equivilent for %s on class %s failed as the method does not exist", methodName,cloned.getClass().getName()));
			} catch (IllegalArgumentException e) {
				logger.error(String.format("Calling set method equivilent for %s on class %s failed with an IllegalArgumentException", methodName,cloned.getClass().getName()));
				
			}
		}
		return cloned;
	}

}
