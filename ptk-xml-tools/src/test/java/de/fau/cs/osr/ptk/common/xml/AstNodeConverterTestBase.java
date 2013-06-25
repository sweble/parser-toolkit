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

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.fau.cs.osr.ptk.common.comparer.AstComparer;
import de.fau.cs.osr.ptk.common.serialization.SimpleTypeNameMapper;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.TestAstNode;
import de.fau.cs.osr.ptk.common.test.TestAstBuilder.Text;
import de.fau.cs.osr.utils.ComparisonException;

public class AstNodeConverterTestBase
{
	private XStream xstream;
	
	private AstNodeConverter<TestAstNode> converter;
	
	// =========================================================================
	
	@Before
	public void before()
	{
		converter = AstNodeConverter.forNodeType(TestAstNode.class);
		converter.setStringNodeType(Text.class);
		
		xstream = new XStream(new DomDriver());
		xstream.registerConverter(converter);
		xstream.setMode(XStream.NO_REFERENCES);
		xstream.processAnnotations(ArticleContainer.class);
	}
	
	public void setupDefaultNodeFactory()
	{
		converter.setNodeFactory(TestAstBuilder.getFactory());
	}
	
	public void setupDefaultTypeMappings()
	{
		SimpleTypeNameMapper typeNameMapper = new SimpleTypeNameMapper();
		typeNameMapper.add(TestAstBuilder.Text.class, "text");
		typeNameMapper.add(TestAstBuilder.NodeList.class, "list");
		typeNameMapper.add(TestAstBuilder.Section.class, "section");
		typeNameMapper.add(TestAstBuilder.Title.class, "title");
		typeNameMapper.add(TestAstBuilder.Body.class, "body");
		typeNameMapper.add(TestAstBuilder.Document.class, "document");
		typeNameMapper.add(TestAstBuilder.IdNode.class, "id");
		typeNameMapper.add(TestAstBuilder.Url.class, "url");
		typeNameMapper.add(TestAstBuilder.NodeWithObjProp.class, "nwop");
		typeNameMapper.add(TestAstBuilder.NodeWithPropAndContent.class, "nwpac");
		converter.setTypeNameMapper(typeNameMapper);
		
		converter.suppressNode(TestAstBuilder.Body.NoBody.class);
		converter.suppressNode(TestAstBuilder.Title.NoTitle.class);
		
		converter.suppressTypeInfo(TestAstBuilder.Body.EmptyBody.class);
		converter.suppressTypeInfo(TestAstBuilder.Body.BodyImpl.class);
		converter.suppressTypeInfo(TestAstBuilder.Title.EmptyTitle.class);
		converter.suppressTypeInfo(TestAstBuilder.Title.TitleImpl.class);
	}
	
	public AstNodeConverter<TestAstNode> getConverter()
	{
		return converter;
	}
	
	public XStream getXstream()
	{
		return xstream;
	}
	
	public String serialize(Object what)
	{
		return xstream.toXML(what);
	}
	
	public Object deserialize(String xml)
	{
		return xstream.fromXML(xml);
	}
	
	public void roundtrip(TestAstNode node) throws ComparisonException
	{
		String xml = serialize(node);
		
		TestAstNode restoredNode = (TestAstNode) deserialize(xml);
		
		try
		{
			AstComparer.compareAndThrow(node, restoredNode, true, true);
		}
		catch (ComparisonException e)
		{
			printXml(xml);
			printRestoredXml(restoredNode);
			throw e;
		}
	}
	
	public void printSerialized(TestAstNode node)
	{
		System.out.println(StringUtils.repeat("=", 80));
		System.out.println("\"\"\"" + serialize(node) + "\"\"\"");
		System.out.println(StringUtils.repeat("=", 80));
	}
	
	public void printXml(String xml)
	{
		System.err.println("Original XML:");
		System.err.println(StringUtils.repeat("=", 80));
		System.err.println("\"\"\"" + xml + "\"\"\"");
		System.err.println(StringUtils.repeat("=", 80));
	}
	
	public void printRestoredXml(Object restoredNode)
	{
		System.err.println("XML after round trip:");
		System.err.println(StringUtils.repeat("=", 80));
		try
		{
			System.err.println("\"\"\"" + serialize(restoredNode) + "\"\"\"");
		}
		catch (Exception e)
		{
			System.err.println("Failed to serialize restored AST!");
			e.printStackTrace(System.err);
		}
		System.err.println(StringUtils.repeat("=", 80));
	}
	
	public Document parseXml(String xml) throws Exception
	{
		ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(is);
		is.close();
		doc.getDocumentElement().normalize();
		return doc;
	}
	
	public Node queryNode(String expression, Node doc) throws Exception
	{
		return (Node) xpath(expression, doc, XPathConstants.NODE);
	}
	
	public NodeList queryNodeSet(String expression, Node doc) throws Exception
	{
		return (NodeList) xpath(expression, doc, XPathConstants.NODESET);
	}
	
	public Object xpath(String expression, Node doc, QName returnType) throws Exception
	{
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		return xpath.evaluate(expression, doc, returnType);
	}
}
