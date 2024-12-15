package tmp;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlTest {

  public static void main(String[] args) throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();

    Document document = builder.newDocument();
    Element rootElement = document.createElement("Node");
    Element child1 = document.createElement("Node");
    child1.setAttribute("value", "bla");
    Element child2 = document.createElement("Node");
    child2.setAttribute("value", "blub");

    rootElement.appendChild(child1);
    rootElement.appendChild(child2);
    document.appendChild(rootElement);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(new File("/home/thomas/Documents/dev/own/taggy/x.xml"))));
  }
}
