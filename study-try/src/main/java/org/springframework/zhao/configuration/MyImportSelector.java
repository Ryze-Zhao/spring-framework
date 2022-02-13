package org.springframework.zhao.configuration;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        importingClassMetadata.getAnnotationTypes().forEach(System.out::println);
        if(importingClassMetadata.hasAnnotation("org.springframework.zhao.configuration.annotation.MyEnableAnnotation")){
            return new String[]{"org.springframework.zhao.configuration.pojo.ImportSelectorBean"};
        }
        return null;
    }
}

