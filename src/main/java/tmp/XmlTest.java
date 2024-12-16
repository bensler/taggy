package tmp;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.bensler.decaf.util.prefs.Prefs;

public class XmlTest {

  public static void main(String[] args) throws Exception {
    write();
    read();
  }

  static void read() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
    factory.setValidating(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(new EntityResolver() {
      @Override
      public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return (Prefs.DTD_SYSTEM_ID_1_0.equals(publicId)
          ? new InputSource(Prefs.class.getResourceAsStream(Prefs.DTD_RESOURCE_1_0))
          : null
        );
      }
    });
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException exception) throws SAXException {
        throw exception;
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
      }

      @Override
      public void error(SAXParseException exception) throws SAXException {
        throw exception;
      }
    });
    Document document = builder.parse(new File(new File(System.getProperty("user.dir")), "x.xml"));
    readElement(document.getDocumentElement(), "");
  }

  private static void readElement(Element element, String indent) {
    System.out.println(indent + element.getAttribute("name") + " # " + element.getAttribute("value"));

    NodeList childNodes = element.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (item.getNodeType() == Node.ELEMENT_NODE) {
        readElement((Element) item, indent + "  ");
      }
    }
  }

  static void write() throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();

    Document document = builder.newDocument();
    Element rootElement = document.createElement("node");
    rootElement.setAttribute("name", "taggy");
    Element child1 = document.createElement("node");
    child1.setAttribute("name", "a");
    child1.setAttribute("value", "blah");
    Element child2 = document.createElement("node");
    child2.setAttribute("name", "b");
    child2.setAttribute("value", "blubb");

    rootElement.appendChild(child1);
    rootElement.appendChild(child2);
    document.appendChild(rootElement);


    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, Prefs.DTD_SYSTEM_ID_1_0);
    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "");
    transformer.transform(new DOMSource(document), new StreamResult(new File(new File(System.getProperty("user.dir")), "x.xml")));
  }
}
