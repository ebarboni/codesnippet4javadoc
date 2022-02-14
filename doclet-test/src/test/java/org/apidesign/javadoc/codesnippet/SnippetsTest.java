/**
 * Codesnippet Javadoc Doclet
 * Copyright (C) 2015-2020 Jaroslav Tulach - jaroslav.tulach@apidesign.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.0 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-3.0.
 */
package org.apidesign.javadoc.codesnippet;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

public class SnippetsTest {

    public SnippetsTest() {
    }

    @Test public void testMissingMethodInAnInterfaceIsDetected() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: xyz\n"
            + "public interface I {\n"
            + "// FINISH: xyz\n"
            + "  public void get();\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals("<b>public</b> <b>interface</b> {@link ahoj.I} {\n}\n", r);
    }

    @Test public void testStartEnd() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// @start region=\"xyz\"\n"
            + "public interface I {\n"
            + "// @end region=\"xyz\"\n"
            + "  public void get();\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals("<b>public</b> <b>interface</b> {@link ahoj.I} {\n", r);
    }

    @Test
    public void properIndentationForMultipleFinish() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: xyz\n"
            + "public interface I {\n"
            + "  public void call() {\n"
            + "// FINISH: xyz\n"
            + "  }\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals(""
            + "<b>public</b> <b>interface</b> {@link ahoj.I} {\n"
            + "  <b>public</b> <b>void</b> call() {\n"
            + "  }\n"
            + "}\n"
            + "", r
        );
    }

    @Test public void testJavaLangImportRecognized() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public interface I {\n"
            + "// BEGIN: xyz\n"
            + "  public String get();\n"
            + "// FINISH: xyz\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals("<b>public</b> {@link java.lang.String} get();\n", r);
    }

    @Test public void testImportRecognized() throws Exception {
        String c1
            = "package ahoj;\n"
            + "import java.io.File;\n"
            + "public interface I {\n"
            + "// BEGIN: xyz\n"
            + "  public File get();\n"
            + "// FINISH: xyz\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals("<b>public</b> {@link java.io.File} get();\n", r);
    }

    @Test public void testStarImportRecognized() throws Exception {
        String c1
            = "package ahoj;\n"
            + "import java.io.*;\n"
            + "public interface I {\n"
            + "// BEGIN: xyz\n"
            + "  public File get();\n"
            + "// FINISH: xyz\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");

        assertEquals("<b>public</b> {@link java.io.File} get();\n", r);
    }

    @Test public void testSpacesAtBeginingAreStripped() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: xyz\n"
            + "   public interface I {\n"
            + "     public void ahoj();\n"
            + "   }\n"
            + "// END: xyz\n"
            + "  public void get();\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");
        String result = "<b>public</b> <b>interface</b> {@link ahoj.I} {\n"
            + "  <b>public</b> <b>void</b> ahoj();\n"
            + "}\n";
        assertEquals(result, r);
    }

    @Test public void testReportUnpairedBracesAsError() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: xyz\n"
            + "   public interface I {\n"
            + "     public void ahoj();\n"
            + "// END: xyz\n"
            + "   }\n"
            + "  public void get();\n"
            + "}"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());

        try {
            String r = snippets.getSnippet(null).findGlobalSnippet(null, "xyz");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("not paired"));
            return;
        }
        fail("The execution of the script shall fail");
    }

    @Test public void testIncludedTexts() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: clazz\n"
            + "public interface I {\n"
            + "  // BEGIN: method\n"
            + "  public void get();\n"
            + "  // END: method\n"
            + "}\n"
            + "// END: clazz\n"
            + "";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "clazz");

        if (r.indexOf("BEGIN") >= 0) {
            fail("BEGIN is there: " + r);
        }
        if (r.indexOf("END") >= 0) {
            fail("END is there: " + r);
        }
        if (r.indexOf("<b>interface</b> {@link ahoj.I}") < 0) {
            fail("Missing interface: " + r);
        }
        if (r.indexOf("<b>void</b> get()") < 0) {
            fail("Missing get: " + r);
        }
    }

    @Test public void testIncludedTextsAmper() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public class C {\n"
            + "  // BEGIN: method\n"
            + "  public void change(int x) { x &= 10; }\n"
            + "  // END: method\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "method");

        if (r.indexOf("&=") >= 0) {
            fail("Wrong XML: " + r);
        }
        if (r.indexOf("&amp;=") == -1) {
            fail("Wrong XML, we need &amp;: " + r);
        }
    }

    @Test public void testIncludedTextsZav() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public class C {\n"
            + "  // BEGIN: method\n"
            + "  @Deprecated\n"
            + "  public void change(int x) { x &= 10; }\n"
            + "  // END: method\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "method");

        if (r.indexOf("&=") >= 0) {
            fail("Wrong XML: " + r);
        }
        if (r.indexOf("&amp;=") == -1) {
            fail("Wrong XML, we need &amp;: " + r);
        }
        if (r.indexOf("@D") >= 0) {
            fail("No @ in XML: " + r);
        }
        if (r.indexOf("&#064;") == -1) {
            fail("Wrong XML, we need &amp;: " + r);
        }
    }

    @Test public void testIncludedTextsAmperAndGenerics() throws Exception {
        String c1
            = "package org.apidesign.api.security;\n"
            + ""
            + "import java.nio.ByteBuffer;\n"
            + "import java.util.ServiceLoader;\n"
            + "import org.apidesign.spi.security.Digestor;\n"
            + "\n"
            + "/** Simplified version of a Digest class that allows to compute a fingerprint\n"
            + " * for buffer of data.\n"
            + " *\n"
            + " * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>\n"
            + " */\n"
            + "// BEGIN: day.end.bridges.Digest\n"
            + "public final class Digest {\n"
            + "    private final DigestImplementation<?> impl;\n"
            + "    \n"
            + "    /** Factory method is better than constructor */\n"
            + "    private Digest(DigestImplementation<?> impl) {\n"
            + "        this.impl = impl;\n"
            + "    }\n"
            + "    \n"
            + "    /** Factory method to create digest for an algorithm.\n"
            + "     */\n"
            + "    public static Digest getInstance(String algorithm) {\n"
            + "        for (Digestor<?> digestor : ServiceLoader.load(Digestor.class)) {\n"
            + "            DigestImplementation<?> impl = \n"
            + "DigestImplementation.create(digestor, algorithm);\n"
            + "            if (impl != null) {\n"
            + "                return new Digest(impl);\n"
            + "            }\n"
            + "        }\n"
            + "        throw new IllegalArgumentException(algorithm);\n"
            + "    }\n"
            + "      \n"
            + "    //\n"
            + "    // these methods are kept the same as in original MessageDigest,\n"
            + "    // but for simplicity choose just some from the original API\n"
            + "    //\n"
            + "    \n"
            + "    public byte[] digest(ByteBuffer bb) {\n"
            + "        return impl.digest(bb);\n"
            + "    }\n"
            + "}\n"
            + "// END: day.end.bridges.Digest\n";

        Path src = createPath(1, "C.java", c1);


        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "day.end.bridges.Digest");

        if (r.indexOf("&=") >= 0) {
            fail("Wrong XML: " + r);
        }
        if (r.indexOf("&amp;") > -1) {
            fail("Wrong XML, no &amp;: " + r);
        }
    }

    @Test public void testLineCommentsInItalic() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public class C {\n"
            + "  // BEGIN: comment\n"
            + "  class Comment {\n"
            + "    // no keywords for this comment\n"
            + "    private int x;\n"
            + "  }\n"
            + "  // END: comment\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "comment");

        String assume = ""
            + "<b>class</b> Comment {\n"
            + "  <em>// no keywords for this comment</em>\n"
            + "  <b>private</b> <b>int</b> x;\n"
            + "}\n";

        assertEquals(assume, r);
    }

    @Test public void testStrings() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public class C {\n"
            + "  // BEGIN: str\n"
            + "  public String str = \"this is my long string\";\n"
            + "  private int length = str.length();\n"
            + "  // END: str\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "str");

        String assume = ""
            + "<b>public</b> {@link java.lang.String} str = <em>\"this is my long string\"</em>;\n"
            + "<b>private</b> <b>int</b> length = str.length();\n"
            + "";

        assertEquals(assume, r);
    }

    @Test public void testConcatenateStrings() throws Exception {
        String c1
            = "package ahoj;\n"
            + "import net.java.html.js.JavaScriptBody;\n"
            + "public class C {\n"
            + "  private static String space = \" \";\n"
            + "  // BEGIN: str\n"
            + "  private static String str = \"Jaroslav\" + space + \"Tulach\";\n"
            + "  // END: str\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "str");

        String assume = ""
            + "<b>private</b> <b>static</b> {@link java.lang.String} str = <em>\"Jaroslav\"</em> + space + <em>\"Tulach\"</em>;\n"
            + "";

        assertEquals(assume, r);
    }

    @Test public void testStringsWithSpecialCharacters() throws Exception {
        String c1
            = "package ahoj;\n"
            + "public class C {\n"
            + "  // BEGIN: str\n" +
"        Source src = Source.newBuilder(\"\\n\"\n" +
"            + \"(function() {\\n\"\n" +
"            + \"  var seconds = 0;\\n\"\n" +
"            + \"  function addTime(h, m, s) {\\n\"\n" +
"            + \"    seconds += 3600 * h;\\n\"\n" +
"            + \"    seconds += 60 * m;\\n\"\n" +
"            + \"    seconds += s;\\n\"\n" +
"            + \"  }\\n\"\n" +
"            + \"  function time() {\\n\"\n" +
"            + \"    return seconds;\\n\"\n" +
"            + \"  }\\n\"\n" +
"            + \"  return {\\n\"\n" +
"            + \"    'addTime': addTime,\\n\"\n" +
"            + \"    'timeInSeconds': time\\n\"\n" +
"            + \"  }\\n\"\n" +
"            + \"})\\n\"\n" +
"        ).build();\n" +
""
            + "  // END: str\n"
            + "}\n"
            + "";
        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "str");

        int at = -1;
        int counter = 0;
        for (;;) {
            at = r.indexOf("</em>", 1 + at);
            if (at == -1) {
                break;
            }
            final String msg = "New line at " + at + ":" + r.substring(at - 5);
            assertEquals(msg, '"', r.charAt(at - 1));
            assertEquals(msg, 'n', r.charAt(at - 2));
            assertEquals(msg, '\\', r.charAt(at - 3));
            counter++;
        }

        assertTrue("Expecting at least ten lines: " + counter, counter > 10);
    }

    @Test public void testInXML() throws Exception {
        String c1
            = "<!-- BEGIN: clazz -->\n"
            + "<interface name='I'/>\n"
            + "<!-- END: clazz -->\n"
            + "";
        Path src = createPath(1, "I.xml", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "clazz");

        if (r.indexOf("BEGIN") >= 0) {
            fail("BEGIN is there: " + r);
        }
        if (r.indexOf("END") >= 0) {
            fail("END is there: " + r);
        }
        if (r.indexOf("&lt;interface name='I'/&gt;") < 0) {
            fail("Missing interface: " + r);
        }
    }

    @Test public void testLongLineNotDetectedAsBeginsWithGen() throws Exception {
        String c1
            = "package org.apidesign.api.security;\n"
            + ""
            + "import java.nio.ByteBuffer;\n"
            + "import java.util.ServiceLoader;\n"
            + "import org.apidesign.spi.security.Digestor;\n"
            + "\n"
            + "/** Simplified version of a Digest class that allows to compute a fingerprint\n"
            + "// BEGIN: x\n"
            + " * for buffer of data.\n"
            + "// END: x\n"
            + " *\n"
            + " * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>\n"
            + " */\n"
            + "// GEN-BEGIN: day.end.bridges.Digest\n"
            + "d;   DigestImplementation<?> impl = DigestImplementation.create(digestor, algorithm);\n"
            + "// GEN-END: day.end.bridges.Digest\n";

        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "x");

        assertNotNull(r);
    }

    @Test public void testLongLineDetected() throws Exception {
        String c1
            = "package org.apidesign.api.security;\n"
            + ""
            + "import java.nio.ByteBuffer;\n"
            + "import java.util.ServiceLoader;\n"
            + "import org.apidesign.spi.security.Digestor;\n"
            + "\n"
            + "/** Simplified version of a Digest class that allows to compute a fingerprint\n"
            + " * for buffer of data.\n"
            + " *\n"
            + " * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>\n"
            + " */\n"
            + "// BEGIN: day.end.bridges.Digest\n"
            + "d;   DigestImplementation<?> impl = DigestImplementation.create(digestor, algorithm);\n"
            + "// END: day.end.bridges.Digest\n";

        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());

        try {
            String r = snippets.getSnippet(null).findGlobalSnippet(null, "day.end.bridges.Digest");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().startsWith("Line is too long"));
            // OK
            return;
        }
        fail("Should fail, as there is long line");
    }

    @Test public void testLongNotLineDetectedAsShortened() throws Exception {
        String c1
            = "package org.apidesign.api.security;\n"
            + ""
            + "import java.nio.ByteBuffer;\n"
            + "import java.util.ServiceLoader;\n"
            + "import org.apidesign.spi.security.Digestor;\n"
            + "\n"
            + "/** Simplified version of a Digest class that allows to compute a fingerprint\n"
            + " * for buffer of data.\n"
            + " *\n"
            + " * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>\n"
            + " */\n"
            + "// BEGIN: day.end.bridges.Digest\n"
            + "   DigestImplementation<?> impl = DigestImplementation.create(digestor, algorithm);\n"
            + "// END: day.end.bridges.Digest\n";

        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "day.end.bridges.Digest");

        assertNotNull(r);
    }

    @Test public void testNotClosedSection() throws Exception {
        String c1
            = "package ahoj;\n"
            + "// BEGIN: clazz\n"
            + "int x;\n"
            + "\n";
        Path src = createPath(1, "I.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());

        try {
            String r = snippets.getSnippet(null).findGlobalSnippet(null, "clazz");
            fail("Has to fail");
        } catch (IllegalStateException ex) {
            // ok
            assertTrue(ex.getMessage(), ex.getMessage().contains("closed"));
        }
    }

    @Test public void testLongNotLineDetectedAsNotInTheList() throws Exception {
        String c1
            = "package org.apidesign.api.security;\n"
            + ""
            + "import java.nio.ByteBuffer;\n"
            + "import java.util.ServiceLoader;\n"
            + "import org.apidesign.spi.security.Digestor;\n"
            + "\n"
            + "/** Simplified version of a Digest class that allows to compute a fingerprint\n"
            + " * for buffer of data.\n"
            + " *\n"
            + " * @author Jaroslav Tulach <jaroslav.tulach@apidesign.org>\n"
            + " */\n"
            + "   DigestImplementation<?> impl    =      DigestImplementation.create(digestor, algorithm);\n"
            + "// BEGIN: day.end.bridges.Digest\n"
            + "   DigestImplementation<?> impl    =      null\n"
            + "// END: day.end.bridges.Digest\n";

        Path src = createPath(1, "C.java", c1);

        Snippets snippets = new Snippets(null);
        addPath(snippets, src.getParent());
        String r = snippets.getSnippet(null).findGlobalSnippet(null, "day.end.bridges.Digest");

        assertNotNull(r);
    }

    private static int cnt;
    protected final Path createPath(int slot, String name, String content) throws Exception {
        FileSystem fs = MemoryFileSystemBuilder.newEmpty().
            build("snippets" + ++cnt);
        Path file = fs.getPath("dir" + slot, name);
        Files.createDirectories(file.getParent());
        BufferedWriter w = Files.newBufferedWriter(file, Charset.defaultCharset());
        w.append(content);
        w.close();
        return file;
    }

    private static void addPath(Snippets snippets, Path parent) {
        snippets.addPath(parent, true);
    }

}
