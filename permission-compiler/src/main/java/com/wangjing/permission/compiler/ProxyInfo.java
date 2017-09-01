package com.wangjing.permission.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.sound.midi.MidiDevice;
import javax.xml.ws.soap.MTOM;

/**
 * Created by xiaoyi on 2017-8-31.
 */

public class ProxyInfo {
    private Elements mUtils;
    private TypeElement element;
    private final static String SUFFIX = "PermissionsProxy";
    private final static String CONCAT = "$$";
    private final String PACKAGENAME;
    private String proxyName;

    //methodName => requestCodes
    Map<String, int[]> grantedMap = new HashMap<>();
    Map<String, int[]> deniedMap = new HashMap<>();
    Map<String, int[]> rationaleMap = new HashMap<>();
    Map<String, int[]> customRationaleMap = new HashMap<>();

    //requestCode => methodName
    Map<Integer, String> singleGrantMap = new HashMap<>();
    Map<Integer, String> singleDeniedMap = new HashMap<>();
    Map<Integer, String> singleRationaleMap = new HashMap<>();
    Map<Integer, String> singleCustomRationaleMap = new HashMap<>();

    //permissions
    Map<int[], String[]> syncPermissions = new HashMap<>();
    int firstRequestCode;
    String firstPermission;

    public ProxyInfo(Elements mUtils, TypeElement element) {
        this.mUtils = mUtils;
        this.element = element;
        PACKAGENAME = mUtils.getPackageOf(element).getQualifiedName().toString();
        String className = element.getQualifiedName().toString().substring(PACKAGENAME.length() + 1);
        proxyName = className + CONCAT + SUFFIX;
    }

    public String getProxyName() {
        return proxyName;
    }

    public TypeElement getElement() {
        return element;
    }

    public void generateJavaFile(Writer writer) throws IOException {
        ClassName typeName = ClassName.get(element);
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(getProxyName())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("com.wangjing.permission.api","PermissionsProxy"),
                        typeName
                ));

        generateMethodCode(typeSpecBuilder);

        JavaFile file = JavaFile.builder(PACKAGENAME, typeSpecBuilder.build())
                .build();
        file.writeTo(writer);

    }

    private void generateMethodCode(TypeSpec.Builder builder) {
        generateGrantedMethod(builder);
        generateDeniedMethod(builder);
        generateRationaleMethod(builder);
        generateCustomRationaleMethod(builder);
        generateSyncRequestPermissoinsMethod(builder);
    }

    private void generateSyncRequestPermissoinsMethod(TypeSpec.Builder typeBuilder) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("startSyncRequestPermissionsMethod")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .returns(TypeName.VOID)
                .addParameter(ClassName.get(element), "target");
        ClassName className = ClassName.get("com.wangjing.permission.api", "WJPermission");
        builder.addStatement("$T.requestPermission(target,$S,$L)", className, firstPermission, firstRequestCode);
        typeBuilder.addMethod(builder.build());
    }

    /**
     * generate ustomrationale method
     *
     * @param typeBuilder
     */
    private void generateCustomRationaleMethod(TypeSpec.Builder typeBuilder) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("customRationale")
                .returns(TypeName.BOOLEAN)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(ClassName.get(element), "target")
                .addParameter(TypeName.INT, "code");
        builder.beginControlFlow("switch(code)");
        for (String methodName : customRationaleMap.keySet()) {
            int[] requestCodes = customRationaleMap.get(methodName);
            for (int requestCode : requestCodes) {
                builder.addStatement("case $L:\n"
                        + "target." + methodName + "($L)", requestCode, requestCode);
                if (singleCustomRationaleMap.containsKey(requestCode)) {
                    singleCustomRationaleMap.remove(requestCode);
                }
                builder.addStatement("return true");
            }
        }
        for (Integer requestCode : singleCustomRationaleMap.keySet()) {
            builder.addStatement("case $L:\n"
                    + "target." + singleCustomRationaleMap.get(requestCode) + "()", requestCode);
            builder.addStatement("return true");
        }
        builder.addStatement("default:\nreturn false");
        builder.endControlFlow();

        typeBuilder.addMethod(builder.build());

    }

    /**
     * generate granted method code
     *
     * @param typeBuilder
     */
    private void generateGrantedMethod(TypeSpec.Builder typeBuilder) {
        ClassName typeName = ClassName.get(element);
        MethodSpec.Builder builder = MethodSpec.methodBuilder("granted")
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName, "target")
                .addParameter(TypeName.INT, "code")
                .returns(TypeName.VOID);

        builder.beginControlFlow("switch(code)");
        for (String methodName : grantedMap.keySet()) {
            int[] ints = grantedMap.get(methodName);
            for (int requestCode : ints) {
                builder.addStatement("case " + requestCode + " :\n"
                        + "target." + methodName + "(" + requestCode + ")");
                addSyncRequestPermisionMethod(builder, requestCode);
                if (singleGrantMap.containsKey(requestCode)) {
                    singleGrantMap.remove(requestCode);
                }
            }
        }

        for (Integer requestCode : singleGrantMap.keySet()) {
            builder.addStatement("case " + requestCode + " :\n"
                    + "target." + singleGrantMap.get(requestCode) + "();\nbreak");
        }
        builder.addStatement("default:\nbreak");
        builder.endControlFlow();

        typeBuilder.addMethod(builder.build());
    }

    /**
     * generate denied method code
     *
     * @param typeBuilder
     */
    private void generateDeniedMethod(TypeSpec.Builder typeBuilder) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("denied")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(ClassName.get(element), "target")
                .addParameter(ClassName.INT, "code");
        builder.beginControlFlow("switch(code)");
        for (String methodName : deniedMap.keySet()) {
            int[] requestCodes = deniedMap.get(methodName);
            for (int requestCode : requestCodes) {
                builder.addStatement("case $L:\n target." + methodName + "($L)",
                        requestCode, requestCode);
                addSyncRequestPermisionMethod(builder, requestCode);
                if (singleDeniedMap.containsKey(requestCode)) {
                    singleDeniedMap.remove(requestCode);
                }
            }
        }

        for (Integer requestCode : singleDeniedMap.keySet()) {
            builder.addStatement("case $L:\n"
                    + "target." + singleDeniedMap.get(requestCode) + "();\nbreak", requestCode);
        }
        builder.addStatement("default:\nbreak");
        builder.endControlFlow();
        typeBuilder.addMethod(builder.build());
    }

    private void generateRationaleMethod(TypeSpec.Builder typeBuilder) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("rationale")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addParameter(ClassName.get(element), "target")
                .addParameter(ClassName.INT, "code");
        builder.beginControlFlow("switch(code)");
        for (String methodName : rationaleMap.keySet()) {
            int[] requestCodes = rationaleMap.get(methodName);
            for (int requestCode : requestCodes) {
                builder.addStatement("case $L:\n"
                        + "target." + methodName + "($L);\nbreak", requestCode, requestCode);
                if (singleRationaleMap.containsKey(requestCode)) {
                    singleRationaleMap.remove(requestCode);
                }
            }
        }

        for (Integer requestCode : singleRationaleMap.keySet()) {
            builder.addStatement("case $L:\n"
                    + "target." + singleRationaleMap.get(requestCode) + "($L);\nbreak", requestCode, requestCode);
        }
        builder.addStatement("default:\nbreak")
                .endControlFlow();

        typeBuilder.addMethod(builder.build());

    }

    private void addSyncRequestPermisionMethod(MethodSpec.Builder builder, int requestCode) {
        ClassName className = ClassName.get("com.wangjing.permission.api", "WJPermission");
        for (int[] requestCodes : syncPermissions.keySet()) {
            int length = requestCodes.length;
            String[] permissions = syncPermissions.get(requestCodes);
            firstPermission = permissions[0];
            firstRequestCode = requestCodes[0];
            for (int i = 0; i < length - 1; i++) {
                if (requestCodes[i] == requestCode) {
                    builder.addStatement("$T.requestPermission(target, $S, $L)"
                            , className, permissions[i + 1], requestCodes[i + 1]);
                }
            }
        }
    }

}
