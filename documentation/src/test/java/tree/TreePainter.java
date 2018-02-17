/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * PROOF-OF-CONCEPT!
 */
@API(status = API.Status.EXPERIMENTAL)
public class TreePainter implements TestExecutionListener {

	private final Map<String, TreeNode> map;
	private TreeNode root;

	public TreePainter() {
		this.map = Collections.synchronizedMap(new HashMap<>());
	}

	private TreeNode addNode(TestIdentifier testIdentifier, Supplier<TreeNode> nodeSupplier) {
		TreeNode node = nodeSupplier.get();
		synchronized (map) {
			map.put(testIdentifier.getUniqueId(), node);
		}
		testIdentifier.getParentId().map(map::get).orElse(root).addChild(node);
		return node;
	}

	private TreeNode getNode(TestIdentifier testIdentifier) {
		return map.get(testIdentifier.getUniqueId());
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		root = new TreeNode(testPlan.toString());
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		root.print();
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		addNode(testIdentifier, () -> new TreeNode(testIdentifier));
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		getNode(testIdentifier).setResult(testExecutionResult);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		addNode(testIdentifier, () -> new TreeNode(testIdentifier, reason));
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		getNode(testIdentifier).addReportEntry(entry);
	}

	class TreeNode {

		private final String caption;
		private final long creation;
		long duration;
		private String reason;
		private TestIdentifier identifier;
		private TestExecutionResult result;
		List<ReportEntry> reports = Collections.emptyList();
		List<TreeNode> children = Collections.emptyList();
		boolean visible;

		TreeNode(String caption) {
			this.caption = caption;
			this.creation = System.currentTimeMillis();
			this.visible = false;
		}

		TreeNode(TestIdentifier identifier) {
			this(identifier.getDisplayName());
			this.identifier = identifier;
			this.visible = true;
		}

		TreeNode(TestIdentifier identifier, String reason) {
			this(identifier);
			this.reason = reason;
		}

		TreeNode addChild(TreeNode node) {
			if (children == Collections.EMPTY_LIST) {
				children = new ArrayList<>();
			}
			children.add(node);
			return this;
		}

		TreeNode addReportEntry(ReportEntry reportEntry) {
			if (reports == Collections.EMPTY_LIST) {
				reports = new ArrayList<>();
			}
			reports.add(reportEntry);
			return this;
		}

		TreeNode setResult(TestExecutionResult result) {
			this.result = result;
			this.duration = System.currentTimeMillis() - creation;
			return this;
		}

		void print() {
			print("", true);
		}

		private void print(String prefix, boolean isTail) {
			String status = reason;
			if (status == null) {
				status = "?";
				if (result != null) {
					status = result.getStatus().toString();
				}
			}
			System.out.println(prefix + (isTail ? "'-- " : "+-- ") + caption + " " + status);
			for (int i = 0; i < children.size() - 1; i++) {
				children.get(i).print(prefix + (isTail ? "    " : "|   "), false);
			}
			if (children.size() > 0) {
				children.get(children.size() - 1).print(prefix + (isTail ? "    " : "|   "), true);
			}
		}
	}

}
