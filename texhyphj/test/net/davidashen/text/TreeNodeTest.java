package net.davidashen.text;

import net.davidashen.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.CoreMatchers.allOf;

public class TreeNodeTest {

	@Test
	public void rootNodeHaAnEmptySegment() {
		TreeNode root = TreeNode.createRoot();

		assertThat("root should be root", root.isRoot());
		assertThat("root shold be blank", root.isBlank());
		assertThat("root segment", root.getSegment(), equalTo(""));
	}

	// TODO: Check that int[] is always one element longer than segment String.

	@Test
	public void shouldHaveWeakestPossibleHyphenationByDefault() {
		TreeNode node = new TreeNode("watson");

		int[] defaultHypenation = node.getHyphenation();
		for (int i = 0; i < defaultHypenation.length; i++) {
			assertThat("Default hypenation is all 0. ELement [" + i + "]",
					defaultHypenation[i], equalTo(0));
		}
		assertThat("Default hypernation is blank", node.isBlank());
	}

	@Test
	public void canAddChildToRoot() {
		TreeNode node = TreeNode.createRoot();

		node.createChild("a", new int[] { 0, 0 });

		TreeNode childNode = node.getChild('a');
		assertThat("child node", childNode,
				hasProperty("segment", equalTo("a")));
		assertThat("child node", childNode,
				hasProperty("lastCharacter", equalTo('a')));
		assertArrayEquals("child node hyphenation", new int[] { 0, 0 },
				childNode.getHyphenation());
	}

	@Test
	public void canAddDirectChild() {
		TreeNode node = new TreeNode("z");
		node.createChild("za", new int[] { 0, 0, 1 });

		TreeNode childNode = node.getChild('a');
		assertThat("child node", childNode,
				hasProperty("segment", equalTo("za")));
		assertThat("child node", childNode,
				hasProperty("lastCharacter", equalTo('a')));
		assertArrayEquals("child node hyphenation", new int[] { 0, 0, 1 },
				childNode.getHyphenation());
	}

	@Test
	public void grandChildShouldBeCreatedAsLeaf() {
		TreeNode node = new TreeNode("z");
		node.createChild("za", new int[] { 0, 0, 1 });
		node.createChild("zab", new int[] { 0, 5, 2, 0 });

		TreeNode childNode = node.getChild('a');
		TreeNode grandchildNode = childNode.getChild('b');

		assertThat("grandchild node", grandchildNode,
				hasProperty("segment", equalTo("zab")));
		assertThat("grandchild node", grandchildNode,
				hasProperty("lastCharacter", equalTo('b')));
		assertArrayEquals("grandchild hyphenation", new int[] { 0, 5, 2, 0 },
				grandchildNode.getHyphenation());
	}

	@Test(expected = RuntimeException.class)
	public void canNotAddChildToLongerSegment() {
		TreeNode node = new TreeNode("aa");
		node.createChild("a", new int[] { 0, 1 });
	}

	@Test(expected = RuntimeException.class)
	public void aChildCanNotBeCreatedOnTheWrongNode() {
		TreeNode node = new TreeNode("a");
		node.createChild("za", new int[] { 0, 0, 1 });
	}
	
	@Test
	public void addGrandchildCreatesChildren() {
		TreeNode root = TreeNode.createRoot();
		root.createChild("za", new int[] { 0, 0, 1 });
		
		TreeNode middleNode = root.getChild('z');
		assertThat(middleNode, hasProperty("segment", equalTo("z")));
		assertThat(middleNode, hasProperty("blank", equalTo(true)));
	}

	
	@Test 
	public void thatNewNodeCanBeCreatedFromPattern() {
		//Digit first
		TreeNode zb = TreeNode.createFromPattern("4zb");
		assertEquals("node segment", "zb", zb.getSegment());
		assertArrayEquals("hyphenation", new int[] { 4, 0, 0 }, zb.getHyphenation());

		//Digit in the middle
		TreeNode zero = TreeNode.createFromPattern("ze3ro");
		assertEquals("node segment", "zero", zero.getSegment());
		assertArrayEquals("hyphenation", new int[] { 0,0, 3,0,0 }, zero.getHyphenation());

		//Digit at the end
		TreeNode za1 = TreeNode.createFromPattern("za1");
		assertEquals("node segment", "za", za1.getSegment());
		assertArrayEquals("hyphenation", new int[] { 0, 0, 1 }, za1.getHyphenation());
	} 
	
	//TODO: Support control numbers higher than 9

	
	@Test 
	public void thatChildNodeCanBeCreatedFromPattern() {
		TreeNode root = TreeNode.createRoot();
		root.createChildFromPattern("a1");
		
		TreeNode child = root.getChild('a');
		assertThat( child,
				hasProperty("segment", equalTo("a")));
		assertArrayEquals("grandchild hyphenation", new int[] { 0, 1 },
				child.getHyphenation());
	}
	
	// TODO: Replace existing blank node with concrete node.
	// TODO: Exception on (non-blank) duplicate


	@Test
	public void canProduceListStructure() {
		TreeNode root = TreeNode.createFromPattern("z");
		root.createChildFromPattern("za1");

		//Expected: ('z' [0,0] ('a' [0,0,1]) )
		List list = root.toList();
		assertThat("z rule", list.head(), allOf(instanceOf(Character.class), equalTo('z')));
		assertArrayEquals("z rule", new int[]{0,0}, (int[])list.longTail().head());
		assertThat("z list length", list.length(), equalTo(3));
		
		List za = (List)list.last();
		assertThat("za rule", za.head(), allOf(instanceOf(Character.class), equalTo('a')));
		assertArrayEquals(new int[]{0,0,1}, (int[])za.longTail().head());
		assertThat("za list length", za.length(), equalTo(2));
	}
	
	// TODO: How to handle edge case TreeNode.createRoot().toList();?
	
	
	@Test
	public void generatedListStructureWorksWithHyphenator() {
		TreeNode root = TreeNode.createRoot();
		root.createChildFromPattern("1own");
		root.createChildFromPattern("v2e");
		root.createChildFromPattern("la3");
		
		Scanner scanner = new TreeNodeScanner(root);
		
		Hyphenator hyphenator = new Hyphenator();
		hyphenator.setRuleSet(scanner);
		
		String actual = hyphenator.hyphenate("The quick brown fox jumps over the lazy dog.");
		String expected = "The quick br\u00adown fox jumps over the la\u00adzy dog.";
		assertEquals(expected, actual);
	}

	public static class TreeNodeScanner implements Scanner  {
		private final TreeNode rootNode;
		
		public TreeNodeScanner(TreeNode root) {
			rootNode = root;
		}
		
		public int[] getException(String word) {
			return null;
		}
		
		public List getList(int c) {
			List list = new List();
			if(rootNode.hasChild((char)c)){
				list.snoc(rootNode.getChild((char)c).toList());
			} 
			return list;
		}
	}
}