/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package com.sun.jna;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import junit.framework.TestCase;

public class JNAUnloadTest extends TestCase {
    
    private static final String BUILDDIR =
        System.getProperty("jna.builddir", "build"
                           + (Platform.is64Bit() ? "-d64" : ""));

    private static class TestLoader extends URLClassLoader {
        public TestLoader(boolean fromJar) throws MalformedURLException {
            super(new URL[] {
                new File(BUILDDIR + (fromJar ? "/jna.jar" : "/classes")).toURI().toURL(),
            }, null);
        }
    }

    public void testUnloadFromJar() throws Exception {
        ClassLoader loader = new TestLoader(true);
        Class cls = Class.forName("com.sun.jna.Native", true, loader);
        assertEquals("Wrong class loader", loader, cls.getClassLoader());

        Field field = cls.getDeclaredField("nativeLibraryPath");
        field.setAccessible(true);
        String path = (String)field.get(null);
        field = null;
        assertTrue("Native library not unpacked from jar: " + path,
                   path.startsWith(System.getProperty("java.io.tmpdir")));

        WeakReference ref = new WeakReference(cls);
        WeakReference clref = new WeakReference(loader);
        loader = null;
        cls = null;
        System.gc();
        assertNull("Class not GC'd: " + ref.get(), ref.get());
        assertNull("ClassLoader not GC'd: " + clref.get(), clref.get());
        File f = new File(path);
        for (int i=0;i < 100 && f.exists();i++) {
            Thread.sleep(1);
            if ("".equals(System.getProperty("jna.native.library.path")))
                break;
        }
        assertFalse("Temporary native library not deleted: " + path,
                    f.exists());

        try {
            loader = new TestLoader(false);
            cls = Class.forName("com.sun.jna.Native", true, loader);
        }
        catch(Throwable t) {
            fail("Native library not unloaded: " + t.getMessage());
        }
        finally {
            loader = null;
            cls = null;
            System.gc();
        }
    }

    public void testUnload() throws Exception {
        ClassLoader loader = new TestLoader(false);
        Class cls = Class.forName("com.sun.jna.Native", true, loader);
        assertEquals("Wrong class loader", loader, cls.getClassLoader());

        Field field = cls.getDeclaredField("nativeLibraryPath");
        field.setAccessible(true);
        String path = (String)field.get(null);
        field = null;
        assertNotNull("Native library not found", path);

        WeakReference ref = new WeakReference(cls);
        WeakReference clref = new WeakReference(loader);
        loader = null;
        cls = null;
        System.gc();
        for (int i=0;i < 100 && (ref.get() != null || clref.get() != null);i++) {
            Thread.sleep(1);
        }
        for (int i=0;i < 100;i++) {
            Thread.sleep(1);
            if ("".equals(System.getProperty("jna.native.library.path")))
                break;
        }
        assertNull("Class not GC'd: " + ref.get(), ref.get());
        assertNull("ClassLoader not GC'd: " + clref.get(), clref.get());

        try {
            loader = new TestLoader(false);
            cls = Class.forName("com.sun.jna.Native", true, loader);
        }
        catch(Throwable t) {
            fail("Native library not unloaded: " + t.getMessage());
        }
        finally {
            loader = null;
            cls = null;
            System.gc();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JNAUnloadTest.class);
    }
}
