/**
 * Copyright 2011 The Open Source Research Group,
 *                University of Erlangen-Nürnberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fau.cs.osr.ptk.common.xml;

import static de.fau.cs.osr.ptk.common.test.TestAstBuilder.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import de.fau.cs.osr.ptk.common.ast.AstLocation;
import de.fau.cs.osr.ptk.common.comparer.AstComparer;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.Document;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.Section;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.Text;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.Url;
import de.fau.cs.osr.utils.ComparisonException;

public class AstNodeConverterTest
		extends
			AstNodeConverterTestBase
{
	@Before
	public void before()
	{
		super.before();
		setupDefaultNodeFactory();
		setupDefaultTypeMappings();
	}
	
	@Test
	public void testRoundTripWithImplicitRoots() throws Exception
	{
		Document doc = astDoc();
		roundtrip(doc);
	}
	
	@Test
	public void testRoundTripWithExplicitRoots() throws Exception
	{
		getConverter().setExplicitRoots(true);
		
		Document doc = astDoc();
		roundtrip(doc);
	}
	
	@Test
	public void testXmlFormatWithImplicitRoots() throws Exception
	{
		org.w3c.dom.Document doc = parseXml(serialize(astDoc()));
		
		// Has correct root node?
		String root = "/" + Document.class.getName().replace("$", "_-");
		assertNotNull(queryNode(root, doc));
		
		// Implicit root has no other children?
		assertEquals(0, queryNodeSet(root + "/*", doc).getLength());
	}
	
	@Test
	public void testXmlFormatWithExplicitRoots() throws Exception
	{
		getConverter().setExplicitRoots(true);
		org.w3c.dom.Document doc = parseXml(serialize(astDoc()));
		
		// Has correct root node?
		String root = "/" + Document.class.getName().replace("$", "_-");
		assertNotNull(queryNode(root, doc));
		
		// Has one explicit element as root?
		assertEquals(1, queryNodeSet(root + "/document", doc).getLength());
		
		// Explicit root has no other children?
		assertEquals(0, queryNodeSet(root + "/document/*", doc).getLength());
	}
	
	@Test
	public void testSerializationOfIntAttribute() throws Exception
	{
		Document doc = astDoc();
		doc.setAttribute("int", 5);
		roundtrip(doc);
	}
	
	@Test
	public void testSerializationOfStringAttribute() throws Exception
	{
		Document doc = astDoc();
		doc.setAttribute("str", "Hello World");
		roundtrip(doc);
	}
	
	@Test
	public void testSerializationOfPropertyWithArbitraryObjectAsValue() throws Exception
	{
		ArbitraryObj obj = new ArbitraryObj();
		obj.set();
		
		Document doc = astDoc(astObjProp(obj));
		roundtrip(doc);
	}
	
	@Test
	public void testSerializationOfPropertyWithNodeAsValue() throws Exception
	{
		ArbitraryObj obj = new ArbitraryObj();
		obj.set();
		
		Document doc = astDoc(astObjProp(astUrl().build()));
		roundtrip(doc);
	}
	
	@Test
	public void testSerializationOfPropertyWithNodeAsValueAndExplicitRoots() throws Exception
	{
		getConverter().setExplicitRoots(true);
		
		ArbitraryObj obj = new ArbitraryObj();
		obj.set();
		
		Document doc = astDoc(astObjProp(astUrl().build()));
		roundtrip(doc);
	}
	
	@Test
	public void testSerializationOfObjectArray() throws Exception
	{
		Document doc = astDoc();
		doc.setAttribute("array", new Object[] { astText("Hallo"), astUrl().build() });
		roundtrip(doc);
	}
	
	@Test
	public void testNoContentPropertyFoundWhenTextNodeTypeSet() throws Exception
	{
		Document doc = astDoc(astText("Hallo"));
		assertFalse(serialize(doc).contains("<content>"));
		
		getConverter().setStringNodeType(null);
		assertTrue(serialize(doc).contains("<content>"));
	}
	
	@Test
	public void testAttributesOnTextNodeForceContentElement() throws Exception
	{
		Text text = astText("Hallo");
		text.setAttribute("ruins", "it");
		Document doc = astDoc(text);
		assertTrue(serialize(doc).contains("<content>"));
	}
	
	@Test
	public void testWrapAstInArticleContainerAndRoundtrip() throws Exception
	{
		Document doc = astDoc(astText("Hallo Welt"));
		ArticleContainer ac = new ArticleContainer(doc);
		
		String xml = serialize(ac);
		
		ArticleContainer restoredAc = (ArticleContainer) deserialize(xml);
		
		try
		{
			AstComparer.compareAndThrow(doc, restoredAc.doc, true, true);
		}
		catch (ComparisonException e)
		{
			printXml(xml);
			printRestoredXml(restoredAc);
			throw e;
		}
	}
	
	@Test
	public void testWrapAstInArticleContainerAndCheckXml() throws Exception
	{
		ArticleContainer ac =
				new ArticleContainer(astDoc(astText("Hallo Welt")));
		
		org.w3c.dom.Document doc = parseXml(serialize(ac));
		
		Element rootElem = (Element) queryNode("/article-container", doc);
		assertNotNull(rootElem);
		assertEquals(ArticleContainer.XMLNS, rootElem.getAttribute("xmlns"));
		assertEquals(ArticleContainer.XMLNS_PTK, rootElem.getAttribute("xmlns:ptk"));
		
		assertEquals(1, queryNodeSet("/article-container/*", doc).getLength());
		assertNotNull(queryNode("/article-container/document", doc));
		
		assertEquals(1, queryNodeSet("/article-container/document/*", doc).getLength());
		assertNotNull(queryNode("/article-container/document/text[text() = 'Hallo Welt']", doc));
	}
	
	@Test
	public void testInstantiationOfNullReplacementProperty() throws Exception
	{
		Document doc = astDoc(astUrl().withProtocol("").build());
		getConverter().setSuppressEmptyStringProperties(true);
		
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertEquals(0, queryNodeSet("//protocol", xmlDoc).getLength());
		
		roundtrip(doc);
	}
	
	@Test
	public void testNullProperty() throws Exception
	{
		Document doc = astDoc(astObjProp(null));
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertEquals(0, queryNodeSet("//prop", xmlDoc).getLength());
		roundtrip(doc);
	}
	
	@Test
	public void testSuppressedProperty() throws Exception
	{
		Document doc = astDoc(astObjProp("Hello World"));
		getConverter().suppressProperty("prop");
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertEquals(0, queryNodeSet("//prop", xmlDoc).getLength());
	}
	
	@Test
	public void testNodeWithContentAndAnotherProperty() throws Exception
	{
		// First make sure that the node is properly recognized as string node
		Document doc = astDoc(astPropContent(null, "Hello World"));
		getConverter().setStringNodeType(TestAstBuilder.NodeWithPropAndContent.class);
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertEquals(0, queryNodeSet("//content", xmlDoc).getLength());
		roundtrip(doc);
		
		doc = astDoc(astPropContent(42, "Hello World"));
		getConverter().setStringNodeType(TestAstBuilder.NodeWithPropAndContent.class);
		xmlDoc = parseXml(serialize(doc));
		assertEquals(1, queryNodeSet("//content", xmlDoc).getLength());
		roundtrip(doc);
	}
	
	@Test
	public void testNodeWithAtLeastTwoNamedChildren() throws Exception
	{
		Document doc = astDoc(astSection().build());
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertNotNull(queryNode("//title", xmlDoc));
		assertNotNull(queryNode("//body", xmlDoc));
		roundtrip(doc);
	}
	
	@Test
	public void testStoreLocation() throws Exception
	{
		Document doc = astWithLocations();
		roundtrip(doc);
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertNotNull(((Element) queryNode("//text", xmlDoc)).getAttributeNode("ptk:location"));
		assertNotNull(((Element) queryNode("//url", xmlDoc)).getAttributeNode("ptk:location"));
	}
	
	@Test
	public void testSuppressLocation() throws Exception
	{
		Document doc = astWithLocations();
		getConverter().setStoreLocation(false);
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertNull(((Element) queryNode("//text", xmlDoc)).getAttributeNode("ptk:location"));
		assertNull(((Element) queryNode("//url", xmlDoc)).getAttributeNode("ptk:location"));
	}
	
	private Document astWithLocations()
	{
		Text text = astText("Hello");
		text.setNativeLocation(new AstLocation("some file", 42, 43));
		Url url = astUrl().build();
		url.setNativeLocation(new AstLocation("some file", 44, 45));
		Document doc = astDoc(text, url);
		return doc;
	}
	
	@Test
	public void testStoreNodesWithAttributes() throws Exception
	{
		Document doc = astWithAttributes();
		roundtrip(doc);
	}
	
	@Test
	public void testSuppressCertainAttributes() throws Exception
	{
		Document doc = astWithAttributes();
		getConverter().suppressAttribute("area52");
		String xml = serialize(doc);
		assertTrue(xml.contains("area51"));
		assertFalse(xml.contains("area52"));
	}
	
	@Test
	public void testSuppressAllAttributes() throws Exception
	{
		Document doc = astWithAttributes();
		getConverter().setStoreAttributes(false);
		String xml = serialize(doc);
		assertFalse(xml.contains("area51"));
		assertFalse(xml.contains("area52"));
	}
	
	private Document astWithAttributes()
	{
		Url url = astUrl().build();
		url.setAttribute("area51", "Hello World 1");
		url.setAttribute("area52", "Hello World 2");
		Document doc = astDoc(url);
		return doc;
	}
	
	@Test
	public void testStoreNullAttribute() throws Exception
	{
		Url url = astUrl().build();
		url.setAttribute("area51", null);
		Document doc = astDoc(url);
		roundtrip(doc);
	}
	
	@Test
	public void testBodyInterfaceNode() throws Exception
	{
		Section sec = astSection().build();
		Document doc = astDoc(sec);
		assertEquals(TestAstBuilder.Body.BodyImpl.class, sec.getBody().getClass());
		assertTrue(sec.hasBody());
		org.w3c.dom.Document xmlDoc = parseXml(serialize(doc));
		assertNotNull(queryNode("//section/body", xmlDoc));
		roundtrip(doc);
	}
	
	@Test
	public void testNoBodyNode() throws Exception
	{
		Section sec = astSection().build();
		sec.removeBody();
		Document doc = astDoc(sec);
		assertEquals(TestAstBuilder.Body.NoBody.class, sec.getBody().getClass());
		assertFalse(sec.hasBody());
		
		roundtrip(doc);
		
		String xml = serialize(doc);
		org.w3c.dom.Document xmlDoc = parseXml(xml);
		assertNull(queryNode("//section/body", xmlDoc));
		
		Document restoredDoc = (Document) deserialize(xml);
		assertFalse(((Section) restoredDoc.get(0)).hasBody());
	}
	
	@Test
	public void testNoTitleNodeToCoverAllPaths() throws Exception
	{
		Section sec = astSection().build();
		sec.removeTitle();
		Document doc = astDoc(sec);
		assertEquals(TestAstBuilder.Title.NoTitle.class, sec.getTitle().getClass());
		assertFalse(sec.hasTitle());
		assertTrue(sec.hasBody());
		
		roundtrip(doc);
		
		String xml = serialize(doc);
		org.w3c.dom.Document xmlDoc = parseXml(xml);
		assertNull(queryNode("//section/title", xmlDoc));
		
		Document restoredDoc = (Document) deserialize(xml);
		assertFalse(((Section) restoredDoc.get(0)).hasTitle());
	}
	
	@Test
	public void testRemoveEmptyTextNode() throws Exception
	{
		Document doc = astDoc(astText(""));
		assertTrue(serialize(doc).contains("<text>"));
		
		getConverter().setSuppressEmptyStringNodes(true);
		assertFalse(serialize(doc).contains("<text>"));
	}
	
	@Test
	public void testStoreComplexArrayAsAttribute() throws Exception
	{
		Document doc = astDoc();
		doc.setAttribute("DoubleTrouble", new Double[][] {
				new Double[] { 3.1415, 2 * 3.1415 },
				new Double[] { 2.7182, 2 * 2.7182 } });
		roundtrip(doc);
	}
}
