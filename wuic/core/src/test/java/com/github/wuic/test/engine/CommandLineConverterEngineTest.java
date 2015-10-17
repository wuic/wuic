package com.github.wuic.test.engine;


import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class CommandLineConverterEngineTest {

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Typescript compilation test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void compileTest() throws Exception {
        final String command = String.format("echo %s > %s | echo %s > %s",
                CommandLineConverterEngine.PATH_TOKEN,
                CommandLineConverterEngine.OUT_PATH_TOKEN,
                "{"
                        + "  \"version\": 3,"
                        + "  \"file\": \"testcode.js\","
                        + "  \"sections\": ["
                        + "    {"
                        + "      \"map\": {"
                        + "         \"version\": 3,"
                        + "         \"mappings\": \"AAAAA,QAASA,UAAS,EAAG;\","
                        + "         \"sources\": [\"testcode.js\"],"
                        + "         \"names\": [\"foo\"]"
                        + "      },"
                        + "      \"offset\": {"
                        + "        \"line\": 1,"
                        + "        \"column\": 1"
                        + "      }"
                        + "    }"
                        + "  ]"
                        + "}",
                CommandLineConverterEngine.SOURCE_MAP_TOKEN);
        final File parent = temporaryFolder.newFolder("parent");
        NutsHeap heap = mockHeap(parent);
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, CommandLineConverterEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("CommandLineConverterEngineBuilder");
        final Engine engine = builder.property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, command)
                .build();

        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null).processContext(ProcessContext.DEFAULT).contextPath("cp").build());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        final String content = IOUtils.readString(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray())));
        Assert.assertTrue(content, content.contains("foo.less"));
        Assert.assertTrue(content, content.contains("bar.less"));
    }

    /**
     * Bad command execution test.
     *
     * @throws java.io.IOException if test succeed
     * @throws com.github.wuic.exception.WuicException if test fails
     */
    @Test(expected = IOException.class)
    public void convertErrorTest() throws Exception {
        final String command = String.format("unknown %s %s %s ",
                CommandLineConverterEngine.PATH_TOKEN,
                CommandLineConverterEngine.OUT_PATH_TOKEN,
                CommandLineConverterEngine.SOURCE_MAP_TOKEN);
        final File parent = temporaryFolder.newFolder("parent");
        NutsHeap heap = mockHeap(parent);
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, CommandLineConverterEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("CommandLineConverterEngineBuilder");
        final Engine engine = builder.property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, command)
                .build();

        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null).processContext(ProcessContext.DEFAULT).contextPath("cp").build());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
    }

    /**
     * Bad configuration test.
     */
    @Test
    public void badConfTokenTest() {
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, CommandLineConverterEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("CommandLineConverterEngineBuilder");

        Assert.assertNull(builder.property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, "unknown")
                .build());

        Assert.assertNull(builder.property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, String.format("unknown %s", CommandLineConverterEngine.PATH_TOKEN))
                .build());

        Assert.assertNull(builder.property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, String.format("unknown %s %s", CommandLineConverterEngine.PATH_TOKEN, CommandLineConverterEngine.OUT_PATH_TOKEN))
                .build());
    }

    /**
     * <p>
     * Mocks a new heap.
     * </p>
     *
     * @param parent where nut could be stored
     * @throws Exception if mock fails
     * @return the mock
     */
    private NutsHeap mockHeap(final File parent) throws Exception {
        final Nut nut1 = mock(Nut.class);
        when(nut1.getInitialName()).thenReturn("foo.less");
        when(nut1.openStream()).thenReturn(new ByteArrayInputStream("foo".getBytes()));

        // Value must be different for each test
        when(nut1.getVersionNumber()).thenReturn(new FutureLong(0L));
        when(nut1.getInitialNutType()).thenReturn(NutType.LESS);

        IOUtils.copyStream(new ByteArrayInputStream("bar".getBytes()), new FileOutputStream(new File(parent, "bar.less")));
        final Nut nut2 = mock(Nut.class);
        when(nut2.getParentFile()).thenReturn(parent.getAbsolutePath());
        when(nut2.getInitialName()).thenReturn("bar.less");
        when(nut2.openStream()).thenReturn(new ByteArrayInputStream("bar".getBytes()));
        when(nut2.getVersionNumber()).thenReturn(new FutureLong(0L));
        when(nut2.getInitialNutType()).thenReturn(NutType.LESS);

        final NutsHeap heap = mock(NutsHeap.class);
        when(heap.getNuts()).thenReturn(Arrays.asList(nut1, nut2));

        return heap;
    }
}
