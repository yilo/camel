/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.support;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;


/**
 *
 */
public class XMLTokenExpressionIteratorTest extends TestCase {
    private static final byte[] TEST_BODY = 
        ("<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='1' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='a' anotherAttr='a'></c:child>"
            + "<c:child some_attr='b' anotherAttr='b'/>"
            + "</c:parent>"
            + "<c:parent some_attr='2' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='c' anotherAttr='c'></c:child>"
            + "<c:child some_attr='d' anotherAttr='d'/>"
            + "</c:parent>"
            + "</grandparent>"
            + "<grandparent><uncle>ben</uncle><aunt/>"
            + "<c:parent some_attr='3' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='e' anotherAttr='e'></c:child>"
            + "<c:child some_attr='f' anotherAttr='f'/>"
            + "</c:parent>"
            + "</grandparent>"
            + "</g:greatgrandparent>").getBytes();
    
    private static final String[] RESULTS_CHILD_WRAPPED = {
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='1' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='a' anotherAttr='a'></c:child>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='1' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='b' anotherAttr='b'/>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='2' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='c' anotherAttr='c'></c:child>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='2' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='d' anotherAttr='d'/>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle>ben</uncle><aunt/>"
            + "<c:parent some_attr='3' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='e' anotherAttr='e'></c:child>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle>ben</uncle><aunt/>"
            + "<c:parent some_attr='3' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='f' anotherAttr='f'/>"
            + "</c:parent></grandparent></g:greatgrandparent>"
    };

    private static final String[] RESULTS_CHILD = {
        "<c:child some_attr='a' anotherAttr='a' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"></c:child>",
        "<c:child some_attr='b' anotherAttr='b' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"/>",
        "<c:child some_attr='c' anotherAttr='c' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"></c:child>",
        "<c:child some_attr='d' anotherAttr='d' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"/>",
        "<c:child some_attr='e' anotherAttr='e' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"></c:child>",
        "<c:child some_attr='f' anotherAttr='f' xmlns:g=\"urn:g\" xmlns:d=\"urn:d\" xmlns:c=\"urn:c\"/>"
    };

    private static final String[] RESULTS_PARENT_WRAPPED = {
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='1' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='a' anotherAttr='a'></c:child>"
            + "<c:child some_attr='b' anotherAttr='b'/>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "<c:parent some_attr='2' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='c' anotherAttr='c'></c:child>"
            + "<c:child some_attr='d' anotherAttr='d'/>"
            + "</c:parent></grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle>ben</uncle><aunt/>"
            + "<c:parent some_attr='3' xmlns:c='urn:c' xmlns:d=\"urn:d\">"
            + "<c:child some_attr='e' anotherAttr='e'></c:child>"
            + "<c:child some_attr='f' anotherAttr='f'/>"
            + "</c:parent></grandparent></g:greatgrandparent>",
    };
    
    private static final String[] RESULTS_PARENT = {
        "<c:parent some_attr='1' xmlns:c='urn:c' xmlns:d=\"urn:d\" xmlns:g='urn:g'>"
            + "<c:child some_attr='a' anotherAttr='a'></c:child>"
            + "<c:child some_attr='b' anotherAttr='b'/>"
            + "</c:parent>",
        "<c:parent some_attr='2' xmlns:c='urn:c' xmlns:d=\"urn:d\" xmlns:g='urn:g'>"
            + "<c:child some_attr='c' anotherAttr='c'></c:child>"
            + "<c:child some_attr='d' anotherAttr='d'/>"
            + "</c:parent>",
        "<c:parent some_attr='3' xmlns:c='urn:c' xmlns:d=\"urn:d\" xmlns:g='urn:g'>"
            + "<c:child some_attr='e' anotherAttr='e'></c:child>"
            + "<c:child some_attr='f' anotherAttr='f'/>"
            + "</c:parent>",
    };
    
    private static final String[] RESULTS_AUNT_WRAPPED = {
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle/><aunt>emma</aunt>"
            + "</grandparent></g:greatgrandparent>",
        "<?xml version='1.0' encoding='UTF-8'?>"
            + "<g:greatgrandparent xmlns:g='urn:g'><grandparent><uncle>ben</uncle><aunt/>"
            + "</grandparent></g:greatgrandparent>"
    };    
    
    private static final String[] RESULTS_AUNT = {
        "<aunt xmlns:g=\"urn:g\">emma</aunt>",
        "<aunt xmlns:g=\"urn:g\"/>"
    };    
    

    private Map<String, String> nsmap;
    private Exchange exchange;

    @Override
    protected void setUp() throws Exception {
        nsmap = new HashMap<String, String>();
        nsmap.put("G", "urn:g");
        nsmap.put("C", "urn:c");
        
        exchange = ExchangeBuilder.anExchange(new DefaultCamelContext()).build();
    }


    public void testExtractChild() throws Exception {
        invokeAndVerify("//C:child", true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildInjected() throws Exception {
        invokeAndVerify("//C:child", false, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD);
    }

    public void testExtractChildWithAncestorGGPdGP() throws Exception {
        invokeAndVerify("/G:greatgrandparent/grandparent//C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildWithAncestorGGPdP() throws Exception {
        invokeAndVerify("/G:greatgrandparent//C:parent/C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildWithAncestorGPddP() throws Exception {
        invokeAndVerify("//grandparent//C:parent/C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildWithAncestorGPdP() throws Exception {
        invokeAndVerify("//grandparent/C:parent/C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildWithAncestorP() throws Exception {
        invokeAndVerify("//C:parent/C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }

    public void testExtractChildWithAncestorGGPdGPdP() throws Exception {
        invokeAndVerify("/G:greatgrandparent/grandparent/C:parent/C:child", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_CHILD_WRAPPED);
    }
    
    public void textExtractParent() throws Exception {
        invokeAndVerify("//C:parent", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_PARENT_WRAPPED);
    }
    
    public void textExtractParentInjected() throws Exception {
        invokeAndVerify("//C:parent", 
                        false, new ByteArrayInputStream(TEST_BODY), RESULTS_PARENT);
    }
    
    public void textExtractAunt() throws Exception {
        invokeAndVerify("//aunt", 
                        true, new ByteArrayInputStream(TEST_BODY), RESULTS_AUNT_WRAPPED);
    }

    public void textExtractAuntInjected() throws Exception {
        invokeAndVerify("//aunt", 
                        false, new ByteArrayInputStream(TEST_BODY), RESULTS_AUNT);
    }

    private void invokeAndVerify(String path, boolean wrap, InputStream in, String[] expected) throws Exception {
        XMLTokenExpressionIterator xtei = new XMLTokenExpressionIterator(path, wrap);
        xtei.setNamespaces(nsmap);
        
        Iterator<?> it = xtei.createIterator(in, exchange);
        List<String> results = new ArrayList<String>();
        while (it.hasNext()) {
            results.add((String)it.next());
        }
        ((Closeable)it).close();

        assertEquals("token count", expected.length, results.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("mismatch [" + i + "]", expected[i], results.get(i));
        }

    }
}