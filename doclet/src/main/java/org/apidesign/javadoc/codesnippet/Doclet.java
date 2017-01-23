/**
 * Codesnippet Javadoc Doclet
 * Copyright (C) 2015-2016 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
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

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.formats.html.HtmlDoclet;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public final class Doclet {
    private static Snippets snippets;
    private Doclet() {
    }
    public static boolean start(RootDoc root) {
        for (ClassDoc clazz : root.classes()) {
            snippets.fixCodesnippets(root, clazz);
            for (MethodDoc method : clazz.methods()) {
                snippets.fixCodesnippets(clazz, method);
            }
            for (FieldDoc field : clazz.fields()) {
                snippets.fixCodesnippets(clazz, field);
            }
            for (ConstructorDoc con : clazz.constructors()) {
                snippets.fixCodesnippets(clazz, con);
            }
        }
        for (PackageDoc pkg : root.specifiedPackages()) {
            snippets.fixCodesnippets(root, pkg);
        }
        return HtmlDoclet.start(hideElements(RootDoc.class, root));
    }

    public static int optionLength(String option) {
        if (option.equals("-snippetpath")) {
            return 2;
        }
        if (option.equals("-snippetclasses")) {
            return 2;
        }
        if (option.equals("-maxLineLength")) {
            return 2;
        }
        if (option.equals("-verifysincepresent")) {
            return 1;
        }
        if (option.equals("-verifysince")) {
            return 2;
        }
        if (option.equals("-hiddingannotation")) {
            return 2;
        }
        return HtmlDoclet.optionLength(option);
    }

    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        snippets = new Snippets(reporter);
        for (String[] optionAndParams : options) {
            Boolean visible = null;
            if (optionAndParams[0].equals("-sourcepath")) {
                visible = true;
            }
            if (optionAndParams[0].equals("-snippetpath")) {
                visible = false;
            }
            if (visible != null) {
                for (int i = 1; i < optionAndParams.length; i++) {
                    for (String elem : optionAndParams[i].split(File.pathSeparator)) {
                        snippets.addPath(new File(elem).toPath(), visible);
                    }
                }
            }
            if (optionAndParams[0].equals("-snippetclasses")) {
                for (int i = 1; i < optionAndParams.length; i++) {
                    snippets.addClasses(optionAndParams[i]);
                }
            }
            if (optionAndParams[0].equals("-maxLineLength")) {
                if ( optionAndParams.length > 1 ) {
                    snippets.setMaxLineLength( optionAndParams[1] );
                }
            }
            if (
                optionAndParams[0].equals("-verifysincepresent") ||
                optionAndParams[0].equals("-verifysince")
            ) {
                if ( optionAndParams.length > 1 ) {
                    snippets.setVerifySince(optionAndParams[1]);
                } else {
                    snippets.setVerifySince("");
                }
            }
            if (
                optionAndParams[0].equals("-hiddingannotation")
            ) {
                snippets.addHiddenAnnotation(optionAndParams[1]);
            }
        }
        return HtmlDoclet.validOptions(options, reporter);
    }

    public static LanguageVersion languageVersion() {
        return HtmlDoclet.languageVersion();
    }

    private static <T> T hideElement(Class<T> clazz, final Object obj) {
        return hideElements(clazz, clazz.cast(obj));
    }

    private static <T> T hideElements(Class<T> clazz, final T obj) {
        if (clazz == MethodDoc.class || clazz == FieldDoc.class) {
            return obj;
        }
        if (obj instanceof ExecutableMemberDoc) {
            return obj;
        }
        Class<?> c = clazz;
        if (clazz.isAssignableFrom(ClassDoc.class)) {
            if (obj instanceof ClassDoc && ((ClassDoc) obj).isAnnotationType()) {
                c = AnnotationTypeDoc.class;
            }
        }
        InvocationHandler h = new DocProxy(obj);
        return clazz.cast(Proxy.newProxyInstance(obj.getClass().getClassLoader(), new Class[]{c}, h));
    }

    private static class DocProxy<T> implements InvocationHandler {
        private final T obj;

        public DocProxy(T obj) {
            this.obj = obj;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && (
                method.getName().equals("equals") ||
                method.getName().equals("compareTo")
            )) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        continue;
                    }
                    InvocationHandler handler = null;
                    try {
                        handler = Proxy.getInvocationHandler(args[i]);
                    } catch (IllegalArgumentException ignore) {
                        continue;
                    }
                    if (handler instanceof DocProxy) {
                        args[i] = ((DocProxy)handler).obj;
                    }
                }
            }

            boolean doSkip = true;
            if (method.getName().equals("allClasses")) {
                doSkip = false;
            }

            Object ret = method.invoke(obj, args);
            final Class<?> requestedType = method.getReturnType();
            if (requestedType.isArray()) {
                final Class<?> componentType = requestedType.getComponentType();
                if (componentType.getPackage() == RootDoc.class.getPackage()) {
                    Object[] arr = (Object[]) ret;
                    List<Object> copy = new ArrayList<>();
                    for (Object element : arr) {
                        boolean skip = false;
                        for (AnnotationDesc desc : findAnnotations(element)) {
                            String name = desc.annotationType().qualifiedName();
                            if (snippets.isHiddingAnnotation(name)) {
                                skip = doSkip;
                                break;
                            }
                        }
                        if (!skip) {
                            copy.add(hideElement(componentType, element));
                        }
                    }
                    Object[] reqArr = (Object[])Array.newInstance(requestedType.getComponentType(), 0);
                    return copy.toArray(reqArr);
                }
            }
            return ret;
        }

        private AnnotationDesc[] findAnnotations(Object element) {
            if (element instanceof ProgramElementDoc) {
                ProgramElementDoc ped = (ProgramElementDoc) element;
                return ped.annotations();
            }
            if (element instanceof PackageDoc) {
                return ((PackageDoc) element).annotations();
            }
            return new AnnotationDesc[0];
        }
    }
}
