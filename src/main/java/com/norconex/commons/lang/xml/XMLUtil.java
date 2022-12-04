/* Copyright 2019-2022 Norconex Inc.
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
package com.norconex.commons.lang.xml;

import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * <p>
 * Utility XML-related methods. When applicable:
 * </p>
 * <ul>
 *   <li>Uses XML Schema version 1.1.</li>
 *   <li>Addresses XML security vulnerabilities (XXE).</li>
 *   <li>Wraps checked exceptions in a runtime {@link XMLException}.</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class XMLUtil {

    private static final Logger LOG = LoggerFactory.getLogger(XMLUtil.class);

    public static final String W3C_XML_SCHEMA_NS_URI_1_1 =
            "http://www.w3.org/XML/XMLSchema/v1.1";

    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String NAMESPACES =
            "http://xml.org/sax/features/namespaces";

    private static final Set<String> alreadyLogged = new HashSet<>();


    private XMLUtil() {}

    public static Validator createSchemaValidator(Schema schema) {
        Validator validator = schema.newValidator();
        set(validator, v -> v.setFeature(FEATURE_SECURE_PROCESSING, true));
        set(validator, v -> v.setProperty(ACCESS_EXTERNAL_DTD, ""));
        set(validator, v -> v.setProperty(ACCESS_EXTERNAL_SCHEMA, ""));
        return validator;
    }

    public static SchemaFactory createSchemaFactory() {
      return SchemaFactory.newInstance( //NOSONAR handled
              W3C_XML_SCHEMA_NS_URI_1_1);
  }

    public static XMLReader createXMLReader() {
        SAXParserFactory parserFactory = createSaxParserFactory();
        SAXParser parser;
        XMLReader xmlReader;
        try {
            parser = parserFactory.newSAXParser();
            xmlReader = parser.getXMLReader();
        } catch (ParserConfigurationException | SAXException e) {
            throw new XMLException("could not create SAX Parser.", e);
        }
        set(xmlReader, r -> r.setFeature(NAMESPACES, true));
        set(xmlReader, r -> r.setFeature(FEATURE_SECURE_PROCESSING, true));
        set(xmlReader, r -> r.setFeature(LOAD_EXTERNAL_DTD, false));
        set(xmlReader, r -> r.setFeature(EXTERNAL_GENERAL_ENTITIES, false));
        set(xmlReader, r -> r.setFeature(EXTERNAL_PARAMETER_ENTITIES, false));
        set(xmlReader, r -> r.setEntityResolver((publicId, systemId) -> null));
        return xmlReader;
    }

    public static DocumentBuilderFactory createDocumentBuilderFactory() {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance(); //NOSONAR handled
        set(factory, f -> f.setFeature(FEATURE_SECURE_PROCESSING, true));
        set(factory, f -> f.setFeature(EXTERNAL_GENERAL_ENTITIES, false));
        set(factory, f -> f.setFeature(EXTERNAL_PARAMETER_ENTITIES, false));
        set(factory, f -> f.setAttribute(ACCESS_EXTERNAL_DTD, ""));
        set(factory, f -> f.setAttribute(ACCESS_EXTERNAL_SCHEMA, ""));
        set(factory, f -> f.setExpandEntityReferences(false));
        return factory;
    }

    public static SAXParserFactory createSaxParserFactory() {
        SAXParserFactory factory =
                SAXParserFactory.newInstance(); //NOSONAR handled
        set(factory, f -> f.setFeature(FEATURE_SECURE_PROCESSING, true));
        set(factory, f -> f.setFeature(EXTERNAL_GENERAL_ENTITIES, false));
        set(factory, f -> f.setFeature(EXTERNAL_PARAMETER_ENTITIES, false));
        return factory;
    }

    public static XMLInputFactory createXMLInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(
                "javax.xml.stream.isSupportingExternalEntities", false);
        return factory;
    }

    /**
     * <p>
     * Creates an {@link XMLEventReader} from the supplied object representing
     * XML. Supported types are:
     * </p>
     * <ul>
     *   <li>{@link Path}</li>
     *   <li>{@link File}</li>
     *   <li>{@link Node}</li>
     *   <li>{@link XML}</li>
     *   <li>{@link String}</li>
     *   <li>{@link InputStream}</li>
     *   <li>{@link Reader}</li>
     *   <li>{@link XMLEventReader} (returns itself)</li>
     * </ul>
     * @param obj the object to read
     * @return XML event reader
     */
    public static XMLEventReader createXMLEventReader(Object obj) {
        if (obj instanceof Path) {
            return createXMLEventReader((Path) obj);
        }
        if (obj instanceof File) {
            return createXMLEventReader((File) obj);
        }
        if (obj instanceof Node) {
            return createXMLEventReader((Node) obj);
        }
        if (obj instanceof XML) {
            return createXMLEventReader((XML) obj);
        }
        if (obj instanceof String) {
            return createXMLEventReader((String) obj);
        }
        if (obj instanceof InputStream) {
            return createXMLEventReader((InputStream) obj);
        }
        if (obj instanceof Reader) {
            return createXMLEventReader((Reader) obj);
        }
        if (obj instanceof XMLEventReader) {
            return (XMLEventReader) obj;
        }
        throw new XMLException("Unsupported object type. Must be one of "
                + "Path, File, Node, XML, String, Reader, or XMLEventReader.");
    }

    // When a tag is prefixed with no namespace defined for it,
    // it is possible that the prefix is returned part of the local name.
    // We account for that in the next two methods, as we always want the
    // local name to be // the part after ":" when present.
    static String toLocalName(QName qname) {
        String name = toName(qname);
        String localName = StringUtils.substringAfterLast(name, ":");
        if (StringUtils.isBlank(localName)) {
            return name;
        }
        return localName;
    }
    static String toName(QName qname) {
        return StringUtils.isBlank(qname.getPrefix())
                ? qname.getLocalPart()
                : qname.getPrefix() + ":" + qname.getLocalPart();
    }

    private static XMLEventReader createXMLEventReader(Path path) {
        return createXMLEventReader(path.toFile());
    }
    private static XMLEventReader createXMLEventReader(File file) {
        try (FileReader r = new FileReader(file)) {
            return createXMLEventReader(r);
        } catch (IOException e) {
            throw new XMLException(
                    "Could not stream XML file " + file.getAbsolutePath(), e);
        }
    }
    private static XMLEventReader createXMLEventReader(Node node) {
        return createXMLEventReader(new XML(node));
    }
    private static XMLEventReader createXMLEventReader(XML xml) {
        return createXMLEventReader(xml.toString());
    }
    private static XMLEventReader createXMLEventReader(String xml) {
        return createXMLEventReader(new StringReader(xml));
    }
    private static XMLEventReader createXMLEventReader(InputStream is) {
        return createXMLEventReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
    }
    private static XMLEventReader createXMLEventReader(Reader reader) {
        try {
            XMLInputFactory factory = XMLUtil.createXMLInputFactory();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
            return factory.createXMLEventReader(IOUtils.buffer(reader));
        } catch (XMLStreamException e) {
            throw new XMLException("Could not create XMLEventReader.", e);
        }
    }

    private static <T> void set(T t, FailableConsumer<T, Exception> c) {
        try {
            c.accept(t);
        } catch (Exception e) {
            String clsMsg = t.getClass() + e.getMessage();
            if (!alreadyLogged.contains(clsMsg)) {
                LOG.debug(e.getMessage());
                alreadyLogged.add(clsMsg);
            }
        }
    }
}
