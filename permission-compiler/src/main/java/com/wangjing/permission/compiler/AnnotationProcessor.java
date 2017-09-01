package com.wangjing.permission.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.wangjing.permission.annotations.PermissionsCustomRationale;
import com.wangjing.permission.annotations.PermissionsDenied;
import com.wangjing.permission.annotations.PermissionsGranted;
import com.wangjing.permission.annotations.PermissionsRationale;
import com.wangjing.permission.annotations.PermissionsRequestSync;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    private Elements mUtils;
    private Filer mFiler;
    private Map<String, ProxyInfo> map = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mUtils = processingEnvironment.getElementUtils();
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        set.add(PermissionsGranted.class.getCanonicalName());
        set.add(PermissionsDenied.class.getCanonicalName());
        set.add(PermissionsRationale.class.getCanonicalName());
        set.add(PermissionsCustomRationale.class.getCanonicalName());
        set.add(PermissionsRequestSync.class.getCanonicalName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        map.clear();
        if (!isAnnotatedWithClass(roundEnv, PermissionsRequestSync.class)) return false;
        if (!isAnnotatedWithMethod(roundEnv, PermissionsGranted.class)) return false;
        if (!isAnnotatedWithMethod(roundEnv, PermissionsDenied.class)) return false;
        if (!isAnnotatedWithMethod(roundEnv, PermissionsRationale.class)) return false;
        if (!isAnnotatedWithMethod(roundEnv, PermissionsCustomRationale.class)) return false;


        for (ProxyInfo info : map.values()) {
            try {
                JavaFileObject file = mFiler.createSourceFile(info.getProxyName(),info.getElement());
                Writer writer = file.openWriter();
                info.generateJavaFile(writer);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private boolean isAnnotatedWithClass(RoundEnvironment roundEnv, Class<? extends Annotation> clazz) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(clazz);
        for (Element element : elements) {
            if (isValid(element)) {
                return false;
            }
            TypeElement typeElement = (TypeElement) element;
            String className = typeElement.getQualifiedName().toString();
            ProxyInfo info = map.get(className);
            if (info == null) {
                info = new ProxyInfo(mUtils, typeElement);
                map.put(className, info);
            }
            Annotation anno = element.getAnnotation(clazz);
            if (anno instanceof PermissionsRequestSync) {
                String[] permissions = ((PermissionsRequestSync) anno).permissions();
                int[] value = ((PermissionsRequestSync) anno).value();
                if (permissions.length != value.length) {
                    error(typeElement, "permission's length not equals value's length");
                    return false;
                }
                info.syncPermissions.put(value, permissions);
            } else {
                error(element, "%s not support ", element);
            }
        }
        return true;
    }

    private boolean isAnnotatedWithMethod(RoundEnvironment roundEnv, Class<? extends Annotation> clazz) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(clazz);
        for (Element element : elements) {
            if (isValid(element)) return false;
            ExecutableElement method = (ExecutableElement) element;
            TypeElement typeElement = (TypeElement) method.getEnclosingElement();
            String className = typeElement.getQualifiedName().toString();
            String methodName = method.getSimpleName().toString();
            ProxyInfo info = map.get(className);
            if (info == null) {
                info = new ProxyInfo(mUtils, typeElement);
                map.put(className, info);
            }
            Annotation anno = method.getAnnotation(clazz);
            if (anno instanceof PermissionsGranted) {
                int[] value = ((PermissionsGranted) anno).value();
                if (value.length > 1) {
                    info.grantedMap.put(methodName, value);
                } else {
                    info.singleGrantMap.put(value[0], methodName);
                }
            } else if (anno instanceof PermissionsDenied) {
                int[] value = ((PermissionsDenied) anno).value();
                if (value.length > 1) {
                    info.deniedMap.put(methodName, value);
                } else {
                    info.singleDeniedMap.put(value[0], methodName);
                }

            } else if (anno instanceof PermissionsRationale) {
                int[] value = ((PermissionsRationale) anno).value();
                if (value.length > 1) {
                    info.rationaleMap.put(methodName, value);
                } else {
                    info.singleRationaleMap.put(value[0], methodName);
                }
            } else if (anno instanceof PermissionsCustomRationale) {
                int[] value = ((PermissionsCustomRationale) anno).value();
                if (value.length > 1) {
                    info.customRationaleMap.put(methodName, value);
                } else {
                    info.singleCustomRationaleMap.put(value[0], methodName);
                }
            } else {
                error(element, "%s not support", element);
                return false;
            }
        }
        return true;
    }


    private void error(Element element, String msg, Object... args) {
        if (args.length != 0) {
            msg = String.format(msg, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
    }

    private boolean isValid(Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE)
                || element.getModifiers().contains(Modifier.ABSTRACT);
    }

}
