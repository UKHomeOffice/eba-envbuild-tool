package com.ipt.ebsa.environment.data.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestBuild {

	private EnvironmentDataImpl environmentData;
	private Build build;

	@Before
	public void setUpBuild() {
		environmentData = mock(EnvironmentDataImpl.class);
		build = new Build("example", environmentData);
		SequencePlaceHolder sequence1 = new SequencePlaceHolder("example-sequence-1", environmentData);
		sequence1.parameters.put("sequence-1-param-1", "Paul");
		sequence1.parameters.put("sequence-1-param-2", "Barry");
		build.sequences.add(sequence1);
		SequencePlaceHolder sequence2 = new SequencePlaceHolder("example-sequence-2", environmentData);
		sequence2.parameters.put("sequence-2-param-1", "Bodger");
		sequence2.parameters.put("sequence-2-param-2", "Badger");
		build.sequences.add(sequence2);
		
		Map<String, SequencePlaceHolder> sequences = new HashMap<>();
		sequences.put("example-sequence-1", sequence1);
		sequences.put("example-sequence-2", new SequencePlaceHolder("example-sequence-2", environmentData));
		when(environmentData.getSequencePlaceholders()).thenReturn(sequences);
	}
	
	/**
	 * Tests getting the sequences that make up a build.
	 */
	@Test
	public void getSequences() {
		List<ParameterisedNode> children = build.getChildren();
		assertEquals("Number of sequences", 2, children.size());
		
		Sequence sequence1 = (Sequence) children.get(0);
		assertEquals("Sequence 1 parameter 1",  "Paul", sequence1.getParameters().get("sequence-1-param-1"));
		assertEquals("Sequence 1 parameter 2",  "Barry", sequence1.getParameters().get("sequence-1-param-2"));
		
		Sequence sequence2 = (Sequence) children.get(1);
		assertEquals("Sequence 2 parameter 1",  "Bodger", sequence2.getParameters().get("sequence-2-param-1"));
		assertEquals("Sequence 2 parameter 2",  "Badger", sequence2.getParameters().get("sequence-2-param-2"));
	}
	
	
	/**
	 * Tests detection and handling of a missing sequence.
	 */
	@Test(expected=IllegalStateException.class)
	public void handleMissingSequence() {
		build.sequences.add(new SequencePlaceHolder("missing-sequence-1", environmentData));
		build.getChildren();
	}
}
