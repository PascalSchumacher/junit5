/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit4;

import static org.junit.gen5.engine.TestExecutionResult.*;

import org.junit.gen5.engine.EngineAwareTestDescriptor;
import org.junit.gen5.engine.EngineDescriptor;
import org.junit.gen5.engine.EngineExecutionListener;
import org.junit.gen5.engine.ExecutionRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.TestEngine;
import org.junit.gen5.engine.TestPlanSpecification;
import org.junit.gen5.engine.junit4.descriptor.RunnerTestDescriptor;
import org.junit.gen5.engine.junit4.discovery.JUnit4TestPlanSpecificationResolver;
import org.junit.gen5.engine.junit4.execution.RunListenerAdapter;
import org.junit.gen5.engine.junit4.execution.TestRun;
import org.junit.runner.JUnitCore;

public class JUnit4TestEngine implements TestEngine {

	@Override
	public String getId() {
		return "junit4";
	}

	@Override
	public EngineAwareTestDescriptor discoverTests(TestPlanSpecification specification) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(this);
		new JUnit4TestPlanSpecificationResolver(engineDescriptor).resolve(specification);
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		TestDescriptor engineTestDescriptor = request.getRootTestDescriptor();
		engineExecutionListener.executionStarted(engineTestDescriptor);
		executeAllChildren(request);
		engineExecutionListener.executionFinished(engineTestDescriptor, successful());
	}

	private void executeAllChildren(ExecutionRequest request) {
		// @formatter:off
		request.getRootTestDescriptor()
			.getChildren()
			.stream()
			.map(RunnerTestDescriptor.class::cast)
			.forEach(runnerTestDescriptor -> executeSingleRunner(runnerTestDescriptor, request.getEngineExecutionListener()));
		// @formatter:on
	}

	private void executeSingleRunner(RunnerTestDescriptor runnerTestDescriptor,
			EngineExecutionListener engineExecutionListener) {
		TestRun testRun = new TestRun(runnerTestDescriptor);
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListenerAdapter(testRun, engineExecutionListener));
		try {
			core.run(runnerTestDescriptor.getRunner());
		}
		catch (Throwable t) {
			if (testRun.isNotStarted(runnerTestDescriptor)) {
				engineExecutionListener.executionStarted(runnerTestDescriptor);
			}
			engineExecutionListener.executionFinished(runnerTestDescriptor, failed(t));
		}
	}
}
